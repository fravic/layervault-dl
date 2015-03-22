(ns layervault-dl.core
  (:require [clojure.string :as string]
            [clojure.data.json :as json]
            [clojure.tools.cli :refer [parse-opts]]))


;; LayerVault API queries

(def LAYERVAULT-API "http://api.layervault.com/api/v2/")

(defn endpoint [endpoint id access-token]
  (str LAYERVAULT-API endpoint
    (when id (str "/" id))
    "/?access_token=" access-token))

(defn get-org-link-ids [data]
  (get-in data ["users" 0 "links" "organizations"]))

(defn get-project-link-ids [data]
  (get-in data ["organizations" 0 "links" "projects"]))

(defn query-exec [callback url id filter access-token]
  (map #(callback % access-token)
    (-> (endpoint url id access-token)
        slurp
        json/read-str
        filter)))

(defn query-project [id access-token]
  ;; print for now...
  (println (str "Found project id: " id)))

(defn query-org [id access-token]
  (println (str "Querying organization id..." id))
  (query-exec query-project "/organizations" id get-project-link-ids access-token))

(defn query-user [access-token]
  (println "Querying user...")
  (query-exec query-org "/me" nil get-org-link-ids access-token))


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
