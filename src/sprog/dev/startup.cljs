(ns sprog.dev.startup
  (:require sprog.dev.pcg-demo
            sprog.dev.basic-demo
            sprog.dev.multi-texture-output-demo
            sprog.dev.pixel-sort-demo
            sprog.dev.physarum-demo
            sprog.dev.simplex-demo
            sprog.dev.tilable-simplex-demo
            sprog.dev.macro-demo
            sprog.dev.bilinear-demo
            sprog.dev.vertex-demo
            sprog.dev.voronoise-demo
            sprog.dev.video-demo
            sprog.dev.webcam-demo
            sprog.dev.bloom-demo
            sprog.dev.texture-3d-demo
            sprog.dev.blur-demo
            sprog.dev.hsv-demo
            sprog.dev.gabor-demo
            sprog.dev.oklab-mix-demo
            sprog.dev.fbm-demo
            sprog.dev.midi-demo
            sprog.dev.raymarch-demo
            sprog.dev.array-demo
            sprog.dev.stencil-demo
            sprog.dev.nonconvex-polygon-demo))

(defn init []
  #_(sprog.dev.basic-demo/init)
  #_(sprog.dev.multi-texture-output-demo/init)
  #_(sprog.dev.pixel-sort-demo/init)
  #_(sprog.dev.physarum-demo/init)
  #_(sprog.dev.simplex-demo/init)
  #_(sprog.dev.tilable-simplex-demo/init)
  #_(sprog.dev.macro-demo/init)
  #_(sprog.dev.bilinear-demo/init)
  #_(sprog.dev.vertex-demo/init)
  #_(sprog.dev.voronoise-demo/init)
  #_(sprog.dev.video-demo/init)
  #_(sprog.dev.webcam-demo/init)
  #_(sprog.dev.bloom-demo/init)
  #_(sprog.dev.texture-3d-demo/init)
  #_(sprog.dev.blur-demo/init)
  #_(sprog.dev.hsv-demo/init)
  #_(sprog.dev.gabor-demo/init)
  #_(sprog.dev.oklab-mix-demo/init)
  #_(sprog.dev.fbm-demo/init)
  #_(sprog.dev.midi-demo/init)
  #_(sprog.dev.raymarch-demo/init)
  (sprog.dev.array-demo/init)
  #_(sprog.dev.stencil-demo/init)
  #_(sprog.dev.pcg-demo/init)
  #_(sprog.dev.nonconvex-polygon-demo/init))

(defn pre-init []
  (js/window.addEventListener "load" init))
