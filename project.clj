(defproject lein-solc "1.0.7-SNAPSHOT"
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
                  ["vcs" "commit" "Version ${:version} [ci skip]"]
                  ["vcs" "tag" "v" "--no-sign"]
                  #_["deploy"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit" "Version ${:version} [ci skip]"]
                  ["vcs" "push"]]

  #_[["vcs" "assert-committed"]
     ["change" "version" "leiningen.release/bump-version"]
     ["change" "version" "leiningen.release/bump-version" "release"]
     ["vcs" "commit" "-am" "Version ${:version} [ci skip]"]
     ["vcs" "tag" "v" "--no-sign"] ; disable signing and add "v" prefix
     ["deploy"]
     ["change" "version" "leiningen.release/bump-version" "qualifier"]
     ["vcs" "commit" "-am" "Version ${:version} [ci skip]"]
     ["vcs" "push"]]

  :eval-in-leiningen true)
