(ns craft.nbt
  "Binary NBT (Named Binary Tag) and Anvil region (.mca) parser and serializer in Clojure."
  (:refer-clojure :exclude [read-string])
  (:require [clojure.java.io :as io])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream InputStream File FileInputStream]
           [java.nio ByteBuffer ByteOrder]
           [java.util.zip GZIPInputStream GZIPOutputStream InflaterInputStream DeflaterOutputStream]))

(set! *warn-on-reflection* true)

(def ^:const TAG-END 0)
(def ^:const TAG-BYTE 1)
(def ^:const TAG-SHORT 2)
(def ^:const TAG-INT 3)
(def ^:const TAG-LONG 4)
(def ^:const TAG-FLOAT 5)
(def ^:const TAG-DOUBLE 6)
(def ^:const TAG-BYTE-ARRAY 7)
(def ^:const TAG-STRING 8)
(def ^:const TAG-LIST 9)
(def ^:const TAG-COMPOUND 10)
(def ^:const TAG-INT-ARRAY 11)
(def ^:const TAG-LONG-ARRAY 12)

(defn- read-nbt-str [^ByteBuffer buf]
  (let [len (bit-and (.getShort buf) 0xFFFF)
        bytes (byte-array len)]
    (.get buf bytes)
    (String. bytes "UTF-8")))

(declare read-tag-value)

(defn- read-compound [^ByteBuffer buf]
  (loop [m (transient {})]
    (let [child-type (int (.get buf))]
      (if (zero? child-type)
        (persistent! m)
        (let [tag-name (read-nbt-str buf)
              val (read-tag-value buf child-type)]
          (recur (assoc! m tag-name val)))))))

(defn- read-list [^ByteBuffer buf]
  (let [sub-type (int (.get buf))
        len (.getInt buf)]
    (if (or (<= len 0) (zero? sub-type))
      []
      (let [v (transient [])]
        (loop [i 0 acc v]
          (if (< i len)
            (recur (inc i) (conj! acc (read-tag-value buf sub-type)))
            (persistent! acc)))))))

(defn- read-tag-value [^ByteBuffer buf tag-type]
  (case (int tag-type)
    1 (.get buf)
    2 (.getShort buf)
    3 (.getInt buf)
    4 (.getLong buf)
    5 (.getFloat buf)
    6 (.getDouble buf)
    7 (let [len (.getInt buf)
            arr (byte-array len)]
        (.get buf arr)
        arr)
    8 (read-nbt-str buf)
    9 (read-list buf)
    10 (read-compound buf)
    11 (let [len (.getInt buf)
             arr (int-array len)]
         (dotimes [i len]
           (aset arr i (.getInt buf)))
         (vec arr))
    12 (let [len (.getInt buf)
             arr (long-array len)]
         (dotimes [i len]
           (aset arr i (.getLong buf)))
         (vec arr))
    nil))

(defn parse-nbt-buffer
  "Parses an NBT structure from a java.nio.ByteBuffer."
  [^ByteBuffer buf]
  (.order buf ByteOrder/BIG_ENDIAN)
  (let [root-type (int (.get buf))]
    (if (zero? root-type)
      {}
      (let [_root-name (read-nbt-str buf)]
        (read-tag-value buf root-type)))))

(defn read-stream-bytes ^bytes [^InputStream is]
  (let [baos (ByteArrayOutputStream.)
        buf (byte-array 8192)]
    (loop []
      (let [n (.read is buf)]
        (when (pos? n)
          (.write baos buf 0 n)
          (recur))))
    (.toByteArray baos)))

(defn read-gzip-nbt
  "Reads a GZIP-compressed NBT file (such as player .dat)."
  [file-or-path]
  (with-open [is (io/input-stream file-or-path)
              gz (GZIPInputStream. is)]
    (let [bytes (read-stream-bytes gz)
          buf (ByteBuffer/wrap bytes)]
      (parse-nbt-buffer buf))))

(defn read-raw-nbt
  "Reads an uncompressed or pre-decompressed NBT file."
  [file-or-path]
  (with-open [is (io/input-stream file-or-path)]
    (let [bytes (read-stream-bytes is)
          buf (ByteBuffer/wrap bytes)]
      (parse-nbt-buffer buf))))

