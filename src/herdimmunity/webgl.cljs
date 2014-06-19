(ns herdimmunity.webgl)

(defn attrib-location [gl shader-program name]
  (.getAttribLocation gl shader-program name))

(defn set-p-matrix [gl shader-program p-matrix]
  (.uniformMatrix4fv gl (.getUniformLocation gl shader-program "uPMatrix") false p-matrix))

(defn set-uniform [gl shader-program name value]
  (.uniform1f gl (.getUniformLocation gl shader-program name) value))

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
    (.uniform1i gl (.getUniformLocation gl shader-program "uParticleData") 0)

    shader-program))

(defn init-buffers [gl board-size board]
  (let [s-vertices-buffer (.createBuffer gl)
        vertices #js [0.0 0.0
                      0.3 0.0
                      0.6 0.0
                      0.9 0.0
                      0.0 0.3
                      0.3 0.3
                      0.6 0.3
                      0.9 0.3
                      0.0 0.6
                      0.3 0.6
                      0.6 0.6
                      0.9 0.6
                      0.0 0.9
                      0.3 0.9
                      0.6 0.9
                      0.9 0.9]
        particle-data-texture (.createTexture gl)
        particle-data #js [1 1 0 0
                           2 2 0 0
                           3 3 0 0
                           4 4 0 0
                           5 5 0 0
                           6 6 0 0
                           7 7 0 0
                           8 8 0 0
                           9 9 0 0
                           10 10 0 0
                           11 11 0 0
                           12 12 0 0
                           13 13 0 0
                           14 14 0 0
                           15 15 0 0
                           16 16 0 0]]

    (.getExtension gl "OES_texture_float")
    
    (.bindBuffer gl (.-ARRAY_BUFFER gl) s-vertices-buffer)
    (.bufferData gl (.-ARRAY_BUFFER gl) (js/Float32Array. vertices) (.-STATIC_DRAW gl))

    (.activeTexture gl (.-TEXTURE0 gl))
    (.bindTexture gl (.-TEXTURE_2D gl) particle-data-texture)
    (.texImage2D gl (.-TEXTURE_2D gl) 0 (.-RGBA gl) 4 4 0 (.-RGBA gl) (.-FLOAT gl) (js/Float32Array. particle-data))
    (.texParameteri gl (.-TEXTURE_2D gl) (.-TEXTURE_MIN_FILTER gl) (.-NEAREST gl))
    (.texParameteri gl (.-TEXTURE_2D gl) (.-TEXTURE_MAG_FILTER gl) (.-NEAREST gl))
    (.texParameteri gl (.-TEXTURE_2D gl) (.-TEXTURE_WRAP_S gl) (.-CLAMP_TO_EDGE gl))
    (.texParameteri gl (.-TEXTURE_2D gl) (.-TEXTURE_WRAP_T gl) (.-CLAMP_TO_EDGE gl))
    
    {:vertex-buffer s-vertices-buffer}))

(defn draw-scene
  [gl shader-program {:keys [vertex-buffer]} board-size]
  (let [p-matrix (.create js/mat4)]
    (.viewport gl 0 0 (.-viewportWidth gl) (.-viewportHeight gl))
    (.clear gl (bit-or (.-COLOR_BUFFER_BIT gl) (.-DEPTH_BUFFER_BIT gl)))

    (.ortho js/mat4 p-matrix 0 board-size board-size 0 0.1 100.0)
    (set-p-matrix gl shader-program p-matrix)
    (set-uniform gl shader-program "uPointSize" board-size)
    
    (.bindBuffer gl (.-ARRAY_BUFFER gl) vertex-buffer)
    (.vertexAttribPointer gl (attrib-location gl shader-program "aVertexPosition") 2 (.-FLOAT gl) (.-FALSE gl) 0 0)

    (.drawArrays gl (.-POINTS gl) 0 16)))

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

