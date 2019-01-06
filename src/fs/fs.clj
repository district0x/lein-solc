(ns fs.fs
  (:require [clojure.java.io :as io]
            [me.raynes.fs :as fs]
            [clojure.string :as string])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn walk-dir
  "Returns list of files in a directory that match a pattern.
  Optionally includes subdirectories"
  [path pattern subdirs?]
  (doall (filter #(re-matches pattern (.getName %))
                 (if subdirs?
                   (file-seq (io/file path))
                   (.listFiles (io/file path))))))

(defn ensure-slash [path]
  (if (string/ends-with? path "/")
    path
    (str path "/")))

(defn safe-create-dir! [path]
  (let [file (io/file (ensure-slash path))]
    (when-not (.isDirectory file)
      (.mkdirs file))))

(defn create-temp-dir!
  "Creates a tmp dir, copies all source files recursively.
  Returns tmp dir File object."
  [path]
  (let [temp-dir (.toFile (Files/createTempDirectory "lein-solc" (into-array FileAttribute [])))]
    (fs/copy-dir-into path temp-dir)
    temp-dir))
