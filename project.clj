(defproject lein-solc "1.0.2"
  :description "lein plugin for compiling solidity contracts"
  :url "https://github.com/district0x/lein-solc"
  :license {:name "WTFPL"
            :url "http://www.wtfpl.net/"}
  :dependencies [[org.clojure/core.async "0.4.474"]
                 [org.clojure/core.match "0.3.0-alpha5"]
                 [clojure-watch "0.1.14"]]
  :eval-in-leiningen true)
