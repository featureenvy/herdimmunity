(ns herdimmunity.server
  (:require [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.refresh :refer [wrap-refresh]]
            [ring.util.response :refer [file-response not-found]]))

(defn handler [request]
  (or (file-response (:uri request) {:root "resources/public"})
      (not-found "File not found")))

(def app
  (-> #'handler
      (wrap-refresh)
      (wrap-resource "public/")))
