(ns craft.dashboard.proxy
  "Asynchronous reverse proxy forwarding /map/* and /bluemap/* to BlueMap on localhost:8100."
  (:require [org.httpkit.client :as http]))

(defn proxy-bluemap [req]
  (let [uri (:uri req)
        sub-path (cond
                   (clojure.string/starts-with? uri "/map/") (subs uri 4)
                   (clojure.string/starts-with? uri "/bluemap/") (subs uri 8)
                   :else uri)
        query (:query-string req)
        target (str "http://127.0.0.1:8100" sub-path (when query (str "?" query)))
        resp @(http/request {:method (:request-method req)
                             :url target
                             :headers (dissoc (:headers req) "host")
                             :body (:body req)
                             :as :byte-array
                             :timeout 10000})]
    (if (:error resp)
      {:status 502
       :headers {"Content-Type" "text/plain; charset=utf-8"}
       :body "502 Bad Gateway: BlueMap (127.0.0.1:8100) indisponível"}
      {:status (:status resp 200)
       :headers (select-keys (:headers resp)
                             ["content-type" "content-length" "cache-control" "etag" "last-modified" "location"])
       :body (:body resp)})))
