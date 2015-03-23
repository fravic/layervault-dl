(ns layervault-dl.core
  (:require [layervault-dl.api :as api]
            [layervault-dl.download :as download]))

(defn download-layervault-map
  [map-out auth-token]
  (spit map-out (api/query-all auth-token)))

(defn download-layervault-files
  [map-in dl-path auth-token]
  (download/download-all dl-path (read-string (slurp map-in)) auth-token))
