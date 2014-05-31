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

(defn build-tile-vertices [x-coord y-coord]
  (let [tile-template [[0 0 0]
                       [1 0 0]
                       [1 1 0]
                       [0 1 0]]
        translate (fn [[x y z]] [(+ x x-coord) (+ y y-coord) z])]
    (mapcat translate tile-template)))

(defn build-tile-indices [x-coord y-coord]
  (let [index-template [0 1 2 0 2 3]
        offset (* 4 y-coord)
        translate (partial + offset (* x-coord 4))]
    (map translate index-template)))

(defn build-tile-colors [x-coord y-coord]
  (let [color-template [[0.1 0.1 0.1 1.0]
                        [0.1 0.1 0.1 1.0]
                        [0.1 0.1 0.1 1.0]
                        [0.1 0.1 0.1 1.0]]
        x-inc (* x-coord 0.1)
        y-inc (* y-coord 0.1)
        translate (fn [[x y z a]] [(+ x x-inc) (+ y y-inc) z a])]
    (mapcat translate color-template)))

(defn create-tiles-row [size y-coord]
  (let [index-template [0 1 2 0 2 3]
        vertices (mapcat #(build-tile-vertices % y-coord) (range size))
        indices (mapcat #(build-tile-indices % (* y-coord size)) (range size))
        colors (mapcat #(build-tile-colors % y-coord) (range size))]
    {:vertices vertices :indices indices :colors colors}))

(defn create-tiles [size]
  (reduce #(merge-with concat %1 (create-tiles-row size %2)) {}  (range size)))

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
    (.bufferData gl (.-ARRAY_BUFFER gl) (js/Float32Array. (clj->js vertices)) (.-STATIC_DRAW gl))

    (.bindBuffer gl (.-ELEMENT_ARRAY_BUFFER gl) s-index-buffer)
    (.bufferData gl (.-ELEMENT_ARRAY_BUFFER gl) (js/Uint16Array. (clj->js indices)) (.-STATIC_DRAW gl))

    (.bindBuffer gl (.-ARRAY_BUFFER gl) s-color-buffer)
    (.bufferData gl (.-ARRAY_BUFFER gl) (js/Float32Array. (clj->js colors)) (.-STATIC_DRAW gl))

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
  (let [board-size 100
        canvas (.getElementById js/document "main-board")
        gl (init-gl canvas)
        shader-program (init-shaders gl)
        buffers (init-buffers gl board-size)]

    (.clearColor gl 0.0 0.0 0.0 1.0)
    (.enable gl (.-DEPTH_TEST gl))

    (tick gl shader-program buffers board-size)))
