(ns scan-castle-holes
  (:require [clojure.string :as str]
            [clojure.pprint :as pp])
  (:import [java.net Socket]
           [java.io BufferedReader InputStreamReader OutputStreamWriter PrintWriter]
           [java.nio.charset StandardCharsets]))

(defn send-cmd [line]
  (try
    (with-open [s (Socket. "127.0.0.1" 25595)]
      (.setTcpNoDelay s true)
      (.setSoTimeout s 2000)
      (let [in (BufferedReader. (InputStreamReader. (.getInputStream s) StandardCharsets/UTF_8))
            out (PrintWriter. (OutputStreamWriter. (.getOutputStream s) StandardCharsets/UTF_8) true)]
        (.println out line)
        (.readLine in)))
    (catch Throwable _ nil)))

(defn get-block [x y z]
  (second (str/split (or (send-cmd (str "BLOCK|" x "|" y "|" z)) "") #"\|")))

(defn air? [b]
  (or (nil? b) (str/includes? b "air") (str/includes? b "light")))

(defn scan-castle []
  (let [work-min-x -60 work-max-x -30
        work-min-z 220 work-max-z 255
        floor-drops (atom [])
        hollow-chambers (atom [])]
    (doseq [x (range -65 5 4)
            z (range 155 218 4)
            :when (not (and (<= work-min-x x work-max-x)
                            (>= z work-min-z)))
            y (range 90 120 4)]
      (let [b (get-block x y z)
            b-down (get-block x (dec y) z)
            b-deep (get-block x (- y 5) z)]
        (when (and (air? b) (air? b-down) (air? b-deep))
          (let [adj (for [[dx dz] [[-2 0] [2 0] [0 -2] [0 2]]]
                      (not (air? (get-block (+ x dx) (dec y) (+ z dz)))))]
            (when (some true? adj)
              (swap! floor-drops conj {:x x :y y :z z}))))))
    {:exclusion-zone {:x [work-min-x work-max-x] :z [work-min-z work-max-z]}
     :floor-drops-count (count @floor-drops)
     :samples (take 12 @floor-drops)}))

(pp/pprint (scan-castle))
