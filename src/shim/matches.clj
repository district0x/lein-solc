(ns shim.matches
  (:require
   ;; [clj-antlr.common :as ac]
   ;; [clj-antlr.interpreted :as i]
   ;; [clj-antlr.proto :as proto]
   ;; [clj-antlr.static :as as]

    [clojure.java.io :as io]
   [clj-antlr.core :as antlr]
   ))

;; (def lexer (i/lexer-interpreter (i/grammar "/home/jmonetta/tmp/LessLexer.g4")))

;; (def parser (i/parser-interpreter (i/grammar "/home/jmonetta/tmp/LessParser.g4") lexer))

(def parse-solidity (-> (io/resource "grammar/Solidity.g4")
                        slurp
                        (antlr/parser)))

(defn parse [code]
  (let [[_ & statements] (parse-solidity code)]
    statements

    ))
