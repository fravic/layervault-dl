(ns layervault-dl.download
  (:require [clojure.string :as string]
            [org.httpkit.client :as http]))

(def LAYERVAULT-DOWNLOAD "http://layervault.com/files/download_node/")

(defn guess-file-extension
  [path]
  (string/join (drop 2 (re-find #"--[a-z]+" path))))

(defn write-file
  [file path]
  (with-open [w (clojure.java.io/output-stream path)]
    (.write w (:body file))))

(defn path-drop-last [path]
  (string/join "/" (drop-last (string/split path #"/"))))

(defn mkdirp [path]
  (let [dir (java.io.File. path)]
    (if (.exists dir)
      true
      (.mkdirs dir))))

(defn download-file
  [path url]
  (mkdirp (path-drop-last path))
  (println (str "Downloading " path " from " url))
  (write-file @(http/get url {:insecure? true :as :byte-array}) path))

(defn download-all
  [path-so-far data-map access-token]
  (cond
    (= (type data-map) clojure.lang.PersistentList)
      (map #(download-all path-so-far % access-token) data-map)
    (= (type data-map) clojure.lang.PersistentArrayMap)
      (recur (str path-so-far "/" (-> data-map first first)) (-> data-map first second) access-token)
    (= (type data-map) java.lang.String)
      (download-file
        (str path-so-far "/" data-map "." (guess-file-extension path-so-far))
        (str LAYERVAULT-DOWNLOAD data-map "?access_token=" access-token))))
