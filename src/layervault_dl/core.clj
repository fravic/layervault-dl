(ns layervault-dl.core
  (:require [clojure.string :as string]
            [clojure.data.json :as json]
            [clojure.tools.cli :refer [parse-opts]]))

;; LayerVault API queries

(def LAYERVAULT-API "http://api.layervault.com/api/v2/")

(defn endpoint
  [endpoint id access-token]
  (str LAYERVAULT-API endpoint
    (when id (str "/" id))
    "/?access_token=" access-token))

(defn get-link-ids
  [data]
  (get-in data ["users" 0 "links" "organizations"]))

(defn query-exec [callback url id access-token directory]
  (map callback
    (-> (endpoint url id access-token)
        slurp
        json/read-str
        get-link-ids)))

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
    (query-exec pprint "/me" nil (first arguments) (second arguments))))
