(ns layervault-dl.core
  (:require [clojure.string :as string]
            [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [clojure.tools.cli :refer [parse-opts]]))

(def LAYERVAULT-API "http://api.layervault.com/api/v2/")

(def cli-options [])
(def usage "layervault-dl ACCESS_TOKEN OUTPUT_DIR")

(defn parse-user-data [data]
  (pprint data))

(defn query-user-data [access-token directory]
  (let [options {:query-params {:access_token access-token}}]
    (println options)
    (http/get (str LAYERVAULT-API "me") options
      (fn [{:keys [status headers body error]}]
        (if error
          (println "Failed! Error is" error)
          (-> body
            json/read-str
            parse-user-data))))))

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
    (query-user-data (first arguments) (second arguments))))
