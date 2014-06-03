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

(defn recursive-create-tiles [offset size create-fn]
  (loop [vertices (atom #js [])
         walk-pos 0]
    (if (< walk-pos size)
      (do
        (reset! vertices (.concat @vertices (create-fn walk-pos offset size)))
        (recur vertices (inc walk-pos)))
      @vertices)))

(defn create-vertice-cell [col-num row-num size]
  (let [tile-template #js [0 0 0
                       1 0 0
                       1 1 0
                           0 1 0]]
    (aset tile-template 0 col-num)
    (aset tile-template 3 (inc col-num))
    (aset tile-template 6 (inc col-num))
    (aset tile-template 9 col-num)

    (aset tile-template 1 row-num)
    (aset tile-template 4 row-num)
    (aset tile-template 7 (inc row-num))
    (aset tile-template 10 (inc row-num))

    tile-template))

(defn create-vertice-row [curr-pos offset size]
  (recursive-create-tiles curr-pos size create-vertice-cell))

(defn create-vertices [size]
  (recursive-create-tiles 0 size create-vertice-row))

(defn create-indices-cell [col-num row-num size]
  (let [index-template #js [0 1 2 0 2 3]
        x-offset (* 4 col-num)
        y-offset (* 4 size row-num)
        offset (+ x-offset y-offset)]
    (aset index-template 0 offset)
    (aset index-template 1 (+ offset 1))
    (aset index-template 2 (+ offset 2))
    (aset index-template 3 offset)
    (aset index-template 4 (+ offset 2))
    (aset index-template 5 (+ offset 3))

    index-template))

(defn create-indices-row [curr-pos offset size]
  (recursive-create-tiles curr-pos size create-indices-cell))

(defn create-indices [size]
  (recursive-create-tiles 0 size create-indices-row))

(defn build-tile-colors [x-coord y-coord]
  (let [color-template [[0.1 0.1 0.1 1.0]
                        [0.1 0.1 0.1 1.0]
                        [0.1 0.1 0.1 1.0]
                        [0.1 0.1 0.1 1.0]]
        x-inc (* x-coord 0.1)
        y-inc (* y-coord 0.1)
        translate (fn [[x y z a]] [(+ x x-inc) (+ y y-inc) z a])]
    (mapcat translate color-template)))

(defn create-color-cell [col-num row-num size]
  (let [color-template #js [0.1 0.1 0.1 1.0
                        0.1 0.1 0.1 1.0
                        0.1 0.1 0.1 1.0
                        0.1 0.1 0.1 1.0]
        x-value (+ 0.1 (* col-num 0.1))
        y-value (* row-num 0.1)]
    (aset color-template 0 x-value)
    (aset color-template 4 x-value)
    (aset color-template 8 x-value)
    (aset color-template 12 x-value)

    (aset color-template 1 y-value)
    (aset color-template 5 y-value)
    (aset color-template 9 y-value)
    (aset color-template 13 y-value)

    color-template))

(defn create-colors-row [curr-pos offset size]
  (recursive-create-tiles curr-pos size create-color-cell))

(defn create-colors [size]
  (recursive-create-tiles 0 size create-colors-row))

(defn create-tiles [size]
  {:vertices (create-vertices size)
   :indices (create-indices size)
   :colors (create-colors size)})

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

(defn init-buffers [gl board-size]
  (let [s-vertices-buffer (.createBuffer gl)
        s-index-buffer (.createBuffer gl)
        s-color-buffer (.createBuffer gl)
        {:keys [vertices indices colors]} (create-tiles board-size)]
    
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

(defn webgl-start []
  (let [board-size 1000
        canvas (.getElementById js/document "main-board")
        gl (init-gl canvas)
        shader-program (init-shaders gl)
        buffers (init-buffers gl board-size)]

    (.clearColor gl 0.0 0.0 0.0 1.0)
    (.enable gl (.-DEPTH_TEST gl))

    (tick gl shader-program buffers board-size)))
