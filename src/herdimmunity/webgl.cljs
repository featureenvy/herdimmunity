(ns herdimmunity.webgl)

(defn attrib-location [gl shader-program name]
  (.getAttribLocation gl shader-program name))

(defn set-matrix-uniform [gl shader-program matrix-name matrix]
  (.uniformMatrix4fv gl (.getUniformLocation gl shader-program matrix-name) false matrix))

(defn set-matrix-uniforms [gl shader-program p-matrix mv-matrix]
  (set-matrix-uniform gl shader-program "uPMatrix" p-matrix)
  (set-matrix-uniform gl shader-program "uMVMatrix" mv-matrix))

(defn init-gl [canvas]
  (let [gl (.getContext canvas "webgl")]
    (set! (.-viewportWidth gl) (.-width canvas))
    (set! (.-viewportHeight gl) (.-height canvas))
    gl))

(defn recursive-create-tiles [output offset size create-fn]
  (loop [walk-pos 0]
    (if (< walk-pos size)
      (do
        (create-fn output walk-pos offset size)
        (recur (inc walk-pos)))
      output)))

(defn create-vertice-cell [output col-num row-num size]
  (let [tile-template #js [0 0 0
                       1 0 0
                       1 1 0
                           0 1 0]
        array-start (* (.-length tile-template) (+ col-num (* row-num size)))]
    (aset output array-start col-num)
    (aset output (+ array-start 3) (inc col-num))
    (aset output (+ array-start 6) (inc col-num))
    (aset output (+ array-start 9) col-num)

    (aset output (+ array-start 1) row-num)
    (aset output (+ array-start 4) row-num)
    (aset output (+ array-start 7) (inc row-num))
    (aset output (+ array-start 10) (inc row-num))

    (aset output (+ array-start 2) 0)
    (aset output (+ array-start 5) 0)
    (aset output (+ array-start 8) 0)
    (aset output (+ array-start 11) 0)

    output))

(defn create-vertice-row [output curr-pos offset size]
  (recursive-create-tiles output curr-pos size create-vertice-cell))

