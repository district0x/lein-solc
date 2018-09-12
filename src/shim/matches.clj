(ns shim.matches
  (:require
   [clojure.walk :as walk]
   [clojure.zip :as zip]
   [clojure.core.match :refer [match]]
   [clojure.spec.alpha :as s]
   [clojure.java.io :as io]
   [clj-antlr.core :as antlr]
   ))

(def wildcard "_")

(def ast `(:expression
           (:expression (:primaryExpression (:identifier "matches")))
           "("
           (:functionCallArguments
            (:expressionList

             (:expression
              (:primaryExpression

               (:tupleExpression
                "["
                (:expression (:primaryExpression (:identifier "x")))
                ","
                (:expression (:primaryExpression (:identifier "y")))
                ","
                (:expression (:primaryExpression (:identifier "z")))
                "]")))

             ","

             (:expression
              (:primaryExpression

               (:tupleExpression
                "["
                (:expression (:primaryExpression (:identifier "_")))
                ","
                (:expression (:primaryExpression "false"))
                ","
                (:expression (:primaryExpression "true"))
                "]")

               ))

             ","

             (:expression
              (:expression (:primaryExpression (:identifier "Match")))
              "."
              (:identifier "One"))

             ","

             (:expression
              (:primaryExpression

               (:tupleExpression
                "["
                (:expression (:primaryExpression "false"))
                ","
                (:expression (:primaryExpression "true"))
                ","
                (:expression (:primaryExpression (:identifier "_")))
                "]")

               ))

             ","

             (:expression
              (:expression (:primaryExpression (:identifier "Match")))
              "."
              (:identifier "Two"))))
           ")")
  )

#_(defn expression-conformer [expr]
    (let [expr (-> expr second second)]
      (when-not (-> expr second (= wildcard))
        (str "if(_var_ == " expr ")"))))

#_(s/def ::expression (s/conformer expression-conformer))

#_(defn ast-edit [zipper matcher editor]
    (loop [loc zipper]
      (if (zip/end? loc)
        (zip/root loc)
        (if-let [matcher-result (matcher (zip/node loc))]
          (recur (zip/next (zip/edit loc (partial editor matcher-result))))
          (recur (zip/next loc))))))

#_(defn matches? [node]
    (= "matches" (:identifier node)) #_true)

#_(defn ast-editor [matcher-result node]
    (prn "matcher-result:" matcher-result  "node" node)
    node)

#_(ast-edit (zip/seq-zip ast)
            matches?
            ast-editor)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;---TODO: with zippers---;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;(clojure.pprint/pprint ast)

;; Match m =
;;          matches([x, y, z],
;;                  [_, false, true], Match.One,
;;                  [false, true, _], Match.Two
;;                  [_, _, false], Match.Three,
;;                  [_, _, true], Match.Four);

;; if (y == false && z == true) {
;;      return 1;
;; } else if (x == false && y == true) {
;;      return 2;
;; } else if (z == false) {
;;      return 3;
;; } else {
;;      return 4;
;; }

;;;;;;;;;;;;;;
;;---spec---;;
;;;;;;;;;;;;;;

(s/def ::expression #{:expression})
(s/def ::primary-expression #{:primaryExpression})
(s/def ::identifier #{:identifier})
(s/def ::tuple-expression #{:tupleExpression})
(s/def ::square-bracket-left #{"["})
(s/def ::square-bracket-right #{"]"})
(s/def ::comma #{","})
;; (s/def ::wildcard #{"_"})

(s/def ::column
  (s/spec (s/cat :val ::expression
                 :next (s/spec (s/cat :val ::primary-expression
                                      :next (s/spec (s/cat :val ::identifier
                                                           :head (s/and string? #(not (= wildcard %))))))))))

;; (s/def ::pattern
;;   (s/spec (s/cat :val ::expression
;;                  :next (s/spec (s/cat :val ::primary-expression
;;                                       :next (s/spec (s/cat :val ::identifier
;;                                                            :head (s/and string? #(not (= wildcard %))))))))))

(s/valid? ::pattern `(:expression (:primaryExpression (:identifier "_"))))

