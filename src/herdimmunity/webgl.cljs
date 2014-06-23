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

(defn generate-cell [avertice board-size y-pos step-size]
  (loop [index 0]
      (if (< index board-size)
        (do
          (.push avertice (* index step-size))
          (.push avertice y-pos)
          (recur (inc index)))
        avertice)))

(defn generate-vertices [board-size]
  (let [step-size (/ 1 (dec board-size))
        vertices #js []]
    (loop [index 0]
      (if (< index board-size)
        (do
          (generate-cell vertices board-size (* index step-size) step-size)
          (recur (inc index)))
        vertices))))

(defn generate-particle-column [data column-size row-index color-index]
  (let [step-size (/ 1 (dec column-size))]
    (loop [col 0]
      (if (< col column-size)
        (do
          (.push data col)
          (.push data row-index)
          (.push data (* step-size col))
          (.push data color-index)
          (recur (inc col)))
        data))))

(defn generate-particles [board-size]
  (let [step-size (/ 1 (dec board-size))
        data #js []]
    (loop [row 0]
      (if (< row board-size)
        (do
          (generate-particle-column data board-size row (* step-size row))
          (recur (inc row)))
        data))))

(defn init-buffers [gl board-size board]
  (let [s-vertices-buffer (.createBuffer gl)
        vertice-step (/ 1 (dec board-size))
        vertices (generate-vertices board-size)
        particle-data-texture (.createTexture gl)
        particle-data (generate-particles board-size)]

    (.getExtension gl "OES_texture_float")
    
    (.bindBuffer gl (.-ARRAY_BUFFER gl) s-vertices-buffer)
    (.bufferData gl (.-ARRAY_BUFFER gl) (js/Float32Array. vertices) (.-STATIC_DRAW gl))

    (.activeTexture gl (.-TEXTURE0 gl))
    (.bindTexture gl (.-TEXTURE_2D gl) particle-data-texture)
    (.texImage2D gl (.-TEXTURE_2D gl) 0 (.-RGBA gl) board-size board-size 0 (.-RGBA gl) (.-FLOAT gl) (js/Float32Array. particle-data))
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
    (set-uniform gl shader-program "uPointSize" (* (/ board-size 5) 4))
    
    (.bindBuffer gl (.-ARRAY_BUFFER gl) vertex-buffer)
    (.vertexAttribPointer gl (attrib-location gl shader-program "aVertexPosition") 2 (.-FLOAT gl) (.-FALSE gl) 0 0)

    (.drawArrays gl (.-POINTS gl) 0 (* board-size board-size))))

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

