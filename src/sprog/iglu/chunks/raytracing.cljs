(ns sprog.iglu.chunks.raytracing
  (:require [clojure.walk :refer [postwalk-replace]]
            [sprog.iglu.core :refer [merge-chunks]]))

(def ray-chunk
  '{:structs {Ray [pos vec3
                   dir vec3]}})

(def plane-intersection-chunk
  (merge-chunks ray-chunk
                '{:functions {findPlaneIntersection
                              {([Ray Ray] float)
                               ([ray planeRay]
                                (/ (dot (- planeRay.pos ray.pos) planeRay.dir)
                                   (dot ray.dir planeRay.dir)))}}}))

(def sphere-intersection-chunk
  (merge-chunks ray-chunk
                '{:functions
                  {findSphereIntersections
                   {([Ray vec3 float] vec2)
                    ([ray sphereCenter sphereRadius]
                     (=vec3 offset (- ray.pos sphereCenter))
                     (=float halfB (dot offset ray.dir))
                     (=float c (- (dot offset offset)
                                  (* sphereRadius sphereRadius)))
                     (=float discriminant (- (* halfB halfB) c))
                     ("if" (> discriminant 0)
                           (=float discriminantSqrt (sqrt discriminant))
                           (return (- 0
                                      (vec2 (+ halfB discriminantSqrt)
                                            (- halfB discriminantSqrt)))))
                     (vec2 0))}}}))

(def plane-sdf-chunk
  (merge-chunks ray-chunk
                '{:functions {sdPlane
                              {([vec3 Ray] float)
                               ([pos planeRay]
                                (dot (- planeRay.pos pos) planeRay.dir))}}}))

(defn create-raymarch-chunk [sdf-name & [{:keys [step-factor
                                                 max-steps
                                                 termination-threshold
                                                 fn-name]
                                          :or {step-factor 1
                                               max-steps 1024
                                               termination-threshold 0.001
                                               fn-name 'march}}]]
  (merge-chunks ray-chunk
                (postwalk-replace
                 {:sdf-name sdf-name
                  :step-factor step-factor
                  :max-steps (str (int max-steps))
                  :termination-threshold termination-threshold
                  :fn-name fn-name}
                 '{:functions {:fn-name
                               {([Ray] vec3)
                                ([ray]
                                 (=float t 0)
                                 (=int maxSteps :max-steps)
                                 ("for(int i=0;i<maxSteps;i++)"
                                  (=float distanceEstimate
                                          (:sdf-name (+ ray.pos (* t ray.dir))))
                                  ("if" (< (abs distanceEstimate) 
                                           :termination-threshold)
                                        "break")
                                  (+= t (* distanceEstimate :step-factor)))
                                 (+ ray.pos (* t ray.dir)))}}})))

(def perspective-camera-chunk
  (merge-chunks ray-chunk
                '{:functions
                  {cameraRay
                   {([vec2 vec3 vec3 float] Ray)
                    ([screenPos camTarget camPos focalDist]
                     (=vec3 camDir (normalize (- camTarget camPos)))
                     (=vec3 camRight (normalize (cross camDir (vec3 0 1 0))))
                     (=vec3 camUp (cross camRight camDir))

                     (=vec3 filmPos (+ (* camDir focalDist)
                                       (* (- screenPos.x 0.5) camRight)
                                       (* (- screenPos.y 0.5) camUp)))

                     (Ray camPos (normalize filmPos)))}}}))

(defn get-voxel-intersection-chunk
  [& [{:keys [max-voxel-steps
              return-type
              default-return-expression
              hit-expression]
       :or {max-voxel-steps 1024
            return-type 'VoxelIntersection
            default-return-expression '(VoxelIntersection "false"
                                                          (ivec3 "0")
                                                          (vec3 0)
                                                          (vec3 0))
            hit-expression '((return voxelIntersection))}}]]
  (merge-chunks
   ray-chunk
   (postwalk-replace
    {:max-voxel-steps (str max-voxel-steps)
     :return-type return-type
     :hit-expression hit-expression
     :default-return-expression default-return-expression
     :voxel-hit-expression
     (concat
      (list "if"
            '(voxelFilled voxelCoords)
            '(=VoxelIntersection voxelIntersection
                                 (VoxelIntersection "true"
                                                    voxelCoords
                                                    (+ ray.pos
                                                       (* ray.dir dist))
                                                    norm)))
      hit-expression)}
    '{:structs {VoxelIntersection [hit bool
                                   gridPos ivec3
                                   pos vec3
                                   norm vec3]}
      :functions
      {findVoxelIntersection
       {([Ray float] :return-type)
        ([ray maxDist]
         (=ivec3 voxelCoords (ivec3 (floor ray.pos)))
         (=vec3 innerCoords (fract ray.pos))

         (=ivec3 step (ivec3 (sign ray.dir)))
         (=vec3 delta (/ (vec3 step) ray.dir))

         (=vec3 tMax (* delta
                        (vec3 (if (> ray.dir.x "0.0")
                                (- "1.0" innerCoords.x)
                                innerCoords.x)
                              (if (> ray.dir.y "0.0")
                                (- "1.0" innerCoords.y)
                                innerCoords.y)
                              (if (> ray.dir.z "0.0")
                                (- "1.0" innerCoords.z)
                                innerCoords.z))))

         (=vec3 norm (vec3 0))
         (=int maxVoxelSteps :max-voxel-steps)
         ("for(int i=0;i<maxVoxelSteps;i++)"
          (=vec3 t
                 (min (/ (- (vec3 voxelCoords) ray.pos) ray.dir)
                      (/ (- (vec3 (+ (vec3 voxelCoords) 1)) ray.pos) ray.dir)))
          (=float dist (max (max t.x t.y) t.z))
          ("if" (>= dist maxDist) (return :default-return-expression))
          :voxel-hit-expression
          ("if" (< tMax.x tMax.y)
                ("if" (< tMax.z tMax.x)
                      (+= tMax.z delta.z)
                      (+= voxelCoords.z step.z)
                      (= norm (vec3 0 0 (- "0.0" (float step.z)))))
                ("else"
                 (+= tMax.x delta.x)
                 (+= voxelCoords.x step.x)
                 (= norm (vec3 (- "0.0" (float step.x)) 0 0))))
          ("else" ("if" (< tMax.z tMax.y)
                        (+= tMax.z delta.z)
                        (+= voxelCoords.z step.z)
                        (= norm (vec3 0 0 (- "0.0" (float step.z)))))
                  ("else"
                   (+= tMax.y delta.y)
                   (+= voxelCoords.y step.y)
                   (= norm (vec3 0 (- "0.0" (float step.y)) 0)))))
         :default-return-expression)}}})))
