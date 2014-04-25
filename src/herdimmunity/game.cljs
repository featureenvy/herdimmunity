(ns herdimmunity.game)

(defn gen-board
  [size]
  (let [values [[:alive] [:infected] [:dead] [:empty]]
        gen-rows (fn [] (vec (map #(rand-nth values) (range 1 size))))]
    (vec (map #(gen-rows) (range 1 size)))))

(def board (gen-board 20))
