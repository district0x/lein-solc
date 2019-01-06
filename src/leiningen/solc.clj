(ns leiningen.solc
  (:require
   [cheshire.core :as json]
   [clojure-watch.core :as watch]
   [clojure.core.async :as async :refer [<!!]]
   [clojure.core.match :refer [match]]
   [clojure.java.io :as io]
   [clojure.java.shell :as sh]
   [clojure.string :as string]
   [leiningen.core.main :as lein]
   [fs.fs :as fs]
   [shim.matches :as matches]))

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
    [exit out]))

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

(defn count-bytes [{:keys [build-path verbose contract-name filename]}]
  ;; contracts import other contracts, which causes the compiled output to be produced multiple times
  ;; this is to avoid printing the bytecount multiple times for the same contract
  ;; only contracts in the :contracts vector will be reported
  ;; if :all is used this means contracts in the root of src-dir
  (when (= contract-name (-> (-> filename
                                 (string/split #"/")
                                 last)
                             (string/split #"\.")
                             first))
    (sh! {:err-only? nil :verbose? verbose} "wc" "-c" (str (fs/ensure-slash build-path)
                                                           contract-name
                                                           ".bin"))))

;; TODO : if artifact exists update :updatedAt :bytecode and :abi
(defn write-truffle-artifact [{:keys [src-path build-path contract-name abi bin]}]
  (let [bin (if (empty? bin) "0x" bin)]
    (spit (str (fs/ensure-slash build-path)
               contract-name
               ".json")
          (json/generate-string
           {:contractName contract-name
            :abi (json/parse-string abi)
            :bytecode bin
            :sourcePath (-> (str (fs/ensure-slash src-path) contract-name ".sol"))
            :networks {}
            :schemaVersion "2.0.1"
            :updatedAt (new java.util.Date)}
           {:pretty true}))))

(defn write-abi [{:keys [build-path contract-name abi]}]
  (spit (str (fs/ensure-slash build-path)
             contract-name
             ".abi")
        abi))

(defn write-bin [{:keys [build-path contract-name bin]}]
  (spit (str (fs/ensure-slash build-path)
             contract-name
             ".bin")
        bin))

(defn compile-contract [{:keys [filename src-path build-path
                                abi? bin? truffle-artifacts?
                                solc-err-only verbose byte-count
                                optimize-runs
                                temp-src-path shim]
                         :as opts-map}]

  (when shim
    (let [code (slurp (str (fs/ensure-slash src-path) filename))]
      (cond->> code
        (some #(= :matches %) shim) matches/shim
        ;; after shimming write to temp dir
        true (spit (str (fs/ensure-slash temp-src-path) filename)))))

  (let [src-path (if shim
                   ;; use temp dir if source code is shimmed
                   temp-src-path
                   src-path)
        runs (or (cond
                   (integer? optimize-runs)
                   optimize-runs

                   (map? optimize-runs)
                   (get optimize-runs filename))
                 200)]

    (sh/with-sh-dir src-path
      (let [[exit-status output] (sh! {:err-only? solc-err-only :verbose? verbose} "solc"
                                      "--overwrite"
                                      "--optimize"
                                      "--optimize-runs" (str runs)
                                      "--bin"
                                      "--abi"
                                      filename)
            compiled-contracts (string/split output #"=======")]
        (when (zero? exit-status)
          (doall
           (for [position (range 1 (count compiled-contracts) 2)]
             (let [[_ contract-name] (-> compiled-contracts
                                         (nth position)
                                         (string/trim)
                                         (string/split #":"))
                   [_ _ bin _ abi] (-> compiled-contracts
                                       (nth (inc position))
                                       (string/split #"\n"))]
               (when-not (empty? bin)
                 (fs/safe-create-dir! build-path)

                 (when truffle-artifacts?
                   (write-truffle-artifact (merge opts-map {:contract-name contract-name :abi abi :bin bin})))

                 (when bin?
                   (write-bin (merge opts-map {:contract-name contract-name :bin bin}))
                   (when byte-count
                     (count-bytes (merge opts-map {:contract-name contract-name}))))

                 (when abi?
                   (write-abi (merge opts-map {:contract-name contract-name :abi abi}))))))))))))

(defn solc
  "Lein plugin for compiling solidity contracts.
  Usage:
  `lein solc once` or `lein solc auto`"
  [project & [args]]
  (let [{:keys [src-path build-path contracts shim]
         :as opts} (merge
                    {:solc-err-only true :verbose false :abi? true :bin? true}
                    (:solc project))
        full-build-path (-> (.getCanonicalPath (io/file "."))
                            fs/ensure-slash
                            (str build-path))

        full-src-path (-> (.getCanonicalPath (io/file "."))
                          fs/ensure-slash
                          (str src-path))
        ;; TODO : delete on exit
        temp-src-path (when shim
                        (.getCanonicalPath (fs/create-temp-dir! src-path)))

        contracts-map (cond (sequential? contracts)
                            (reduce (fn [m c]
                                      (assoc m (str (fs/ensure-slash src-path) c) c))
                                    {}
                                    contracts)

                            (= :all contracts)
                            (reduce (fn [m f]
                                      (let [path (.getPath f)]
                                        (assoc m path (-> path
                                                          (string/split #"/")
                                                          last))))
                                    {}
                                    (fs/walk-dir src-path #".*\.sol" false))

                            :else (lein/abort "Unknown `:contracts` option found in project.clj"))
        opts-map (merge opts {:build-path full-build-path
                              :src-path full-src-path
                              :temp-src-path temp-src-path})
        once (fn [] (cond (sequential? contracts)
                          (doseq [c contracts]
                            (compile-contract (merge opts-map
                                                     {:filename c})))

                          (= :all contracts)
                          (doseq [[path c] contracts-map]
                            (compile-contract (merge opts-map
                                                     {:filename (-> path
                                                                    (string/split (re-pattern (fs/ensure-slash src-path)))
                                                                    last)})))))
        auto (fn [] (let [watcher (start-watcher src-path)]
                      (while true
                        (let [filename (<!! watcher)]
                          (if (contains? (-> contracts-map keys set) filename)
                            (do (lein/info (format "%s has changed" filename))
                                (compile-contract (merge opts-map
                                                         {:filename (get contracts-map filename)})))
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
