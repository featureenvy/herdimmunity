(ns herdimmunity.webgl)

(def pyramid-vertex-position-buffer (atom []))
(def pyramid-vertex-color-buffer (atom []))
(def cube-vertex-position-buffer (atom []))
(def cube-vertex-color-buffer (atom []))
(def cube-vertex-index-buffer (atom []))
(def p-matrix (atom (.create js/mat4)))
(def mv-matrix (atom (.create js/mat4)))
(def mv-matrix-stack (atom []))
(def r-pyramid (atom 0))
(def r-cube (atom 0))
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
  (let [p-vertices #js [; front face
                        0 1 0
                        -1 -1 1
                        1 -1 1
                                        ; right face
                        0 1 0
                        1 -1 1
                        1 -1 -1
                                        ; back face
                        0 1 0
                        1 -1 -1
                        -1 -1 -1
                                        ; left face
                        0 1 0
                        -1 -1 -1
                        -1 -1 1]
        p-colors #js [1.0 0.0 0.0 1.0 ; front face
                      0.0 1.0 0.0 1.0
                      0.0 0.0 1.0 1.0
                      1 0 0 1 ; right face
                      0 0 1 1
                      0 1 0 1
                      1 0 0 1 ; back face
                      0 1 0 1
                      0 0 1 1
                      1 0 0 1 ; left face
                      0 0 1 1
                      0 1 0 1]
        c-vertices #js [-1 -1 1 ; front face
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
        c-single-colors  [[1 0 0 1] ; front face
                          [1 1 0 1] ; back face
                          [0 1 0 1] ; top face
                          [1 .5 .5 1] ; bottom face
                          [1 0 1 1] ; right face
                          [0 0 1 1] ; left face
                          ]
        c-colors (clj->js (flatten (map (fn [val] (repeatedly 4 #(identity val))) c-single-colors)))
        c-indexes #js [0 1 2 0 2 3 ; front face
                   4 5 6 4 6 7
                   8 9 10 8 10 11
                   12 13 14 12 14 15
                   16 17 18 16 18 19
                   20 21 22 20 22 23
                   ]]
    (reset! pyramid-vertex-position-buffer (.createBuffer gl))
    (.bindBuffer gl (.-ARRAY_BUFFER gl) @pyramid-vertex-position-buffer)
    (.bufferData gl (.-ARRAY_BUFFER gl) (js/Float32Array. p-vertices) (.-STATIC_DRAW gl))
    (set! (.-itemSize @pyramid-vertex-position-buffer) 3)
    (set! (.-numItems @pyramid-vertex-position-buffer) 12)

    (reset! pyramid-vertex-color-buffer (.createBuffer gl))
    (.bindBuffer gl (.-ARRAY_BUFFER gl) @pyramid-vertex-color-buffer)
    (.bufferData gl (.-ARRAY_BUFFER gl) (js/Float32Array. p-colors) (.-STATIC_DRAW gl))
    (set! (.-itemSize @pyramid-vertex-color-buffer) 4)
    (set! (.-numItems @pyramid-vertex-color-buffer) 12)

    (reset! cube-vertex-position-buffer (.createBuffer gl))
    (.bindBuffer gl (.-ARRAY_BUFFER gl) @cube-vertex-position-buffer)
    (.bufferData gl (.-ARRAY_BUFFER gl) (js/Float32Array. c-vertices) (.-STATIC_DRAW gl))
    (set! (.-itemSize @cube-vertex-position-buffer) 3)
    (set! (.-numItems @cube-vertex-position-buffer) 24)

    (reset! cube-vertex-color-buffer (.createBuffer gl))
    (.bindBuffer gl (.-ARRAY_BUFFER gl) @cube-vertex-color-buffer)
    (.bufferData gl (.-ARRAY_BUFFER gl) (js/Float32Array. c-colors) (.-STATIC_DRAW gl))
    (set! (.-itemSize @cube-vertex-color-buffer) 4)
    (set! (.-numItems @cube-vertex-color-buffer) 24)

    (reset! cube-vertex-index-buffer (.createBuffer gl))
    (.bindBuffer gl (.-ELEMENT_ARRAY_BUFFER gl) @cube-vertex-index-buffer)
    (.bufferData gl (.-ELEMENT_ARRAY_BUFFER gl) (js/Uint16Array. c-indexes) (.-STATIC_DRAW gl))
    (set! (.-itemSize @cube-vertex-index-buffer) 1)
    (set! (.-numItems @cube-vertex-index-buffer) 36)))

(defn draw-scene [gl shader-program]
  (.viewport gl 0 0 (.-viewportWidth gl) (.-viewportHeight gl))
  (.clear gl (bit-or (.-COLOR_BUFFER_BIT gl) (.-DEPTH_BUFFER_BIT gl)))
  
  (.perspective js/mat4 @p-matrix 45 (/ (.-viewportWidth gl) (.-viewportHeight gl)) 0.1 100.0)
  (.identity js/mat4 @mv-matrix)
  
  (.translate js/mat4 @mv-matrix @mv-matrix #js [-1.5 0.0 -7.0])
  (mv-push-matrix)
  (.rotate js/mat4 @mv-matrix @mv-matrix (deg-to-rad @r-pyramid) #js [0 1 0])
  (.bindBuffer gl (.-ARRAY_BUFFER gl) @pyramid-vertex-position-buffer)
  (.vertexAttribPointer gl (.-vertexPositionAttribute shader-program) (.-itemSize @pyramid-vertex-position-buffer) (.-FLOAT gl) false 0 0)

  (.bindBuffer gl (.-ARRAY_BUFFER gl) @pyramid-vertex-color-buffer)
  (.vertexAttribPointer gl (.-vertexColorAttribute shader-program) (.-itemSize @pyramid-vertex-color-buffer) (.-FLOAT gl) false 0 0)
  
  (set-matrix-uniforms gl shader-program)
  (.drawArrays gl (.-TRIANGLES gl) 0 (.-numItems @pyramid-vertex-position-buffer))

  (mv-pop-matrix)
  
  (.translate js/mat4 @mv-matrix @mv-matrix #js [3.0 0.0 0.0])
  (mv-push-matrix)
  (.rotate js/mat4 @mv-matrix @mv-matrix (deg-to-rad @r-cube) #js [1 1 1])
  (.bindBuffer gl (.-ARRAY_BUFFER gl) @cube-vertex-position-buffer)
  (.vertexAttribPointer gl (.-vertexPositionAttribute shader-program) (.-itemSize @cube-vertex-position-buffer) (.-FLOAT gl) false 0 0)

  (.bindBuffer gl (.-ARRAY_BUFFER gl) @cube-vertex-color-buffer)
  (.vertexAttribPointer gl (.-vertexColorAttribute shader-program) (.-itemSize @cube-vertex-color-buffer) (.-FLOAT gl) false 0 0)

  (.bindBuffer gl (.-ELEMENT_ARRAY_BUFFER gl) @cube-vertex-index-buffer)
  (set-matrix-uniforms gl shader-program)
  (.drawElements gl (.-TRIANGLES gl) (.-numItems @cube-vertex-index-buffer) (.-UNSIGNED_SHORT gl) 0)
  ;(.drawArrays gl (.-TRIANGLE_STRIP gl) 0 (.-numItems @cube-vertex-position-buffer))

  (mv-pop-matrix)
  )

(defn animate
  []
  (let [time-now (.getTime (js/Date.))
        elapsed (- time-now @last-time)]
    (if (not= @last-time 0)
      (do
        (reset! r-pyramid (+ @r-pyramid (/ (* 90 elapsed) 1000)))
        (reset! r-cube (- @r-cube (/ (* 75 elapsed) 1000)))))
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
