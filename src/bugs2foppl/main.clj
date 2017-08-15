(ns bugs2foppl.main
  (require [clj-antlr.core :as antlr]))

(use '[clojure.pprint :only [pprint]])

; (load "bugs2foppl/utils")
(use '[bugs2foppl.utils])

(def bugs (antlr/parser "grammars/bugs.g4"))
(def R-data (antlr/parser "grammars/R_data.g4"))

(defn walk-ast
  "Uses clojure.walk/postwalk to apply rule-node-visit only to rule nodes of the tree, rather than token nodes."
  [rule-node-visit tree]
  (let [f (fn [n] (if (seq? n)
                    (let [] (rule-node-visit n))
                    n))]
    (let [new-tree (clojure.walk/postwalk f tree)]
      ; (println new-tree)
      new-tree)))

(let [tree (bugs (slurp "examples/PLA2_example3"))
      output (walk-ast visit-node tree)]
  ; (println tree)
  (println (clojure.string/join " " (flatten output))))

(println (slurp "examples/PLA2_example3"))
(bugs (slurp "examples/PLA2_example3"))

(defn foppl-distr-for [distr]
  (case distr
    "dnorm"   "normal"
    "dgamma"  "gamma"))
(defn foppl-func-for [func]
  (case func
    "sqrt"   "sqrt"))

(foppl-query (let [ mu (sample ( normal 0 1 ) ) ]) (let [ tau 1 ]) (let [ gam (sample ( gamma 0.1 0.1))]))
; TODO it would be great if there would be a way to change the nth command to sth more meaningful depending ont he type of node we're visiting at the moment
; TODO I cannot differentiate different 'on the right' subscenarios in antlr just using length of the node list because e.g. function ...
(defmulti visit-node (fn [node] (println node) (first node)))
(defmethod visit-node :default [node] (second node))
(defmethod visit-node :stochasticRelation [node]
  (list (nth node 1) "(sample" (nth node 3) ")"))
(defmethod visit-node :deterministicRelation [node]
  (list (nth node 1) (nth node 3)))
(defmethod visit-node :expressionList [node]
  (if (= (count node) 2)
    (second node)
    (list (nth node 1) (nth node 3))))
(defmethod visit-node :distribution [node]
  (list "(" (foppl-distr-for (nth node 1)) (nth node 3) ")"))
(defmethod visit-node :relationList [node]
  (if (= (count node) 2)
    (list "(let [" (nth node 1) "])")
    (list "(let [" (nth node 1) "]" (nth node 2) ")")))
(defmethod visit-node :function [node]
  (list "(" (foppl-func-for (nth node 1)) (nth node 3) ")"))
(defmethod visit-node :exponentiation [node]
  (list "(math/expt" (nth node 1) (nth node 3) ")"))
(defmethod visit-node :arithmetic [node]
  (list "(" (nth node 2) (nth node 1) (nth node 3) ")"))
(defmethod visit-node :modelStatement [node]
  (list "(foppl-query" (nth node 3) ")"))


(bugs (slurp "examples/PLA2_example1"))

(defn visit [n] (call (first n) "visit"))

(defn var-visit [n] (call (first n) "visit"))

(defn f [n]
  (println n)
  n)



(use '[clojure.inspector :only (inspect-tree)])

(inspect-tree (bugs (slurp "examples/PLA2_example1")))
