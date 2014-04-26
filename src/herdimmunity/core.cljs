(ns herdimmunity.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [herdimmunity.game :as game]))

(enable-console-print!)

(def board-size 20)
(def width 20)
(def height width)

(def app-state (atom {:board (game/gen-board board-size)}))

(defn cycle-state [curr-state]
  (let [transitions {:alive :infected
                     :infected :dead
                     :dead :empty
                     :empty :alive}]
    [((first curr-state) transitions)]))

(defn cell-view [cell owner]
  (reify
    om/IRenderState
    (render-state [_ {:keys [x y]}]
      (dom/rect #js {:x (* x width) :y (* y height)
                     :width width :height height
                     :className (name (first cell))
                                        ;:onClick #(om/transact! cell cycle-state)
                     :onClick (fn [e] (om/transact! cell (fn [_] [:dead])))
                     }
                nil))))

(defn row-view [row owner]
  (reify
    om/IRenderState
    (render-state [_ {:keys [y]}]
      (map-indexed (fn [x cell] (om/build cell-view cell
                                         {:init-state {:x x :y y}}))
                   row))))

(defn board-view [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (apply dom/svg #js {:className "main-board"}
                      (apply concat
                             (map-indexed (fn [y row]
                                            (map-indexed (fn [x cell] (om/build cell-view cell
                                                                               {:init-state {:x x :y y}}))
                                                         row))
                                          (:board app))))
               (dom/button #js {:onClick #(om/transact! app :board game/step)}
                           "Next step")
               (dom/button #js {:onClick #(om/update! app :board (game/gen-board board-size))}
                           "New Game")))))

(om/root board-view app-state
         {:target (. js/document (getElementById "app"))})
