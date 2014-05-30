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

(defn build-tile-vertices [n]
  (let [tile-template [[0 0 0]
                       [1 0 0]
                       [1 1 0]
                       [0 1 0]]
        increase-x (fn [[x y z]] [(+ x n) y z])]
    (mapcat increase-x tile-template)))

(defn build-tile-indices [n]
  (let [index-template [0 1 2 0 2 3]]
    (map (partial + (* n 4)) index-template)))

(defn build-tile-colors [n]
  (let [color-template [0.1 0.1 0.1 1.0
                        0.1 0.1 0.1 1.0
                        0.1 0.1 0.1 1.0
                        0.1 0.1 0.1 1.0]]
    (map (partial + (/ n 10)) color-template)))

(defn create-tiles [size]
  (let [index-template [0 1 2 0 2 3]
        vertices (mapcat build-tile-vertices (range size))
        indices (mapcat build-tile-indices (range size))
        colors (mapcat build-tile-colors (range size))]
    {:vertices vertices :indices indices :colors colors}))

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
        s-colors #js [0.5 0.5 1.0 1.0 ; right
                      0.5 0.5 1.0 1.0
                      0.5 0.5 1.0 1.0
                      0.5 0.5 1.0 1.0
                      0.7 0.2 1.0 1.0 ; left
                      0.7 0.2 1.0 1.0
                      0.7 0.2 1.0 1.0
                      0.7 0.2 1.0 1.0]
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
    (.drawElements gl (.-TRIANGLES gl) (* 6 board-size) (.-UNSIGNED_SHORT gl) 0)))

(defn tick
  [gl shader-program buffers board-size]
  (js/requestAnimationFrame (partial tick gl shader-program buffers board-size))
  (draw-scene gl shader-program buffers board-size))

(defn webgl-start []
  (let [board-size 5
        canvas (.getElementById js/document "main-board")
        gl (init-gl canvas)
        shader-program (init-shaders gl)
        buffers (init-buffers gl board-size)]

    (.clearColor gl 0.0 0.0 0.0 1.0)
    (.enable gl (.-DEPTH_TEST gl))

    (tick gl shader-program buffers board-size)))
