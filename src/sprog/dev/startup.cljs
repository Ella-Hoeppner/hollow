(ns sprog.dev.startup
  (:require sprog.dev.basic-demo
            sprog.dev.multi-texture-output-demo
            sprog.dev.fn-sort-demo
            sprog.dev.pixel-sort-demo
            sprog.dev.raymarch-demo
            sprog.dev.physarum-demo
            sprog.dev.texture-channel-demo
            sprog.dev.struct-demo
            sprog.dev.simplex-demo
            sprog.dev.tilable-simplex-demo
            sprog.dev.macro-demo
            sprog.dev.bilinear-demo
            sprog.dev.vertex-demo
            sprog.dev.voronoise-demo
            sprog.dev.video-demo
            sprog.dev.webcam-demo))

(defn init []
  #_(sprog.dev.basic-demo/init)
  #_(sprog.dev.multi-texture-output-demo/init)
  #_(sprog.dev.fn-sort-demo/init)
  #_(sprog.dev.pixel-sort-demo/init)
  #_(sprog.dev.raymarch-demo/init)
  #_(sprog.dev.physarum-demo/init)
  #_(sprog.dev.texture-channel-demo/init)
  #_(sprog.dev.struct-demo/init)
  #_(sprog.dev.simplex-demo/init)
  #_(sprog.dev.tilable-simplex-demo/init)
  #_(sprog.dev.macro-demo/init)
  #_(sprog.dev.bilinear-demo/init)
  #_(sprog.dev.vertex-demo/init)
  #_(sprog.dev.voronoise-demo/init)
  #_(sprog.dev.video-demo/init)
  (sprog.dev.webcam-demo/init))

(defn pre-init []
  (js/window.addEventListener "load" (fn [_] (init))))
