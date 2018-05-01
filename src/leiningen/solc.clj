(ns leiningen.solc
  (:require [clojure-watch.core :as watch]
            [clojure.core.async :as async :refer [<!!]]
            [clojure.java.shell :as sh]
            [clojure.string :as string]
            [leiningen.core.main :as lein]))

(defn sh! [{:keys [err-only] :as opts} & args]
  (lein/info (format "* Running shell command \"%s\"" (string/join " " args)))
  (let [{:keys [exit out err]} (apply sh/sh args)
        out (string/trim-newline out)]
    (when-not (and (zero? exit) err-only)
      (lein/warn (format "Command %s with the following result: \n %s" (if (zero? exit)
                                                                         "exited"
                                                                         "failed") err)))
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
    (future (watch/start-watch
             [{:path path
               :event-types [:modify]
               :bootstrap (fn [p] (lein/info (format "- Watching %s for file changes ..." p)))
               :callback (fn [_ file] (async/put! channel file))
               :options {:recursive true}}]))
    channel))

(defn compile-contract [{:keys [filename src-path build-path solc-err-only]}]
  (sh/with-sh-dir src-path
    (sh! {:err-only solc-err-only} "solc" "--overwrite" "--optimize" "--bin" "--abi" filename "-o" build-path)))

(defn solc
  "Lein plugin for compiling solidity contracts.
  Usage:
  `lein solc once` or `lein solc auto`"
  [project & [args]]
  (let [{:keys [src-path build-path contracts solc-err-only] :as opts} (:solc project)
        contracts-map (reduce (fn [m c]
                                (assoc m (str (ensure-slash src-path) c) c))
                              {}
                              contracts)
        full-build-path (-> (.getCanonicalPath (clojure.java.io/file "."))
                            ensure-slash
                            (str build-path))
        once (fn [] (doseq [c contracts]
                      (compile-contract {:filename c
                                         :src-path src-path
                                         :build-path full-build-path
                                         :solc-err-only solc-err-only})))
        auto (fn [] (let [watcher (start-watcher src-path)]
                      (while true
                        (let [filename (<!! watcher)]
                          (if (contains? (-> contracts-map keys set) filename)
                            (do (lein/info (format "%s has changed" filename))
                                (compile-contract {:filename (get contracts-map filename)
                                                   :src-path src-path
                                                   :build-path full-build-path
                                                   :solc-err-only solc-err-only}))
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
