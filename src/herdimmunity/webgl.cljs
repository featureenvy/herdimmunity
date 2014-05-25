(ns herdimmunity.webgl)

(def cube-vertex-position-buffer (atom []))
(def cube-vertex-texture-coord-buffer (atom []))
(def cube-vertex-index-buffer (atom []))
(def p-matrix (atom (.create js/mat4)))
(def mv-matrix (atom (.create js/mat4)))
(def mv-matrix-stack (atom []))
(def x-rot (atom 0))
(def x-speed (atom 0))
(def y-rot (atom 0))
(def y-speed (atom 0))
(def z-rot (atom 0))
(def z (atom -5))
(def shader-filter (atom 0))
(def last-time (atom 0))
(def gl-obj (atom 0))
(def shader-program-obj (atom 0))
(def crate-textures (atom []))
(def currently-pressed-key (atom -1))

(defn mv-push-matrix
  []
  (let [copy (.create js/mat4)]
    (.copy js/mat4 copy @mv-matrix)
    (reset! mv-matrix-stack (conj @mv-matrix-stack copy))))

(defn mv-pop-matrix
  []
  (reset! mv-matrix (last @mv-matrix-stack))
  (reset! mv-matrix-stack (butlast @mv-matrix-stack)))

(defn deg-to-rad
  [degrees]
  (/ (* degrees (.-PI js/Math)) 180))

(defn handle-key-down [event]
  (reset! currently-pressed-key (.-keyCode event))
  (if (= "F" (.fromCharCode js/String (.-keyCode event)))
    (do
      (swap! shader-filter inc)
      (if (= 3 @shader-filter)
        (reset! shader-filter 0)))))

(defn handle-key-up [event]
  (reset! currently-pressed-key -1))

(defn handle-keys []
  (condp = @currently-pressed-key
    33 (reset! z (- @z 0.05))
    34 (reset! z (+ @z 0.05))
    37 (swap! y-speed dec)
    39 (swap! y-speed inc)
    38 (swap! x-speed dec)
    40 (swap! x-speed inc)
    nil))

(defn init-gl [canvas]
  (let [gl (.getContext canvas "webgl")]
    (set! (.-viewportWidth gl) (.-width canvas))
    (set! (.-viewportHeight gl) (.-height canvas))
    gl))

(defn handle-loaded-texture [gl textures]
  (.pixelStorei gl (.-UNPACK_FLIP_Y_WEBGL gl) true)
  
  (.bindTexture gl (.-TEXTURE_2D gl) (nth textures 0))
  (.texImage2D gl (.-TEXTURE_2D gl) 0 (.-RGBA gl) (.-RGBA gl) (.-UNSIGNED_BYTE gl)  (.-image (nth textures 0)))
  (.texParameteri gl (.-TEXTURE_2D gl) (.-TEXTURE_MAG_FILTER gl) (.-NEAREST gl))
  (.texParameteri gl (.-TEXTURE_2D gl) (.-TEXTURE_MIN_FILTER gl) (.-NEAREST gl))

  (.bindTexture gl (.-TEXTURE_2D gl) (nth textures 1))
  (.texImage2D gl (.-TEXTURE_2D gl) 0 (.-RGBA gl) (.-RGBA gl) (.-UNSIGNED_BYTE gl)  (.-image (nth textures 1)))
  (.texParameteri gl (.-TEXTURE_2D gl) (.-TEXTURE_MAG_FILTER gl) (.-LINEAR gl))
  (.texParameteri gl (.-TEXTURE_2D gl) (.-TEXTURE_MIN_FILTER gl) (.-LINEAR gl))

  (.bindTexture gl (.-TEXTURE_2D gl) (nth textures 2))
  (set! (.-number (nth textures 2)) "three")
  (.texImage2D gl (.-TEXTURE_2D gl) 0 (.-RGBA gl) (.-RGBA gl) (.-UNSIGNED_BYTE gl)  (.-image (nth textures 2)))
  (.texParameteri gl (.-TEXTURE_2D gl) (.-TEXTURE_MAG_FILTER gl) (.-LINEAR gl))
  (.texParameteri gl (.-TEXTURE_2D gl) (.-TEXTURE_MIN_FILTER gl) (.-LINEAR_MIPMAP_NEAREST gl))
  (.generateMipmap gl (.-TEXTURE_2D gl))
  
  (.bindTexture gl (.-TEXTURE_2D gl) nil))

