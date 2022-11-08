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
