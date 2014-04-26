(ns herdimmunity.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [herdimmunity.game :as game]))

(enable-console-print!)

(def board-size 50)
(def width board-size)
(def height width)

(def app-state (atom {:board (game/gen-board board-size)}))

(defn cycle-state [curr-state]
  (let [transitions {:alive :infected
                     :infected :dead
                     :dead :empty
                     :empty :alive}]
    [((first curr-state) transitions)]))

(defn get-color [state]
  (cond
   (= :alive state) "green"
   (= :infected state) "yellow"
   (= :dead state) "red"
   :else "white"))

(defn draw-cell [canvas cell pos]
  (let [x (mod pos width)
        y (.floor js/Math (/ pos width))
        x-pos (* board-size x)
        y-pos (* board-size y)
        context (.getContext canvas "2d")]
    (set! (.-fillStyle context) (get-color cell))
    (.fillRect context x-pos y-pos width height)))

(defn fill-canvas [board canvas]
  (doall (map-indexed (fn [y cell]
                        (draw-cell canvas cell y))
                      (flatten board))))

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