(defn read-anvil-chunk-nbt
  "Reads and parses a specific chunk (chunk-x, chunk-z) from an Anvil .mca file."
  [^File mca-file chunk-x chunk-z]
  (when (.exists mca-file)
    (with-open [fis (FileInputStream. mca-file)]
      (let [all-bytes (read-stream-bytes fis)
            buf (ByteBuffer/wrap all-bytes)]
        (.order buf ByteOrder/BIG_ENDIAN)
        (let [rx (long (mod chunk-x 32))
              rz (long (mod chunk-z 32))
              rx (if (neg? rx) (+ rx 32) rx)
              rz (if (neg? rz) (+ rz 32) rz)
              loc-idx (int (* 4 (+ rx (* rz 32))))]
          (when (< (+ loc-idx 4) (alength all-bytes))
            (let [b0 (bit-and (int (.get buf loc-idx)) 0xFF)
                  b1 (bit-and (int (.get buf (int (+ loc-idx 1)))) 0xFF)
                  b2 (bit-and (int (.get buf (int (+ loc-idx 2)))) 0xFF)
                  b3 (bit-and (int (.get buf (int (+ loc-idx 3)))) 0xFF)
                  offset-sectors (bit-or (bit-shift-left b0 16)
                                         (bit-shift-left b1 8)
                                         b2)
                  sector-count b3]
              (when (and (pos? offset-sectors) (pos? sector-count))
                (let [byte-offset (int (* offset-sectors 4096))]
                  (.position buf byte-offset)
                  (let [length (.getInt buf)
                        compression-type (.get buf)]
                    (when (and (pos? length) (#{1 2} (int compression-type)))
                      (let [comp-bytes (byte-array (dec length))]
                        (.get buf comp-bytes)
                        (with-open [bais (ByteArrayInputStream. comp-bytes)
                                    ^InputStream inf (if (= (int compression-type) 1)
                                                       (GZIPInputStream. bais)
                                                       (InflaterInputStream. bais))]
                          (let [decomp (read-stream-bytes inf)]
                            (parse-nbt-buffer (ByteBuffer/wrap decomp))))))))))))))))

(defn- write-nbt-str [^ByteBuffer buf ^String s]
  (let [bytes (.getBytes (or s "") "UTF-8")]
    (.putShort buf (short (alength bytes)))
    (.put buf bytes)))

(declare write-tag-value)

(defn- write-compound [^ByteBuffer buf m]
  (doseq [[k v] m]
    (let [k-str (name k)
          tag-type (cond
                     (nil? v) nil
                     (instance? Byte v) TAG-BYTE
                     (instance? Short v) TAG-SHORT
                     (instance? Integer v) TAG-INT
                     (instance? Long v) TAG-LONG
                     (instance? Float v) TAG-FLOAT
                     (instance? Double v) TAG-DOUBLE
                     (bytes? v) TAG-BYTE-ARRAY
                     (string? v) TAG-STRING
                     (vector? v) (if (every? integer? v)
                                   (if (and (seq v) (instance? Long (first v))) TAG-LONG-ARRAY TAG-INT-ARRAY)
                                   TAG-LIST)
                     (map? v) TAG-COMPOUND
                     :else nil)]
      (when tag-type
        (.put buf (byte tag-type))
        (write-nbt-str buf k-str)
        (write-tag-value buf tag-type v))))
  (.put buf (byte TAG-END)))

(defn- write-tag-value [^ByteBuffer buf tag-type v]
  (case (int tag-type)
    1 (.put buf (byte v))
    2 (.putShort buf (short v))
    3 (.putInt buf (int v))
    4 (.putLong buf (long v))
    5 (.putFloat buf (float v))
    6 (.putDouble buf (double v))
    7 (let [^bytes arr v]
        (.putInt buf (alength arr))
        (.put buf arr))
    8 (write-nbt-str buf (str v))
    9 (let [elem-type (cond
                        (empty? v) TAG-END
                        (map? (first v)) TAG-COMPOUND
                        (string? (first v)) TAG-STRING
                        (instance? Double (first v)) TAG-DOUBLE
                        (instance? Float (first v)) TAG-FLOAT
                        (instance? Integer (first v)) TAG-INT
                        (instance? Long (first v)) TAG-LONG
                        :else TAG-COMPOUND)]
        (.put buf (byte elem-type))
        (.putInt buf (count v))
        (doseq [item v]
          (if (= elem-type TAG-COMPOUND)
            (write-compound buf item)
            (write-tag-value buf elem-type item))))
    10 (write-compound buf v)
    11 (do
         (.putInt buf (count v))
         (doseq [i v] (.putInt buf (int i))))
    12 (do
         (.putInt buf (count v))
         (doseq [l v] (.putLong buf (long l))))
    nil))

(defn write-nbt-to-stream!
  "Writes an NBT compound map to an OutputStream."
  [^java.io.OutputStream os root-map & [root-name]]
  (let [buf (ByteBuffer/allocate (* 4 1024 1024))]
    (.order buf ByteOrder/BIG_ENDIAN)
    (.put buf (byte TAG-COMPOUND))
    (write-nbt-str buf (or root-name ""))
    (write-compound buf root-map)
    (.flip buf)
    (let [arr (byte-array (.remaining buf))]
      (.get buf arr)
      (.write os arr))))

(defn write-gzip-nbt!
  "Writes an NBT compound map to a GZIP-compressed file."
  [file-or-path root-map & [root-name]]
  (with-open [fos (io/output-stream file-or-path)
              gz (GZIPOutputStream. fos)]
    (write-nbt-to-stream! gz root-map root-name)))

