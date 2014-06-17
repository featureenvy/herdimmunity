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

(defn init-shaders [gl]
  (let [fragment-shader (js/getShader gl "shader-fs")
        vertex-shader (js/getShader gl "shader-vs")
        shader-program (.createProgram gl)]
    (.attachShader gl shader-program vertex-shader)
    (.attachShader gl shader-program fragment-shader)
    (.linkProgram gl shader-program)
    (.useProgram gl shader-program)

    (.enableVertexAttribArray gl (attrib-location gl shader-program "aVertexPosition"))

    shader-program))

(defn init-buffers [gl board-size board]
  (let [s-vertices-buffer (.createBuffer gl)
        vertices #js [1 1 2 2 3 3]]
    
    (.bindBuffer gl (.-ARRAY_BUFFER gl) s-vertices-buffer)
    (.bufferData gl (.-ARRAY_BUFFER gl) (js/Float32Array. vertices) (.-STATIC_DRAW gl))

    {:vertex-buffer s-vertices-buffer}))

(defn draw-scene
  [gl shader-program {:keys [vertex-buffer]} board-size]
  (let [mv-matrix (.create js/mat4)
        p-matrix (.create js/mat4)]
    (.viewport gl 0 0 (.-viewportWidth gl) (.-viewportHeight gl))
    (.clear gl (bit-or (.-COLOR_BUFFER_BIT gl) (.-DEPTH_BUFFER_BIT gl)))
    
    (.ortho js/mat4 p-matrix 0 board-size board-size 0 0.1 100.0)
    
    (.translate js/mat4 mv-matrix mv-matrix #js [0 0 -14])
    
    (.bindBuffer gl (.-ARRAY_BUFFER gl) vertex-buffer)
    (.vertexAttribPointer gl (attrib-location gl shader-program "aVertexPosition") 2 (.-FLOAT gl) false 0 0)

    (set-matrix-uniforms gl shader-program p-matrix mv-matrix)
    (.drawArrays gl (.-POINTS gl) 0 3)))

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

