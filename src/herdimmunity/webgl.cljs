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
    (.enableVertexAttribArray gl (attrib-location gl shader-program "aVertexColor"))

    shader-program))

(defn init-buffers [gl]
  (let [s-vertices-buffer (.createBuffer gl)
        s-vertices #js [-1 -1 0
                        1 -1 0
                        1 1 0
                        -1 1 0]
        s-index-buffer (.createBuffer gl)
        s-indexes #js [0 1 2
                       0 2 3]
        s-color-buffer (.createBuffer gl)
        s-colors #js [0.5 0.5 1.0 1.0
                      0.5 0.5 1.0 1.0
                      0.5 0.5 1.0 1.0
                      0.5 0.5 1.0 1.0]]
    
    (.bindBuffer gl (.-ARRAY_BUFFER gl) s-vertices-buffer)
    (.bufferData gl (.-ARRAY_BUFFER gl) (js/Float32Array. s-vertices) (.-STATIC_DRAW gl))

    (.bindBuffer gl (.-ELEMENT_ARRAY_BUFFER gl) s-index-buffer)
    (.bufferData gl (.-ELEMENT_ARRAY_BUFFER gl) (js/Uint16Array. s-indexes) (.-STATIC_DRAW gl))

    (.bindBuffer gl (.-ARRAY_BUFFER gl) s-color-buffer)
    (.bufferData gl (.-ARRAY_BUFFER gl) (js/Float32Array. s-colors) (.-STATIC_DRAW gl))

    {:vertex-buffer s-vertices-buffer
     :index-buffer s-index-buffer
     :color-buffer s-color-buffer}))

(defn draw-scene
  [gl shader-program {:keys [vertex-buffer index-buffer color-buffer]}]
  (let [mv-matrix (.create js/mat4)
        p-matrix (.create js/mat4)]
    (.viewport gl 0 0 (.-viewportWidth gl) (.-viewportHeight gl))
    (.clear gl (bit-or (.-COLOR_BUFFER_BIT gl) (.-DEPTH_BUFFER_BIT gl)))
    
    (.perspective js/mat4 p-matrix 45 (/ (.-viewportWidth gl) (.-viewportHeight gl)) 0.1 100.0)
    
    (.translate js/mat4 mv-matrix mv-matrix #js [0 0 -7])
    
    (.bindBuffer gl (.-ARRAY_BUFFER gl) vertex-buffer)
    (.vertexAttribPointer gl (attrib-location gl shader-program "aVertexPosition") 3 (.-FLOAT gl) false 0 0)

    (.bindBuffer gl (.-ARRAY_BUFFER gl) color-buffer)
    (.vertexAttribPointer gl (attrib-location gl shader-program "aVertexColor") 4 (.-FLOAT gl) false 0 0)

    (.bindBuffer gl (.-ELEMENT_ARRAY_BUFFER gl) index-buffer)
    (set-matrix-uniforms gl shader-program p-matrix mv-matrix)
    (.drawElements gl (.-TRIANGLES gl) 6 (.-UNSIGNED_SHORT gl) 0)))

(defn tick
  [gl shader-program buffers]
  (js/requestAnimationFrame (partial tick gl shader-program buffers))
  (draw-scene gl shader-program buffers))

(defn webgl-start []
  (let [canvas (.getElementById js/document "main-board")
        gl (init-gl canvas)
        shader-program (init-shaders gl)
        buffers (init-buffers gl)]

    (.clearColor gl 0.0 0.0 0.0 1.0)
    (.enable gl (.-DEPTH_TEST gl))

    (tick gl shader-program buffers)))