(defn create-vertices [size]
  (let [vertices #js []]
    (recursive-create-tiles vertices 0 size create-vertice-row)

    vertices))

(defn create-indices-cell [output col-num row-num size]
  (let [index-template #js [0 1 2 0 2 3]
        x-offset (* 4 col-num)
        y-offset (* 4 size row-num)
        offset (+ x-offset y-offset)
        array-start (* (.-length index-template) (+ col-num (* row-num size)))]
    (aset output array-start offset)
    (aset output (+ array-start 1) (+ offset 1))
    (aset output (+ array-start 2) (+ offset 2))
    (aset output (+ array-start 3) offset)
    (aset output (+ array-start 4) (+ offset 2))
    (aset output (+ array-start 5) (+ offset 3))

    output))

(defn create-indices-row [output curr-pos offset size]
  (recursive-create-tiles output curr-pos size create-indices-cell))

(defn create-indices [size]
  (let [indices #js []]
    (recursive-create-tiles indices 0 size create-indices-row)

    indices))

(defn create-color-cell-old [output col-num row-num size]
  (let [color-template #js [0.1 0.1 0.1 1.0
                        0.1 0.1 0.1 1.0
                        0.1 0.1 0.1 1.0
                        0.1 0.1 0.1 1.0]
        x-value (+ 0.1 (* col-num 0.1))
        y-value (* row-num 0.1)
        array-start (* (.-length color-template) (+ col-num (* row-num size)))]
    (aset output array-start x-value)
    (aset output (+ array-start 4) x-value)
    (aset output (+ array-start 8) x-value)
    (aset output (+ array-start 12) x-value)

    (aset output (+ array-start 1) y-value)
    (aset output (+ array-start 5) y-value)
    (aset output (+ array-start 9) y-value)
    (aset output (+ array-start 13) y-value)

    (aset output (+ array-start 2) 0.1)
    (aset output (+ array-start 6) 0.1)
    (aset output (+ array-start 10) 0.1)
    (aset output (+ array-start 14) 0.1)

    (aset output (+ array-start 3) 1.0)
    (aset output (+ array-start 7) 1.0)
    (aset output (+ array-start 11) 1.0)
    (aset output (+ array-start 15) 1.0)

    output))

(defn create-color-cell [colors board-value]
  (.log js/console (first board-value)))

(defn create-colors-row [colors board-row]
  (doall (map #(create-color-cell colors %) board-row)))

(defn create-colors [size board]
  (let [colors #js []]
    (doall (map #(create-colors-row colors %) board))

    colors))

(defn create-tiles [size board]
  {:vertices (create-vertices size)
   :indices (create-indices size)
   :colors (create-colors size board)})

(defn init-shaders [gl]
  (let [fragment-shader (js/getShader gl "shader-fs")
        vertex-shader (js/getShader gl "shader-vs")
        shader-program (.createProgram gl)]
    (.attachShader gl shader-program vertex-shader)
    (.attachShader gl shader-program fragment-shader)
    (.linkProgram gl shader-program)
    (.useProgram gl shader-program)

    (.enableVertexAttribArray gl (attrib-location gl shader-program "aVertexPosition"))
    (.enableVertexAttribArray gl (attrib-location gl shader-program "aVertexColor"))

    shader-program))

(defn init-buffers [gl board-size board]
  (let [s-vertices-buffer (.createBuffer gl)
        s-index-buffer (.createBuffer gl)
        s-color-buffer (.createBuffer gl)
        {:keys [vertices indices colors]} (create-tiles board-size board)]
    
    (.bindBuffer gl (.-ARRAY_BUFFER gl) s-vertices-buffer)
    (.bufferData gl (.-ARRAY_BUFFER gl) (js/Float32Array. vertices) (.-STATIC_DRAW gl))

    (.bindBuffer gl (.-ELEMENT_ARRAY_BUFFER gl) s-index-buffer)
    (.bufferData gl (.-ELEMENT_ARRAY_BUFFER gl) (js/Uint16Array. indices) (.-STATIC_DRAW gl))

    (.bindBuffer gl (.-ARRAY_BUFFER gl) s-color-buffer)
    (.bufferData gl (.-ARRAY_BUFFER gl) (js/Float32Array. colors) (.-STATIC_DRAW gl))

    {:vertex-buffer s-vertices-buffer
     :index-buffer s-index-buffer
     :color-buffer s-color-buffer}))

(defn draw-scene
  [gl shader-program {:keys [vertex-buffer index-buffer color-buffer]} board-size]
  (let [mv-matrix (.create js/mat4)
        p-matrix (.create js/mat4)]
    (.viewport gl 0 0 (.-viewportWidth gl) (.-viewportHeight gl))
    (.clear gl (bit-or (.-COLOR_BUFFER_BIT gl) (.-DEPTH_BUFFER_BIT gl)))
    
    (.ortho js/mat4 p-matrix 0 board-size board-size 0 0.1 100.0)
    
    (.translate js/mat4 mv-matrix mv-matrix #js [0 0 -14])
    
    (.bindBuffer gl (.-ARRAY_BUFFER gl) vertex-buffer)
    (.vertexAttribPointer gl (attrib-location gl shader-program "aVertexPosition") 3 (.-FLOAT gl) false 0 0)

    (.bindBuffer gl (.-ARRAY_BUFFER gl) color-buffer)
    (.vertexAttribPointer gl (attrib-location gl shader-program "aVertexColor") 4 (.-FLOAT gl) false 0 0)

    (.bindBuffer gl (.-ELEMENT_ARRAY_BUFFER gl) index-buffer)
    (set-matrix-uniforms gl shader-program p-matrix mv-matrix)
    (.drawElements gl (.-TRIANGLES gl) (* 6 board-size board-size) (.-UNSIGNED_SHORT gl) 0)))

(defn tick
  [gl shader-program buffers board-size]
  (js/requestAnimationFrame (partial tick gl shader-program buffers board-size))
  (draw-scene gl shader-program buffers board-size))

(defn webgl-start [board]
  (let [board-size (count board)
        canvas (.getElementById js/document "main-board")
        gl (init-gl canvas)
        shader-program (init-shaders gl)
        buffers (init-buffers gl board-size board)]

    (.clearColor gl 0.0 0.0 0.0 1.0)
    (.enable gl (.-DEPTH_TEST gl))

    (tick gl shader-program buffers board-size)))