(defn init-shaders [gl]
  (let [fragment-shader (js/getShader gl "shader-fs")
        vertex-shader (js/getShader gl "shader-vs")
        shader-program (.createProgram gl)]
    (.attachShader gl shader-program vertex-shader)
    (.attachShader gl shader-program fragment-shader)
    (.linkProgram gl shader-program)
    (.useProgram gl shader-program)

    (set! (.-vertexPositionAttribute shader-program) (.getAttribLocation gl shader-program "aVertexPosition"))
    (.enableVertexAttribArray gl (.-vertexPositionAttribute shader-program))

    (set! (.-textureCoordAttribute shader-program) (.getAttribLocation gl shader-program "aTextureCoord"))
    (.enableVertexAttribArray gl (.-textureCoordAttribute shader-program))

    (set! (.-pMatrixUniform shader-program) (.getUniformLocation gl shader-program "uPMatrix"))
    (set! (.-mvMatrixUniform shader-program) (.getUniformLocation gl shader-program "uMVMatrix"))
    (set! (.-samplerUniform shader-program) (.getUniformLocation gl shader-program "uSampler"))

    shader-program))

(defn set-matrix-uniforms [gl shader-program]
  (.uniformMatrix4fv gl (.-pMatrixUniform shader-program) false @p-matrix)
  (.uniformMatrix4fv gl (.-mvMatrixUniform shader-program) false @mv-matrix))

(defn init-buffers [gl]
  (let [c-vertices #js [-1 -1 1 ; front face
                        1 -1 1
                        1 1 1
                        -1 1 1
                        -1 -1 -1 ; back face
                        -1 1 -1
                        1 1 -1
                        1 -1 -1
                        -1 1 -1 ; top face
                        -1 1 1
                        1 1 1
                        1 1 -1
                        -1 -1 -1 ; bottom face
                        1 -1 -1
                        1 -1 1
                        -1 -1 1
                        1 -1 -1 ; right face
                        1 1 -1
                        1 1 1
                        1 -1 1
                        -1 -1 -1 ; left face
                        -1 -1 1
                        -1 1 1
                        -1 1 -1]
        c-texture-coords #js [0 0 ; front face
                              1 0
                              1 1
                              0 1
                              1 0 ; back face
                              1 1
                              0 1
                              0 0
                              0 1 ; top face
                              0 0
                              1 0
                              1 1
                              1 1 ; bottom face
                              0 1
                              0 0
                              1 0
                              1 0 ; right face
                              1 1
                              0 1
                              0 0
                              0 0 ; left face
                              1 0
                              1 1
                              0 1
                              ]
        c-indexes #js [0 1 2 0 2 3 ; front face
                   4 5 6 4 6 7
                   8 9 10 8 10 11
                   12 13 14 12 14 15
                   16 17 18 16 18 19
                   20 21 22 20 22 23
                       ]]
    
    (reset! cube-vertex-position-buffer (.createBuffer gl))
    (.bindBuffer gl (.-ARRAY_BUFFER gl) @cube-vertex-position-buffer)
    (.bufferData gl (.-ARRAY_BUFFER gl) (js/Float32Array. c-vertices) (.-STATIC_DRAW gl))
    (set! (.-itemSize @cube-vertex-position-buffer) 3)
    (set! (.-numItems @cube-vertex-position-buffer) 24)

    (reset! cube-vertex-texture-coord-buffer (.createBuffer gl))
    (.bindBuffer gl (.-ARRAY_BUFFER gl) @cube-vertex-texture-coord-buffer)
    (.bufferData gl (.-ARRAY_BUFFER gl) (js/Float32Array. c-texture-coords) (.-STATIC_DRAW gl))
    (set! (.-itemSize @cube-vertex-texture-coord-buffer) 2)
    (set! (.-numItems @cube-vertex-texture-coord-buffer) 24)

    (reset! cube-vertex-index-buffer (.createBuffer gl))
    (.bindBuffer gl (.-ELEMENT_ARRAY_BUFFER gl) @cube-vertex-index-buffer)
    (.bufferData gl (.-ELEMENT_ARRAY_BUFFER gl) (js/Uint16Array. c-indexes) (.-STATIC_DRAW gl))
    (set! (.-itemSize @cube-vertex-index-buffer) 1)
    (set! (.-numItems @cube-vertex-index-buffer) 36)))

