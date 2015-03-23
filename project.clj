(defproject layervault-dl "0.1.0-SNAPSHOT"
  :description "Downloads all revisions from a LayerVault account"
  :dependencies [
    [org.clojure/clojure "1.6.0"]
    [org.clojure/tools.cli "0.3.1"]
    [org.clojure/data.json "0.2.6"]
    [http-kit "2.1.16"]
  ]
  :jvm-opts ["-Xmx1G"] ;; To download large layervault files
  :main layervault-dl.core)
