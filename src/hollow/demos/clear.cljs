(ns hollow.demos.clear
  (:require [hollow.util :as u]
            [hollow.dom.canvas :refer [maximize-gl-canvas
                                       canvas-resolution]]
            [kudzu.core :refer [kudzu->glsl]]
            [hollow.webgl.textures :refer [create-tex]]
            [hollow.webgl.shaders :refer [run-purefrag-shader!]]
            [hollow.webgl.core
             :refer-macros [with-context]
             :refer [start-hollow!
                     clear!]]
            [kudzu.tools :refer-macros [unquotable]]))

(defn init-page! [gl]
  (with-context gl
    (maximize-gl-canvas {:aspect-ratio 1})
    (let [resolution (canvas-resolution)
          tex (create-tex :u32 resolution)]
      (clear! {:color [0 1 1]})
      (clear! {:color [(dec (Math/pow 2 32)) 0 0 0]
               :target tex})
      (run-purefrag-shader!
       (kudzu->glsl
        (unquotable
         '{:precision {float highp
                       usampler2D highp}
           :uniforms {resolution vec2
                      tex usampler2D}
           :outputs {frag-color vec4}
           :main ((= frag-color
                     (-> tex
                         (texelFetch (ivec2 gl_FragCoord.xy) "0")
                         .rgb
                         vec3
                         (/ ~(dec (Math/pow 2 32)))
                         (vec4 1))))}))
       (mapv #(/ % 2) resolution)
       {:resolution resolution
        :tex tex}))
    {}))

(defn init []
  (start-hollow! init-page! nil))
