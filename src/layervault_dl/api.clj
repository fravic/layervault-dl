(ns layervault-dl.api
  (:require [clojure.data.json :as json]))

(def LAYERVAULT-API "http://api.layervault.com/api/v2/")

(defn get-all-links
  [data object link]
  (get-in data [object 0 "links" link]))

(defn get-last-link
  [data object link]
  [(last (get-in data [object 0 "links" link]))])

(defn revision-download-url
  [data]
  [(get-in data ["revisions" 0 "lookup"])])

(defn file-last-revision
  [data]
  [(get-in data ["files" 0 "links" "last_revision"])])

(defn user-org-links [data] (get-all-links data "users" "organizations"))
(defn org-project-links [data] (get-all-links data "organizations" "projects"))
(defn project-folder-links [data] (get-all-links data "projects" "folders"))
(defn project-file-links [data] (get-all-links data "projects" "files"))
(defn folder-folder-links [data] (get-all-links data "folders" "folders"))
(defn folder-file-links [data] (get-all-links data "folders" "files"))
(defn file-revision-cluster-links [data] (get-all-links data "files" "revision_clusters"))
(defn revision-cluster-revision-last-link [data] (get-last-link data "revision_clusters" "revisions"))

(defn user-name [data] (get-in data ["users" 0 "first_name"]))
(defn org-name [data] (get-in data ["organizations" 0 "slug"]))
(defn project-name [data] (get-in data ["projects" 0 "slug"]))
(defn folder-name [data] (get-in data ["folders" 0 "slug"]))
(defn file-name [data] (get-in data ["files" 0 "slug"]))
(defn revision-cluster-name [data] (str "rev-" (get-in data ["revision_clusters" 0 "cluster_number"])))
(defn revision-name [data] (str "save-" (get-in data ["revisions" 0 "slug"])))

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
               file-last-revision query-revision}
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

(defn query-all
  [auth-token]
  (pr-str (query-user auth-token)))
