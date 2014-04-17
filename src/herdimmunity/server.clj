(ns herdimmunity.server
  (:require [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.refresh :refer [wrap-refresh]]
            [ring.util.response :refer [file-response not-found]]
            [ring.middleware.file-info :refer [wrap-file-info]]
            [ring.middleware.austin :refer [wrap-austin]]))

(defn handler [request]
  (or (file-response (:uri request) {:root "resources/public"})
      (not-found "File not found")))

(def app
  (-> #'handler
      (wrap-resource "public/")
      (wrap-file-info)
      (wrap-austin)))
