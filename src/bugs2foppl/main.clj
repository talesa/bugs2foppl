(ns bugs2foppl.main
  (require [clj-antlr.coerce :as coerce]
           [clj-antlr.interpreted :as interpreted]
           :reload-all)
  (use [clojure.pprint :only [pprint]]
       [bugs2foppl.utils]
       [clojure.inspector :only (inspect-tree)]
       :reload-all))

(defn parse
  [grammar-file input-file]
  (let [input-string (slurp input-file)
        grammar-str (slurp grammar-file)
        grammar (interpreted/grammar grammar-str)
        m (interpreted/parse grammar {} input-string)
        e (coerce/tree->sexpr m)]
    e))

(parse "grammars/bugs.g4" "examples/PLA2_example1")

(parse "grammars/R_data.g4" "examples/examples_JAGS/classic-bugs/vol1/blocker/blocker-data.R")

(parse "grammars/R_data2.g4" "examples/examples_JAGS/classic-bugs/vol1/blocker/bench-test2.R")

(inspect-tree (parse "grammars/R_data2.g4" "examples/examples_JAGS/classic-bugs/vol1/blocker/bench-test2.R"))

(defn walk-ast
  "Uses clojure.walk/postwalk to apply rule-node-visit only to rule nodes of the tree, rather than token nodes."
  [rule-node-visit tree]
  (let [f (fn [n] (if (seq? n)
                    (let [] (rule-node-visit n))
                    n))]
    (let [new-tree (clojure.walk/postwalk f tree)]
      new-tree)))

(let [tree (parse "grammars/bugs.g4" "examples/PLA2_example3")
      output (walk-ast visit-node tree)]
  (println (clojure.string/join " " (flatten output))))

(println (slurp "examples/PLA2_example3"))

(defn foppl-distr-for [distr]
  (case distr
    "dnorm"   "normal"
    "dgamma"  "gamma"))
(defn foppl-func-for [func]
  (case func
    "sqrt"   "sqrt"))

; TODO it would be great if there would be a way to change the nth command to sth more meaningful depending ont he type of node we're visiting at the moment
(defmulti visit-node (fn [node]
                      ;  (println node)
                       (first node)))
(defmethod visit-node :default [node] (second node))
(defmethod visit-node :stochasticRelation [node]
  (list (nth node 1) "(sample" (nth node 3) ")"))
(defmethod visit-node :deterministicRelation [node]
  (list (nth node 1) (nth node 3)))
(defmethod visit-node :expressionList2 [node]
  (list (nth node 1) (nth node 3)))
(defmethod visit-node :distribution [node]
  (list "(" (foppl-distr-for (nth node 1)) (nth node 3) ")"))
(defmethod visit-node :relationList1 [node]
  (list "(let [" (nth node 1) "])"))
(defmethod visit-node :relationList2 [node]
  (list "(let [" (nth node 1) "]" (nth node 2) ")"))
(defmethod visit-node :function [node]
  (list "(" (foppl-func-for (nth node 1)) (nth node 3) ")"))
(defmethod visit-node :exponentiation [node]
  (list "(math/expt" (nth node 1) (nth node 3) ")"))
(defmethod visit-node :arithmetic [node]
  (list "(" (nth node 2) (nth node 1) (nth node 3) ")"))
(defmethod visit-node :modelStatement [node]
  (list "(foppl-query" (nth node 3) ")"))
