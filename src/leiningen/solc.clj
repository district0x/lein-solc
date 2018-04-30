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
      (lein/warn (format "Command failed with result %s %s" err out)))
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
               :bootstrap (fn [p] (lein/info (str "Watching " p " for file changes ...")))
               :callback (fn [_ file] (async/put! channel file))
               :options {:recursive true}}]))
    channel))

(defn compile-contract [filename src-path build-path]
  (sh! "solc" "--overwrite" "--optimize" "--bin" "--abi" (str src-path filename) "-o" build-path))

;; TODO : solc-error-only
;; solc "$@" 2>&1 | grep -A 2 -i "Error"
(defn solc
  "Lein plugin for compiling solidity contracts.
  Usage:
  `lein solc once` or `lein solc auto`"
  [project & [args]]
  (let [{:keys [src-path build-path contracts solc-err-only] :as opts} (:solc project)
        contracts-set (set contracts)
        safe-src-path (ensure-slash src-path)
        safe-build-path (ensure-slash build-path)
        once (fn [] (doseq [c contracts]
                      (compile-contract c safe-src-path safe-build-path)))
        auto (fn [] (let [watcher (start-watcher src-path)]
                      (while true
                        (let [filename (-> (<!! watcher)
                                           (string/split #"/")
                                           last)]
                          (if (contains? contracts-set filename)
                            (do (lein/info (str filename " has changed"))
                                (compile-contract filename safe-src-path safe-build-path))
                            (lein/info (str "Ignoring changes in " filename)))))))]
    (cond
      (not opts)
      (lein/abort "No `:solc` options map found in project.clj")

      (= "once" args)
      (once)

      (= "auto" args)
      (auto)

      :default (once))))
