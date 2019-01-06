(ns shim.conditionals
  (:require
   [solidity.parser :as solidity]
   ))


(defn shim [code]

  (prn (solidity/parse code))

  code)
