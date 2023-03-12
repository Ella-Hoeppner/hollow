(ns sprog.dev.startup
  (:require sprog.dev.array-demo))

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
  (sprog.dev.array-demo/init))

(defn pre-init []
  (js/window.addEventListener "load" init))
