(ns ring.middleware.austin
  (:require [clojure.string :as str]
            [cemerick.austin.repls :refer (browser-connected-repl-js)]))

(defn- get-request? [request]
  (= (:request-method request) :get))

(defn- success? [response]
  (<= 200 (:status response) 299))

(defn- html-content? [response]
  (re-find
   #"text/html"
   (get-in response [:headers "Content-Type"])))

(defprotocol AsString
  (as-str [x]))

(extend-protocol AsString
  String
  (as-str [s] s)
  java.io.File
  (as-str [f] (slurp f))
  java.io.InputStream
  (as-str [i] (slurp i))
  clojure.lang.ISeq
  (as-str [xs] (apply str xs))
  nil
  (as-str [_] nil))

(defn- add-script [body]
  (if-let [body-str (as-str body)]
    (str/replace
     body-str
     #"</body\s*[^>]*>"
     #(str "<script type=\"text/javascript\" id=\"austin\">" (browser-connected-repl-js) "</script>" %))))

(defn- wrap-with-script [handler]
  (fn [request]
    (let [response (handler request)]
      (if (and (get-request? request)
               (success? response)
               (html-content? response))
        (-> response
            (update-in [:body] add-script)
            (update-in [:headers] dissoc "Content-Length"))
        response))))

(defn wrap-austin [handler]
  (wrap-with-script handler))
