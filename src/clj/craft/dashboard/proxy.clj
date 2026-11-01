(ns craft.dashboard.proxy
  "Asynchronous reverse proxy forwarding /map/*, /bluemap/*, and /maps/* to BlueMap on localhost:8100."
  (:require [clojure.string :as str]
            [org.httpkit.client :as http]))

(defn- normalize-headers
  "Converts http-kit keyword response headers to standard Ring response headers with proper case.
   Excludes hop-by-hop headers and content-encoding because http-kit client already decodes the body."
  [headers]
  (reduce-kv
   (fn [acc k v]
     (let [k-str (name k)
           k-lower (str/lower-case k-str)]
       (if (#{"transfer-encoding" "connection" "content-encoding"} k-lower)
         acc
         (assoc acc
                (case k-lower
                  "content-type" "Content-Type"
                  "cache-control" "Cache-Control"
                  "etag" "ETag"
                  "last-modified" "Last-Modified"
                  "location" "Location"
                  "content-length" "Content-Length"
                  k-str)
                (str v)))))
   {}
   headers))

(defn proxy-bluemap [req]
  (let [uri (:uri req)
        sub-path (cond
                   (str/starts-with? uri "/map/") (subs uri 4)
                   (str/starts-with? uri "/bluemap/") (subs uri 8)
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
      (let [body (:body resp)
            body-len (when (bytes? body) (count body))
            hdrs (cond-> (normalize-headers (:headers resp))
                   body-len (assoc "Content-Length" (str body-len)))]
        {:status (:status resp 200)
         :headers hdrs
         :body body}))))

