(ns layervault-dl.core
  (:require [clojure.string :as string]
            [clojure.data.json :as json]
            [clojure.tools.cli :refer [parse-opts]]
            [org.httpkit.client :as http]))


;; LayerVault API parsing

(defn get-all-links
  [data object link]
  (get-in data [object 0 "links" link]))

(defn get-last-link
  [data object link]
  [(last (get-in data [object 0 "links" link]))])

(defn revision-download-url
  [data]
  [(get-in data ["revisions" 0 "lookup"])])

(defn user-org-links [data] (get-all-links data "users" "organizations"))
(defn org-project-links [data] (get-all-links data "organizations" "projects"))
(defn project-folder-links [data] (get-all-links data "projects" "folders"))
(defn project-file-links [data] (get-all-links data "projects" "files"))
(defn folder-folder-links [data] (get-all-links data "folders" "folders"))
(defn folder-file-links [data] (get-all-links data "folders" "files"))
(defn file-revision-cluster-links [data] (get-all-links data "files" "revision_clusters"))
(defn file-revision-last-link [data] (get-last-link data "files" "revisions"))
(defn revision-cluster-revision-last-link [data] (get-last-link data "revision_clusters" "revisions"))

(defn user-name [data] (get-in data ["users" 0 "first_name"]))
(defn org-name [data] (get-in data ["organizations" 0 "slug"]))
(defn project-name [data] (get-in data ["projects" 0 "slug"]))
(defn folder-name [data] (get-in data ["folders" 0 "slug"]))
(defn file-name [data] (get-in data ["files" 0 "slug"]))
(defn revision-cluster-name [data] (str "rev-" (get-in data ["revision_clusters" 0 "cluster_number"])))
(defn revision-name [data] (str "save-" (get-in data ["revisions" 0 "slug"])))


;; LayerVault API queries

(def LAYERVAULT-API "http://api.layervault.com/api/v2/")

(defn endpoint [endpoint id access-token]
  (str LAYERVAULT-API endpoint
    (when id (str "/" id))
    "/?access_token=" access-token))

(defn query-exec-filter [filter callback data naming-fn access-token]
  {(naming-fn data)
    (doall (map
      #(callback % access-token)
      (filter data)))})

(defn query-exec [link-functions url id naming-fn access-token]
  (println (str (endpoint url id access-token)))
  (let [data (-> (endpoint url id access-token)
                 slurp
                 json/read-str)]
    (doall (map
      #(query-exec-filter % (get link-functions %) data naming-fn access-token)
      (keys link-functions)))))

(defn download-url [url access-token]
  url)

(defn query-revision [id access-token]
  (query-exec {revision-download-url download-url}
               "revisions" id revision-name access-token))

(defn query-revision-cluster [id access-token]
  (query-exec {revision-cluster-revision-last-link query-revision}
               "revision_clusters" id revision-cluster-name access-token))

(defn query-file [id access-token]
  (query-exec {file-revision-cluster-links query-revision-cluster
               file-revision-last-link query-revision}
               "files" id file-name access-token))

(defn query-folder [id access-token]
  (query-exec {folder-folder-links query-folder
               folder-file-links query-file}
               "folders" id folder-name access-token))

(defn query-project [id access-token]
  (query-exec {project-folder-links query-folder
               project-file-links query-file}
               "projects" id project-name access-token))

(defn query-org [id access-token]
  (query-exec {org-project-links query-project} "organizations" id org-name access-token))

(defn query-user [access-token]
  (query-exec {user-org-links query-org} "me" nil user-name access-token))


;; Download management

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

(defn download-data-map
  [path-so-far data-map access-token]
  (cond
    (= (type data-map) clojure.lang.PersistentList)
      (map #(recur path-so-far % access-token) data-map)
    (= (type data-map) clojure.lang.PersistentArrayMap)
      (recur (str path-so-far "/" (-> data-map first first)) (-> data-map first second) access-token)
    (= (type data-map) java.lang.String)
      (download-file
        (str path-so-far "/" data-map "." (guess-file-extension path-so-far))
        (str LAYERVAULT-DOWNLOAD data-map "?access_token=" access-token))))


;; Executable functions

(defn download-layervault-map
  [map-out auth-token]
  (spit map-out (pr-str (query-user auth-token))))

(defn download-layervault-files
  [map-in dl-path auth-token]
  (download-data-map dl-path (read-string (slurp map-in)) auth-token))
