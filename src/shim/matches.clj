(ns shim.matches
  (:require
   ;; [clj-antlr.core :as antlr]
   [clojure.core.match :refer [match]]
   ;; [clojure.java.io :as io]
   [clojure.spec.alpha :as s]
   [clojure.string :as string]
   [clojure.walk :as walk]
   [clojure.zip :as zip]
   [solidity.parser :as solidity]
   )
  (:import [org.stringtemplate.v4 ST STGroup STGroupFile]))

;; spec
(def wildcard "_")
(def matches "matches")

(s/def ::expression #{:expression})
(s/def ::primary-expression #{:primaryExpression})
(s/def ::identifier #{:identifier})
(s/def ::tuple-expression #{:tupleExpression})
(s/def ::wildcard (s/cat :val ::identifier
                         :head #{wildcard}))
(s/def ::boolean #{"false" "true"})

(s/def ::column
  (s/spec (s/cat :val ::expression
                 :next (s/spec (s/cat :val ::primary-expression
                                      :root (s/spec (s/cat :val ::identifier
                                                           :head #(and (string? %)
                                                                       (not (= % wildcard))))))))))

(s/def ::pattern
  (s/spec (s/cat :val ::expression
                 :next (s/spec (s/cat :val ::primary-expression
                                      :root (s/or :boolean ::boolean
                                                  :wildcard ::wildcard))))))

(s/def ::return (s/spec (s/cat :expression ::expression
                               :primary-expression ::column
                               :dot #{"."}
                               :identifier (s/spec (s/cat :val ::identifier
                                                          :head #(string? %))))))

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

(s/def ::pattern-tuple (s/conformer (fn [exprs]
                                      (cond
                                        (not (seq? exprs))
                                        ::s/invalid

                                        (->> exprs first (contains? #{:tupleExpression}))
                                        (let [patterns (map #(let [head (-> % second second)]
                                                               (cond
                                                                 (s/valid? ::boolean head)
                                                                 head

                                                                 (s/valid? ::wildcard head)
                                                                 (second head)

                                                                 :else %))
                                                            (filter #(s/valid? ::pattern %) exprs))]
                                          (if (empty? patterns)
                                            ::s/invalid
                                            patterns))

                                        :else ::s/invalid))))

;; matcher

(defn dispatch-visitor [{:keys [:node :state :path]}]
  (cond
    (s/valid? ::column-tuple node) :column
    (s/valid? ::pattern-tuple node) :pattern
    (s/valid? ::return node) :return
    :else :default))

(defmulti visitor dispatch-visitor)

;; editors

(defmethod visitor :default
  [{:keys [:node :state :path]}]
  {:node node :state state :path path})

(defmethod visitor :column
  [{:keys [:node :state :path]}]
  (let [cols (s/conform ::column-tuple node)]
    (when-not (empty? cols)
      (swap! state assoc :columns cols)))
  {:node node :state state :path path})

(defmethod visitor :pattern
  [{:keys [:node :state :path]}]
  (let [pattern (s/conform ::pattern-tuple node)]
    (when-not (empty? pattern)
      (swap! state update-in [:patterns] conj pattern)))
  {:node node :state state :path path})

(defmethod visitor :return
  [{:keys [:node :state :path]}]
  (let [pattern (s/conform ::return node)]
    (swap! state update-in [:returns] conj
           (str (-> pattern :primary-expression :next :root :head)
                (:dot pattern)
                (-> pattern :identifier :head)))
    {:node node :state state :path path}))

;; syntax tree walker

(defn- visit-tree*
  ([zipper visitor]
   (visit-tree* zipper nil visitor))
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

(defn visit-tree
  "Given syntax-tree and initial state
  returns updated state"
  [ast state]
  (visit-tree* (zip/seq-zip ast)
               state
               visitor)
  state)

;; test

(comment
  (def state (atom {:columns []
                    :patterns []
                    :returns []}))

  (visit-tree ast
              state)

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
                       (:expression (:primaryExpression "true"))
                       ","
                       (:expression (:primaryExpression (:identifier "_")))
                       ","
                       (:expression (:primaryExpression "false"))
                       ","
                       (:expression (:primaryExpression "true"))
                       "]"))

  (def return '(:expression
                (:expression (:primaryExpression (:identifier "Match")))
                "."
                (:identifier "One")))

  (s/valid? ::return return)

  (s/explain ::return return)

  (s/valid? ::pattern `(:expression (:primaryExpression (:identifier "_"))))

  (s/valid? ::column '(:expression (:primaryExpression (:identifier "y"))))

  (s/valid? ::column `(:expression (:primaryExpression (:identifier "_"))))

  (s/conform ::column '(:expression (:primaryExpression (:identifier "y"))))

  (s/conform ::pattern `(:expression (:primaryExpression (:identifier "_"))))

  (s/conform ::pattern `(:expression (:primaryExpression "false")))

  (s/valid? ::pattern-tuple pattern-tuple)

  (s/conform ::pattern-tuple pattern-tuple)

  (s/valid? ::pattern `(:expression (:primaryExpression "false")))

  (s/valid? ::column-tuple pattern-tuple)

  (s/valid? ::column-tuple '(:expression (:primaryExpression (:identifier "x"))))

  (s/valid? ::column-tuple column-tuple)

  (s/conform ::column-tuple column-tuple)
  )

;; generator

(defn generate-solidity
  [{:keys [:columns :patterns :returns]}]
  (let [keep-indices (fn [coll]
                       (keep-indexed #(if-not (= wildcard %2) %1)
                                     coll))
        filter-by-index (fn [coll idx]
                          (map (partial nth coll) idx))
        generate-match (fn [cols patt ret]
                         (-> (STGroupFile. "templates/solidity.stg")
                             (.getInstanceOf "match")
                             (.add "columns" cols)
                             (.add "pattern" patt)
                             (.add "return" ret)
                             (.render)))
        generate-return (fn [ret]
                          (-> (STGroupFile. "templates/solidity.stg")
                              (.getInstanceOf "return")
                              (.add "return" ret)
                              (.render)))]
    (loop [[head & tail] patterns
           res ""
           idx 0]
      (if tail
        (let [indices (keep-indices head)]
          (recur tail
                 (str res (generate-match (filter-by-index columns indices)
                                          (filter-by-index head indices)
                                          (nth returns idx)))
                 (inc idx)))
        ;; return last
        (str res (generate-return (nth returns idx)))))))

;; test

(comment
  (def matches {:columns ["x" "y" "z"],
                :patterns ['("_" "false" "true") '("false" "true" "_") '("_" "_" "false") '("_" "_" "true")]
                :returns ["Match.One" "Match.Two" "Match.Three" "Match.Four"]})

  (generate-solidity matches))

;; shim

(defn get-matches-code
  [code]
  (first (re-find #"(?is)(matches\()(.*)(\);)" code)))

(defn shim
  "Given a vanilla source code passes it through shim and
  returns a string with the shimmed source code.
  Idempotent : vanilla solidity code is returned as-is."
  [code]
  (if-let [replacement (get-matches-code code)]
    (let [ast (solidity/parse code)
          initial-state (atom {:columns []
                               :patterns []
                               :returns []})
          updated-state (visit-tree ast initial-state)
          shim (generate-solidity @updated-state)
          replacement (get-matches-code code)]
      (string/replace code replacement shim))
    code))
