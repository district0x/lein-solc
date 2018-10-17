(defproject lein-solc "1.9.9"
  :description "lein plugin for compiling solidity contracts"
  :url "https://github.com/district0x/lein-solc"
  :license {:name "WTFPL"
            :url "http://www.wtfpl.net/"}
  :dependencies [[clj-antlr "0.2.4"]
                 [clojure-future-spec "1.9.0-beta4"]
                 [clojure-watch "0.1.14"]
                 [org.antlr/ST4 "4.0.8"]
                 [org.clojure/core.match "0.3.0-alpha5"]
                 [org.clojure/core.async "0.4.474"]]
  :exclusions [[org.clojure/clojure]]
  :resource-paths ["resources"]
  :eval-in-leiningen true
  ;; :profiles {:user {:dependencies [[org.clojure/clojure "1.8.0"]]}}
  :repl-options {:init-ns ^:skip-aot shim.matches}
  )
