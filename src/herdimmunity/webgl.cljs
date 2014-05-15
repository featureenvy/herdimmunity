(ns herdimmunity.webgl)

(def triangle-vertex-position-buffer (atom []))
(def square-vertex-position-buffer (atom []))
(def p-matrix (atom (.create js/mat4)))
(def mv-matrix (atom (.create js/mat4)))

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

    (set! (.-vertexPositionAttribute shader-program) (.getAttribLocation gl shader-program "aVertexPosition"))
    (.enableVertexAttribArray gl (.-vertexPositionAttribute shader-program))

    (set! (.-pMatrixUniform shader-program) (.getUniformLocation gl shader-program "uPMatrix"))
    (set! (.-mvMatrixUniform shader-program) (.getUniformLocation gl shader-program "uMVMatrix"))

    shader-program))

(defn set-matrix-uniforms [gl shader-program]
  (.uniformMatrix4fv gl (.-pMatrixUniform shader-program) false @p-matrix)
  (.uniformMatrix4fv gl (.-mvMatrixUniform shader-program) false @mv-matrix))

(defn init-buffers [gl]
  (let [t-vertices #js [0.0 1.0 0.0
                  -1.0 -1.0 0.0
                        1.0 -1.0 0.0]
        s-vertices #js [1.0 1.0 0.0
                        -1.0 1.0 0.0
                        1.0 -1.0 0.0
                        -1.0 -1.0 0.0]]
    (reset! triangle-vertex-position-buffer (.createBuffer gl))
    (.bindBuffer gl (.-ARRAY_BUFFER gl) @triangle-vertex-position-buffer)
    (.bufferData gl (.-ARRAY_BUFFER gl) (js/Float32Array. t-vertices) (.-STATIC_DRAW gl))
    (set! (.-itemSize @triangle-vertex-position-buffer) 3)
    (set! (.-numItems @triangle-vertex-position-buffer) 3)

    (reset! square-vertex-position-buffer (.createBuffer gl))
    (.bindBuffer gl (.-ARRAY_BUFFER gl) @square-vertex-position-buffer)
    (.bufferData gl (.-ARRAY_BUFFER gl) (js/Float32Array. s-vertices) (.-STATIC_DRAW gl))
    (set! (.-itemSize @square-vertex-position-buffer) 3)
    (set! (.-numItems @square-vertex-position-buffer) 4)))

(defn draw-scene [gl shader-program]
  (.viewport gl 0 0 (.-viewportWidth gl) (.-viewportHeight gl))
  (.clear gl (bit-or (.-COLOR_BUFFER_BIT gl) (.-DEPTH_BUFFER_BIT gl)))
  (.perspective js/mat4 @p-matrix 45 (/ (.-viewportWidth gl) (.-viewportHeight gl)) 0.1 100.0)
  (.identity js/mat4 @mv-matrix)
  (.translate js/mat4 @mv-matrix @mv-matrix #js [-1.5 0.0 -7.0])
  (.bindBuffer gl (.-ARRAY_BUFFER gl) @triangle-vertex-position-buffer)
  (.vertexAttribPointer gl (.-vertexPositionAttribute shader-program) (.-itemSize @triangle-vertex-position-buffer) (.-FLOAT gl) false 0 0)
  
  (set-matrix-uniforms gl shader-program)

  (.drawArrays gl (.-TRIANGLES gl) 0 (.-numItems @triangle-vertex-position-buffer))
  (.translate js/mat4 @mv-matrix @mv-matrix #js [3.0 0.0 0.0])
  (.bindBuffer gl (.-ARRAY_BUFFER gl) @square-vertex-position-buffer)
  (.vertexAttribPointer gl (.-vertexPositionAttribute shader-program) (.-itemSize @square-vertex-position-buffer) (.-FLOAT gl) false 0 0)
  (set-matrix-uniforms gl shader-program)

  (.drawArrays gl (.-TRIANGLE_STRIP gl) 0 (.-numItems @square-vertex-position-buffer)))

(defn webgl-start []
  (let [canvas (.getElementById js/document "main-board")
        gl (init-gl canvas)
        shader-program (init-shaders gl)]
    (init-buffers gl)

    (.clearColor gl 0.0 0.0 0.0 1.0)
    (.enable gl (.-DEPTH_TEST gl))

    (draw-scene gl shader-program)))
