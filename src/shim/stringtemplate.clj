(ns shim.stringtemplate
  (:require [shim.matches :refer [wildcard]])
  (:import [org.stringtemplate.v4 ST STGroup STGroupFile]))

;; if (y == false) {
;;      if (z == true) {
;;              return 1;
;;      }
;; }

;; if (x == false) {
;;      if (y == true) {
;;              return 2;
;;      }
;; }

;; if (z == false) {
;;      return 3;
;; }
;; return 4;

;; TODO: indentation
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

(comment
  (def matches {:columns ["x" "y" "z"],
                :patterns ['("_" "false" "true") '("false" "true" "_") '("_" "_" "false") '("_" "_" "true")]
                :returns ["Match.One" "Match.Two" "Match.Three" "Match.Four"]})


  (generate-solidity matches))
