(defproject lein-solc "1.0.8-SNAPSHOT"
  :description "lein plugin for compiling solidity contracts"
  :url "https://github.com/district0x/lein-solc"
  :license {:name "WTFPL"
            :url "http://www.wtfpl.net/"}

  :dependencies [[org.clojure/core.async "0.4.474"]
                 [org.clojure/core.match "0.3.0-alpha5"]
                 [clojure-watch "0.1.14"]]

  :plugins [[lein-shell "0.5.0"]]

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
                  ["vcs" "commit" "Version %s [ci skip]"]
                  ["vcs" "tag" "v" "--no-sign"]
                  ["deploy"]
                  #_["change" "version" "leiningen.release/bump-version"]
                  #_["vcs" "commit" "Version %s [ci skip]"]
                  #_["vcs" "push"]]

  :eval-in-leiningen true)
