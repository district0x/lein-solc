(defproject lein-solc "1.9.9"
  :description "lein plugin for compiling solidity contracts"
  :url "https://github.com/district0x/lein-solc"
  :license {:name "WTFPL"
            :url "http://www.wtfpl.net/"}
  :dependencies [[org.clojure/core.async "0.4.474"]
                 [org.clojure/core.match "0.3.0-alpha5"]
                 [clojure-future-spec "1.9.0-beta4"]
                 [clj-antlr "0.2.4"]
                 [org.antlr/ST4 "4.0.8"]
                 [clojure-watch "0.1.14"]]
  :resource-paths ["resources"]
  ;; :eval-in-leiningen true
  )
