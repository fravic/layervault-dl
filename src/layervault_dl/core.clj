(ns layervault-dl.core
  (:require [clojure.string :as string]
            [clojure.data.json :as json]
            [clojure.tools.cli :refer [parse-opts]]))


;; LayerVault API parsing

(defn get-all-links
  [data object link]
  (get-in data [object 0 "links" link]))

(defn get-last-link
  [data object link]
  [(last (get-in data [object 0 "links" link]))])

(defn user-org-links [data] (get-all-links data "users" "organizations"))
(defn org-project-links [data] (get-all-links data "organizations" "projects"))
(defn project-folder-links [data] (get-all-links data "projects" "folders"))
(defn project-file-links [data] (get-all-links data "projects" "files"))
(defn folder-folder-links [data] (get-all-links data "folders" "folders"))
(defn folder-file-links [data] (get-all-links data "folders" "files"))
(defn file-revision-cluster-links [data] (get-all-links data "files" "revision_clusters"))
(defn file-revision-last-link [data] (get-last-link data "files" "revisions"))
(defn revision-cluster-revision-last-link [data] (get-last-link data "files" "revisions"))


;; LayerVault API queries

(def LAYERVAULT-API "http://api.layervault.com/api/v2/")

(defn endpoint [endpoint id access-token]
  (str LAYERVAULT-API endpoint
    (when id (str "/" id))
    "/?access_token=" access-token))

(defn query-exec-filter [filter callback data access-token]
  (map
    #(callback % access-token)
    (filter data)))

(defn query-exec [link-functions url id access-token]
  (println (str (endpoint url id access-token)))
  (let [data (-> (endpoint url id access-token)
                 slurp
                 json/read-str)]
    (map
      #(query-exec-filter % (get link-functions %) data access-token)
      (keys link-functions))))

(defn query-revision [id access-token]
  (println (str "Querying revision..." id))
  id)

(defn query-revision-cluster [id access-token]
  (println (str "Querying revision cluster..." id))
  (query-exec {revision-cluster-revision-last-link query-revision}
               "revision_clusters" id access-token))

(defn query-file [id access-token]
  (println (str "Querying file..." id))
  (query-exec {file-revision-cluster-links query-revision-cluster
               file-revision-last-link query-revision}
               "files" id access-token))

(defn query-folder [id access-token]
  (println (str "Querying folder..." id))
  (query-exec {folder-folder-links query-folder
               folder-file-links query-file}
               "folders" id access-token))

(defn query-project [id access-token]
  (println (str "Querying project id... " id))
  (query-exec {project-folder-links query-folder
               project-file-links query-file}
               "projects" id access-token))

(defn query-org [id access-token]
  (println (str "Querying organization id..." id))
  (query-exec {org-project-links query-project} "organizations" id access-token))

(defn query-user [access-token]
  (println "Querying user...")
  (query-exec {user-org-links query-org} "me" nil access-token))


;; Entry point

(def usage "layervault-dl ACCESS_TOKEN OUTPUT_DIR")
(def cli-options [])

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main [& args]
  "Downloads revisions for the given access token into the specified directory"
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (not= (count arguments) 2) (exit 1 usage)
      errors (exit 1 (error-msg errors)))
    (query-user (first arguments))))