(s/valid? ::pattern `(:expression (:primaryExpression "false")))

(s/def ::column-tuple (s/conformer (fn [expr]
                                     (cond
                                       (not (seq? expr))
                                       ::s/invalid

                                       (->> expr first (contains? #{:tupleExpression}))
                                       (let [columns (map #(-> % second second second)
                                                          (filter #(s/valid? ::column %) expr))]
                                         (if (empty? columns)
                                           ::s/invalid
                                           columns))

                                       :else ::s/invalid))))

(def column-tuple '(:tupleExpression
                    "["
                    (:expression (:primaryExpression (:identifier "x")))
                    ","
                    (:expression (:primaryExpression (:identifier "y")))
                    ","
                    (:expression (:primaryExpression (:identifier "z")))
                    "]"))

(def pattern-tuple '(:tupleExpression
                     "["
                     (:expression (:primaryExpression (:identifier "_")))
                     ","
                     (:expression (:primaryExpression "false"))
                     ","
                     (:expression (:primaryExpression "true"))
                     "]"))

(s/valid? ::column-tuple pattern-tuple)

(s/conform ::column-tuple pattern-tuple)

(s/valid? ::column-tuple '(:expression (:primaryExpression (:identifier "x"))) #_column-tuple)

(s/valid? ::column-tuple column-tuple)

(s/conform ::column-tuple column-tuple)

(s/explain ::square-bracket-left "[")

(s/valid? ::column '(:expression (:primaryExpression (:identifier "y"))))

(s/conform ::column '(:expression (:primaryExpression (:identifier "y"))))

;;;;;;;;;;;;;;;;;
;;---matcher---;;
;;;;;;;;;;;;;;;;;

(defn dispatch-visitor [{:keys [:node :state :path]}]
  (cond
    (s/valid? ::column-tuple node) (do (prn node " IS VALID") :column)
    :else :default))

(defmulti visitor dispatch-visitor)

;;;;;;;;;;;;;;;;;
;;---editors---;;
;;;;;;;;;;;;;;;;;

(defmethod visitor :default
  [{:keys [:node :state :path]}]
  ;; (prn "INFO" "node" node)
  {:node node :state state :path path})

(defmethod visitor :column
  [{:keys [:node :state :path]}]
  ;; (prn "INFO" "MATCH" node)
  (let [cols (s/conform ::column-tuple node)]
    (when-not (empty? cols)
      (swap! state update-in [:columns] conj cols)))
  {:node node :state state :path path})

;;;;;;;;;;;;;;;;;;;;;
;;---tree walker---;;
;;;;;;;;;;;;;;;;;;;;;

(defn visit-tree
  ([zipper visitor] (visit-tree zipper nil visitor))
  ([zipper initial-state visitor]
   (loop [loc zipper
          state initial-state]
     (let [current-node (zip/node loc)
           ret (visitor {:node current-node :state state :path (zip/path loc)})
           updated-state (:state ret)
           updated-node (:node ret)
           updated-zipper (if (= updated-node current-node)
                            loc
                            (zip/replace loc updated-node))]
       (if (zip/end? (zip/next updated-zipper))
         {:node (zip/root updated-zipper) :state updated-state}
         (recur (zip/next updated-zipper)
                updated-state))))))

;;;;;;;;;;;;;;
;;---test---;;
;;;;;;;;;;;;;;

(def state (atom {:columns []
                  :patterns []}))

(visit-tree (zip/seq-zip ast)
            state
            visitor)

;;;;;;;;;;;;;;;;
;;---parser---;;
;;;;;;;;;;;;;;;;

(def parse-solidity (-> (io/resource "grammar/Solidity.g4")
                        slurp
                        (antlr/parser)))

(defn parse [code]
  (let [[_ & statements] (parse-solidity code)]
    statements))
