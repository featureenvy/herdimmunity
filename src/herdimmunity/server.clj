(ns herdimmunity.server
  (:require [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.refresh :refer [wrap-refresh]]
            [ring.util.response :refer [file-response not-found]]
            [ring.middleware.file-info :refer [wrap-file-info]]
            [ring.middleware.austin :refer [wrap-austin]]
            [ring.server.standalone :refer [serve]]
            cemerick.austin.repls))

(defn handler [request]
  (or (file-response (:uri request) {:root "resources/public"})
      (not-found "File not found")))

(def app
  (-> #'handler
      (wrap-resource "public/")
      (wrap-file-info)
      (wrap-austin)))

(defn run []
  (serve app {:join? false
               :open-browser? false
               :stacktraces? true
               :auto-reload? true
               :auto-refresh? true}))

(defn brepl []
  (let [repl-env (reset! cemerick.austin.repls/browser-repl-env
                         (cemerick.austin/repl-env))]
    (cemerick.austin.repls/cljs-repl repl-env)))
