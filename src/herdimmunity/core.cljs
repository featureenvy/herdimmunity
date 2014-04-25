(ns herdimmunity.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [herdimmunity.game :as game]))

(enable-console-print!)

(def board-size 20)

(def app-state (atom {:board (game/gen-board board-size)}))

(defn cycle-state [curr-state]
  (let [transitions {:alive :infected
                     :infected :dead
                     :dead :empty
                     :empty :alive}]
    [((first curr-state) transitions)]))

(defn cell-view [cell owner]
  (reify
    om/IRender
    (render [_]
      (dom/td #js {:className (name (first cell))
                   :onClick #(om/transact! cell cycle-state)}
                         nil))))

(defn board-view [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (apply dom/table #js {:className "main-board"}
                      (map (fn [row] (apply dom/tr nil
                                           (om/build-all cell-view row)))
                           (:board app)))
               (dom/button #js {:onClick #(om/transact! app :board game/step)}
                           "Next step")
               (dom/button #js {:onClick #(om/update! app :board (game/gen-board board-size))}
                           "New Game")))))

(om/root board-view app-state
         {:target (. js/document (getElementById "app"))})
