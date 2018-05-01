(ns leiningen.solc
  (:require [clojure-watch.core :as watch]
            [clojure.core.async :as async :refer [<!!]]
            [clojure.java.shell :as sh]
            [clojure.string :as string]
            [leiningen.core.main :as lein]))

(defn sh! [& args]
  (lein/info (format "* Running shell command \"%s\"" (string/join " " args)))
  (let [{:keys [exit out err]} (apply sh/sh args)
        out (string/trim-newline out)]
    (when-not (zero? exit)
      (lein/warn (format "Command failed with the following result: \n %s %s" err out)))
    (when-not (string/blank? out)
      (lein/info out))
    out))

(defn ensure-slash [path]
  (if (string/ends-with? path "/")
    path
    (str path "/")))

(defn start-watcher
  [path]
  (let [channel (async/chan)]
    (watch/start-watch
     [{:path path
       :event-types [:modify]
       :bootstrap (fn [p] (lein/info (format "- Watching %s for file changes ..." p)))
       :callback (fn [_ file] (async/put! channel file))
       :options {:recursive true}}])
    channel))

(defn compile-contract [filename src-path build-dir]
  (sh/with-sh-dir src-path
    (sh! "solc" "--overwrite" "--optimize" "--bin" "--abi" filename "-o" build-dir)))

(defn solc
  "Lein plugin for compiling solidity contracts.
  Usage:
  `lein solc once` or `lein solc auto`"
  [project & [args]]
  (let [{:keys [src-path build-path contracts solc-err-only] :as opts} (:solc project)
        contracts-set (set contracts)
        build-dir (-> (.getCanonicalPath (clojure.java.io/file "."))
                      ensure-slash
                      (str build-path))
        once (fn [] (doseq [c contracts]
                      (compile-contract c src-path build-dir)))
        auto (fn [] (let [watcher (start-watcher src-path)]
                      (while true
                        (let [filename (-> (<!! watcher)
                                           (string/split #"/")
                                           last)]
                          (if (contains? contracts-set filename)
                            (do (lein/info (format "%s has changed" filename))
                                (compile-contract filename src-path build-dir))
                            (lein/info (format "Ignoring changes in %s" filename)))))))]
    (cond
      (not opts)
      (lein/abort "No `:solc` options map found in project.clj")

      (= "once" args)
      (once)

      (= "auto" args)
      (do (once)
          (auto))

      :default (once))))
