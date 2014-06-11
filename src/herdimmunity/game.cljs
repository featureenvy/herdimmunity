(ns herdimmunity.game)

(defn gen-board
  [size]
  (let [values [[:alive] [:infected] [:dead] [:empty]]
        gen-rows (fn [] (map #(rand-nth values) (range 1 size)))]
     (map #(gen-rows) (range 1 size))))

(defn neighbors [x y]
  (for [dx [-1 0 1] dy [-1 0 1]
        :when (not= 0 dx dy)]
    [(+ dx x) (+ dy y)]))

(defn transition-cell [x y board]
  (let [neighbor-values (map #(get-in board %) (neighbors x y))
        states (frequencies neighbor-values)]
    (cond
     (> 2 (get states [:infected])) [:infected]
     (> 4 (+ (get states [:alive]) (get states [:infected]))) [:alive]
     :else [:dead])))

(defn step
  [board]
  (map-indexed (fn [row-idx row] (map-indexed #(transition-cell %1 row-idx board) row))
                 board))
