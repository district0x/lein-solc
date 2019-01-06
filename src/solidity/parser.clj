(ns solidity.parser
  (:require
   [clj-antlr.core :as antlr]
   [clojure.java.io :as io]
))

;; parser

(def parse-solidity (-> (io/resource "grammar/Solidity.g4")
                        slurp
                        (antlr/parser)))

(defn parse [code]
  (let [[_ & statements] (parse-solidity code)]
    statements))
