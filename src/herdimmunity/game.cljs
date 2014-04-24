(ns herdimmunity.game)

(defn gen-board
  [size]
  (let [values ["X" "O" " "]
        gen-rows (fn [] (map #(rand-nth values) (range 1 size)))]
    (map #(gen-rows) (range 1 size))))

(def board (gen-board 20))