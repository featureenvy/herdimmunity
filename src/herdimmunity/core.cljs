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

(defn print-board
  [board]
  (map (fn [row] (apply dom/tr nil (print-row row))) board))

(om/root
 (fn [app owner]
   (apply dom/table #js {:className "main-board"}
          (print-board (:board app))))
 app-state
 {:target (. js/document (getElementById "app"))})
