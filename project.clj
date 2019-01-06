(defproject lein-solc "1.1.11-SNAPSHOT"
  :description "lein plugin for compiling solidity contracts"
  :author "Filip Bielejec"
  :url "https://github.com/district0x/lein-solc"
  :license {:name "WTFPL"
            :url "http://www.wtfpl.net/"}

  :dependencies [[clj-antlr "0.2.4"]
                 [clojure-future-spec "1.9.0-beta4"]
                 [me.raynes/fs "1.4.6"]
                 [clojure-watch "0.1.14"]
                 [cheshire "5.8.1"]
                 [org.antlr/ST4 "4.0.8"]
                 [org.clojure/core.match "0.3.0-alpha5"]
                 [org.clojure/core.async "0.4.474"]]

  :resource-paths ["resources"]

  :deploy-repositories [["snapshots" {:url "https://clojars.org/repo"
                                      :username :env/clojars_username
                                      :password :env/clojars_password
                                      :sign-releases false}]
                        ["releases"  {:url "https://clojars.org/repo"
                                      :username :env/clojars_username
                                      :password :env/clojars_password
                                      :sign-releases false}]]

  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["deploy"]]

  :eval-in-leiningen true)
