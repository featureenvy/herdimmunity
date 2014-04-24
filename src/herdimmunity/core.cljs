(ns herdimmunity.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [herdimmunity.game :as game]))

(enable-console-print!)

(def app-state (atom {:board game/board}))

(defn alive? [type]
  (if (= "X" type) "alive"))

(defn dead? [type]
  (if (= "O" type) "dead"))

(defn print-row
  [row]
  (let [type-switcher (fn [type]
                        (or (alive? type) (dead? type) "empty"))]
    (map (fn [text] (dom/td #js {:className (type-switcher text)
                                :onClick (fn [e] (js/console.log (.. e -target -className)))} nil)) row)))

(defn print-board
  [board]
  (map (fn [row] (apply dom/tr nil (print-row row))) board))

(om/root
 (fn [app owner]
   (apply dom/table #js {:className "main-board"}
          (print-board (:board app))))
 app-state
 {:target (. js/document (getElementById "app"))})
