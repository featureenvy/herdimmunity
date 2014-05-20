(ns herdimmunity.webgl)

(def triangle-vertex-position-buffer (atom []))
(def triangle-vertex-color-buffer (atom []))
(def square-vertex-position-buffer (atom []))
(def square-vertex-color-buffer (atom []))
(def p-matrix (atom (.create js/mat4)))
(def mv-matrix (atom (.create js/mat4)))
(def mv-matrix-stack (atom []))
(def r-tri (atom 0))
(def r-square (atom 0))
(def last-time (atom 0))
(def gl-obj (atom 0))
(def shader-program-obj (atom 0))

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

    (set! (.-vertexColorAttribute shader-program) (.getAttribLocation gl shader-program "aVertexColor"))
    (.enableVertexAttribArray gl (.-vertexColorAttribute shader-program))

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
        t-colors #js [1.0 0.0 0.0 1.0
                      0.0 1.0 0.0 1.0
                      0.0 0.0 1.0 1.0]
        s-vertices #js [1.0 1.0 0.0
                        -1.0 1.0 0.0
                        1.0 -1.0 0.0
                        -1.0 -1.0 0.0]
        s-colors #js [0.5 0.5 1.0 1.0
                      0.5 0.5 1.0 1.0
                      0.5 0.5 1.0 1.0
                      0.5 0.5 1.0 1.0]]
    (reset! triangle-vertex-position-buffer (.createBuffer gl))
    (.bindBuffer gl (.-ARRAY_BUFFER gl) @triangle-vertex-position-buffer)
    (.bufferData gl (.-ARRAY_BUFFER gl) (js/Float32Array. t-vertices) (.-STATIC_DRAW gl))
    (set! (.-itemSize @triangle-vertex-position-buffer) 3)
    (set! (.-numItems @triangle-vertex-position-buffer) 3)

    (reset! triangle-vertex-color-buffer (.createBuffer gl))
    (.bindBuffer gl (.-ARRAY_BUFFER gl) @triangle-vertex-color-buffer)
    (.bufferData gl (.-ARRAY_BUFFER gl) (js/Float32Array. t-colors) (.-STATIC_DRAW gl))
    (set! (.-itemSize @triangle-vertex-color-buffer) 4)
    (set! (.-numItems @triangle-vertex-color-buffer) 3)

    (reset! square-vertex-position-buffer (.createBuffer gl))
    (.bindBuffer gl (.-ARRAY_BUFFER gl) @square-vertex-position-buffer)
    (.bufferData gl (.-ARRAY_BUFFER gl) (js/Float32Array. s-vertices) (.-STATIC_DRAW gl))
    (set! (.-itemSize @square-vertex-position-buffer) 3)
    (set! (.-numItems @square-vertex-position-buffer) 4)

    (reset! square-vertex-color-buffer (.createBuffer gl))
    (.bindBuffer gl (.-ARRAY_BUFFER gl) @square-vertex-color-buffer)
    (.bufferData gl (.-ARRAY_BUFFER gl) (js/Float32Array. s-colors) (.-STATIC_DRAW gl))
    (set! (.-itemSize @square-vertex-color-buffer) 4)
    (set! (.-numItems @square-vertex-color-buffer) 4)
    ))

(defn draw-scene [gl shader-program]
  (.viewport gl 0 0 (.-viewportWidth gl) (.-viewportHeight gl))
  (.clear gl (bit-or (.-COLOR_BUFFER_BIT gl) (.-DEPTH_BUFFER_BIT gl)))
  
  (.perspective js/mat4 @p-matrix 45 (/ (.-viewportWidth gl) (.-viewportHeight gl)) 0.1 100.0)
  (.identity js/mat4 @mv-matrix)
  
  (.translate js/mat4 @mv-matrix @mv-matrix #js [-1.5 0.0 -7.0])
  (mv-push-matrix)
  (.rotate js/mat4 @mv-matrix @mv-matrix (deg-to-rad @r-tri) #js [0 1 0])
  (.bindBuffer gl (.-ARRAY_BUFFER gl) @triangle-vertex-position-buffer)
  (.vertexAttribPointer gl (.-vertexPositionAttribute shader-program) (.-itemSize @triangle-vertex-position-buffer) (.-FLOAT gl) false 0 0)

  (.bindBuffer gl (.-ARRAY_BUFFER gl) @triangle-vertex-color-buffer)
  (.vertexAttribPointer gl (.-vertexColorAttribute shader-program) (.-itemSize @triangle-vertex-color-buffer) (.-FLOAT gl) false 0 0)
  
  (set-matrix-uniforms gl shader-program)
  (.drawArrays gl (.-TRIANGLES gl) 0 (.-numItems @triangle-vertex-position-buffer))

  (mv-pop-matrix)
  
  (.translate js/mat4 @mv-matrix @mv-matrix #js [3.0 0.0 0.0])
  (mv-push-matrix)
  (.rotate js/mat4 @mv-matrix @mv-matrix (deg-to-rad @r-square) #js [1 0 0])
  (.bindBuffer gl (.-ARRAY_BUFFER gl) @square-vertex-position-buffer)
  (.vertexAttribPointer gl (.-vertexPositionAttribute shader-program) (.-itemSize @square-vertex-position-buffer) (.-FLOAT gl) false 0 0)

  (.bindBuffer gl (.-ARRAY_BUFFER gl) @square-vertex-color-buffer)
  (.vertexAttribPointer gl (.-vertexColorAttribute shader-program) (.-itemSize @square-vertex-color-buffer) (.-FLOAT gl) false 0 0)
  
  (set-matrix-uniforms gl shader-program)
  (.drawArrays gl (.-TRIANGLE_STRIP gl) 0 (.-numItems @square-vertex-position-buffer)  (mv-pop-matrix)))

(defn animate
  []
  (let [time-now (.getTime (js/Date.))
        elapsed (- time-now @last-time)]
    (if (not= @last-time 0)
      (do
        (reset! r-tri (+ @r-tri (/ (* 90 elapsed) 1000)))
        (reset! r-square (+ @r-square (/ (* 75 elapsed) 1000)))))
    (reset! last-time time-now)))

(defn tick
  []
  (js/requestAnimationFrame tick)

  (draw-scene @gl-obj @shader-program-obj)
  (animate))

(defn webgl-start []
  (let [canvas (.getElementById js/document "main-board")
        gl (init-gl canvas)
        shader-program (init-shaders gl)]
    (init-buffers gl)

    (.clearColor gl 0.0 0.0 0.0 1.0)
    (.enable gl (.-DEPTH_TEST gl))

    (reset! gl-obj gl)
    (reset! shader-program-obj shader-program)

    (tick)))