(defn init-texture [gl]
  (let [crate-image (js/Image.)]
    (dotimes [n 3]
      (let [texture (.createTexture gl)]
        (set! (.-image texture) crate-image)
        (reset! crate-textures (conj @crate-textures texture))))
    (set! (.-onload crate-image) (partial handle-loaded-texture gl @crate-textures))
    (set! (.-src crate-image) "crate.gif")))

(defn draw-scene [gl shader-program]
  (.viewport gl 0 0 (.-viewportWidth gl) (.-viewportHeight gl))
  (.clear gl (bit-or (.-COLOR_BUFFER_BIT gl) (.-DEPTH_BUFFER_BIT gl)))
  
  (.perspective js/mat4 @p-matrix 45 (/ (.-viewportWidth gl) (.-viewportHeight gl)) 0.1 100.0)
  (.identity js/mat4 @mv-matrix)
  
  (.translate js/mat4 @mv-matrix @mv-matrix #js [0.0 0.0 @z])
  (.rotate js/mat4 @mv-matrix @mv-matrix (deg-to-rad @x-rot) #js [1 0 0])
  (.rotate js/mat4 @mv-matrix @mv-matrix (deg-to-rad @y-rot) #js [0 1 0])
  
  (.bindBuffer gl (.-ARRAY_BUFFER gl) @cube-vertex-position-buffer)
  (.vertexAttribPointer gl (.-vertexPositionAttribute shader-program) (.-itemSize @cube-vertex-position-buffer) (.-FLOAT gl) false 0 0)

  (.bindBuffer gl (.-ARRAY_BUFFER gl) @cube-vertex-texture-coord-buffer)
  (.vertexAttribPointer gl (.-textureCoordAttribute shader-program) (.-itemSize @cube-vertex-texture-coord-buffer) (.-FLOAT gl) false 0 0)

  (.activeTexture gl (.-TEXTURE0 gl))
  (.bindTexture gl (.-TEXTURE_2D gl) (nth @crate-textures @shader-filter))
  (.uniform1i gl (.-samplerUniform shader-program) 0)

  (.bindBuffer gl (.-ELEMENT_ARRAY_BUFFER gl) @cube-vertex-index-buffer)
  (set-matrix-uniforms gl shader-program)
  (.drawElements gl (.-TRIANGLES gl) (.-numItems @cube-vertex-index-buffer) (.-UNSIGNED_SHORT gl) 0))

(defn animate
  []
  (let [time-now (.getTime (js/Date.))
        elapsed (- time-now @last-time)]
    (if (not= @last-time 0)
      (do
        (reset! x-rot (+ @x-rot (/ (* @x-speed elapsed) 1000)))
        (reset! y-rot (+ @y-rot (/ (* @y-speed elapsed) 1000)))))
    (reset! last-time time-now)))

(defn tick
  []
  (js/requestAnimationFrame tick)
  (handle-keys)
  (draw-scene @gl-obj @shader-program-obj)
  (animate))

(defn webgl-start []
  (let [canvas (.getElementById js/document "main-board")
        gl (init-gl canvas)
        shader-program (init-shaders gl)]
    (init-buffers gl)
    (init-texture gl)

    (.clearColor gl 0.0 0.0 0.0 1.0)
    (.enable gl (.-DEPTH_TEST gl))

    (set! (.-onkeydown js/document) handle-key-down)
    (set! (.-onkeyup js/document) handle-key-up)

    (reset! gl-obj gl)
    (reset! shader-program-obj shader-program)

    (tick)))
