(ns leiningen.solc
  (:require [clojure-watch.core :as watch]
            [clojure.core.async :as async :refer [<!!]]
            [clojure.core.match :refer [match]]
            [clojure.java.shell :as sh]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [leiningen.core.main :as lein]))

(defn sh! [{:keys [err-only? verbose?] :as opts} & args]
  (when verbose?
    (lein/info (format "* Running shell command \"%s\"" (string/join " " args))))
  (let [{:keys [exit out err]} (apply sh/sh args)
        out (string/trim-newline out)
        output? (not (string/blank? out))
        error-or-warning? (not (string/blank? err))
        zero-exit? (zero? exit)
        warning? (and error-or-warning? zero-exit? )
        error? (and error-or-warning? (not zero-exit?))]
    (match [err-only? error? warning? verbose? output?]
           [_ true _ true _] (lein/warn (format "Command failed with the following result: %s" err))
           [_ true _ false _] (lein/warn err)
           [true false _ _ _] nil
           [_ false _ true true] (lein/info (format "Command exited with the following result: %s" out))
           [_ false _ false true] (lein/info out)
           [false false true true _] (lein/warn (format "Command exited with the following result: %s" err))
           [false false true false _] (lein/warn err)
           :else (lein/info {:error-or-warning? error-or-warning?
                             :zero-exit? zero-exit?
                             :err-only? err-only?
                             :error? error?
                             :warning? warning?
                             :verbose? verbose?
                             :output? output?}))
    exit))

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

(defn compile-contract [{:keys [filename src-path build-path solc-err-only verbose wc]}]
  (sh/with-sh-dir src-path
    (let [exit-status (sh! {:err-only? solc-err-only :verbose? verbose} "solc" "--overwrite" "--optimize" "--bin" "--abi" filename "-o" build-path)]
      (when (and (zero? exit-status) wc)
        (sh! {:err-only? nil :verbose? verbose} "wc" "-c" (str (ensure-slash build-path)
                                                               (-> (-> filename
                                                                       (string/split #"/")
                                                                       last)
                                                                   (string/split #"\.")
                                                                   first
                                                                   (str ".bin"))))))))

(defn walk-dir [path pattern subdirs?]
  (doall (filter #(re-matches pattern (.getName %))
                 (if subdirs?
                   (file-seq (io/file path))
                   (.listFiles (io/file path))))))

(defn solc
  "Lein plugin for compiling solidity contracts.
  Usage:
  `lein solc once` or `lein solc auto`"
  [project & [args]]
  (let [{:keys [src-path build-path contracts solc-err-only verbose wc] :as opts} (:solc project)
        contracts-map (cond (sequential? contracts)
                            (reduce (fn [m c]
                                      (assoc m (str (ensure-slash src-path) c) c))
                                    {}
                                    contracts)

                            (= :all contracts)
                            (reduce (fn [m f]
                                      (let [path (.getPath f)]
                                        (assoc m path (-> path
                                                          (string/split #"/")
                                                          last))))
                                    {}
                                    (walk-dir src-path #".*\.sol" false))

                            :else (lein/abort "Unknown `:contracts` option found in project.clj"))

        full-build-path (-> (.getCanonicalPath (clojure.java.io/file "."))
                            ensure-slash
                            (str build-path))
        once (fn [] (cond (sequential? contracts)
                          (doseq [c contracts]
                            (compile-contract {:filename c
                                               :src-path src-path
                                               :build-path full-build-path
                                               :solc-err-only solc-err-only
                                               :verbose verbose
                                               :wc wc}))

                          (= :all contracts)
                          (doseq [[path c] contracts-map]
                            (compile-contract {:filename (-> path
                                                             (string/split (re-pattern (ensure-slash src-path)))
                                                             last)
                                               :src-path src-path
                                               :build-path full-build-path
                                               :solc-err-only solc-err-only
                                               :verbose verbose
                                               :wc wc}))))
        auto (fn [] (let [watcher (start-watcher src-path)]
                      (while true
                        (let [filename (<!! watcher)]
                          (if (contains? (-> contracts-map keys set) filename)
                            (do (lein/info (format "%s has changed" filename))
                                (compile-contract {:filename (get contracts-map filename)
                                                   :src-path src-path
                                                   :build-path full-build-path
                                                   :solc-err-only solc-err-only
                                                   :verbose verbose
                                                   :wc wc}))
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
