(ns herdimmunity.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [herdimmunity.game :as game]
            [herdimmunity.webgl :as webgl]))

(enable-console-print!)

(def board-size 50)
(def width board-size)
(def height width)

(def app-state (atom {:board (game/gen-board board-size)}))

(defn fill-canvas [board canvas]
  (webgl/webgl-start board))

(defn board-view [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/canvas #js {:id "main-board" :ref "main-board" :width (* width board-size) :height (* height board-size)}
                           nil)
               (dom/button #js {:onClick #(om/transact! app :board game/step)}
                           "Next step")
               (dom/button #js {:onClick #(om/update! app :board (game/gen-board board-size))}
                           "New Game")))
    om/IDidUpdate
    (did-update [_ _ _]
      (fill-canvas (:board app) (om/get-node owner "main-board")))
    om/IDidMount
    (did-mount [_]
      (fill-canvas (:board app) (om/get-node owner "main-board")))))

(om/root board-view app-state
         {:target (. js/document (getElementById "app"))})
