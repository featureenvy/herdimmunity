(ns herdimmunity.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [herdimmunity.game :as game]))

(enable-console-print!)

(def app-state (atom {:board game/board}))

(defn print-row
  [row]
  (map (fn [text] (dom/td #js {:className (name text)
                              :onClick (fn [e] (js/console.log (.. e -target -className)))}
                         nil))
       row))

(defn cell-view [cell owner]
  (reify
    om/IRender
    (render [_]
      (dom/td #js {:className (name cell)
                              :onClick (fn [e] (js/console.log (.. e -target -className)))}
                         nil))))

(defn board-view [app owner]
  (reify
    om/IRender
    (render [_]
      (apply dom/table #js {:className "main-board"}
             (map (fn [row] (apply dom/tr nil
                                  (om/build-all cell-view row)))
                  (:board app))))))

(om/root board-view app-state
         {:target (. js/document (getElementById "app"))})
