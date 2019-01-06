(ns fs.fs
  (:require [clojure.java.io :as io]
            [clojure.string :as string])
  (:import [java.nio.file Files]))

(defmacro ^:private predicate [s path]
  `(if ~path
     (. ~path ~s)
     false))

#_(defn ^File file
  "If `path` is a period, replaces it with cwd and creates a new File object
   out of it and `paths`. Or, if the resulting File object does not constitute
   an absolute path, makes it absolutely by creating a new File object out of
   the `paths` and cwd."
  [path & paths]
  (when-let [path (apply
                   io/file (if (= path ".")
                             *cwd*
                             path)
                   paths)]
    (if (.isAbsolute ^File path)
      path
      (io/file *cwd* path))))

(defn exists?
  "Return true if `path` exists."
  [path]
  (predicate exists (io/file path)))

(defn file?
  "Return true if `path` is a file."
  [path]
  (predicate isFile (io/file path)))

#_(defn copy-dir
  "Copy a directory from `from` to `to`. If `to` already exists, copy the directory
   to a directory with the same name as `from` within the `to` directory."
  [from to]
  (when (exists? from)
    (if (file? to)
      (throw (IllegalArgumentException. (str to " is a file")))
      (let [from (io/file from)
            to (if (exists? to)
                 (io/file to (base-name from))
                 (io/file to))
            trim-size (-> from str count inc)
            dest #(io/file to (subs (str %) trim-size))]
        (mkdirs to)
        (dorun
         (walk (fn [root dirs files]
                 (doseq [dir dirs]
                   (when-not (directory? dir)
                     (-> root (file dir) dest mkdirs)))
                 (doseq [f files]
                   (copy+ (file root f) (dest (file root f)))))
               from))
        to))))

(defn ensure-slash [path]
  (if (string/ends-with? path "/")
    path
    (str path "/")))

(defn walk-dir
  "returns list of files in a directory that match a pattern.
  Optionally includes subdirectories"
  [path pattern subdirs?]
  (doall (filter #(re-matches pattern (.getName %))
                 (if subdirs?
                   (file-seq (io/file path))
                   (.listFiles (io/file path))))))

(defn safe-create-dir! [path]
  (let [file (io/file (ensure-slash path))]
    (when-not (.isDirectory file)
      (.mkdirs file))))



;; TODO : create tmp dir, copy all source files recursively
(defn create-temp-dir!
  [path]
  (let [temp-dir (java.nio.file.Files/createTempDirectory "lein-solc" (into-array java.nio.file.attribute.FileAttribute []))]

    #_(file-seq (io/file path))

    )


  )
