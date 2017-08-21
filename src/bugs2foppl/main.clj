(ns bugs2foppl.main
  (require [clj-antlr.coerce :as coerce]
           [clj-antlr.interpreted :as interpreted]
           :reload-all)
  (use [clojure.pprint :only [pprint]]
       [bugs2foppl.utils]
       [clojure.inspector :only (inspect-tree)]
       [loom.graph]
       [loom.io :only (view)]
       [loom.alg :only (topsort)]
       [clojure.walk]
       :reload-all))



(defn parse
  [grammar-file input-file]
  (let [input-string (slurp input-file)
        grammar-str (slurp grammar-file)
        grammar (interpreted/grammar grammar-str)
        m (interpreted/parse grammar {} input-string)
        e (coerce/tree->sexpr m)]
    e))

(parse "grammars/bugs.g4" "examples/PLA2_example3")

(parse "grammars/R_data.g4" "examples/examples_JAGS/classic-bugs/vol1/blocker/blocker-data.R")

(parse "grammars/R_data2.g4" "examples/examples_JAGS/classic-bugs/vol1/blocker/bench-test2.R")

; (inspect-tree (parse "grammars/R_data2.g4" "examples/examples_JAGS/classic-bugs/vol1/blocker/bench-test2.R"))

(defn node? [n] (seq? n))

(defn walk-ast
  "Uses walker to apply rule-node-visit only to rule nodes of the tree, rather than token nodes."
  [walker rule-node-visit tree]
  (let [f (fn [n] (if (node? n)
                    (rule-node-visit n)
                    n))]
    (let [new-tree (do (walker f tree))]
      new-tree)))

(println (slurp "examples/PLA2_example3"))

(defn foppl-distr-for [distr]
  (case distr
    "dnorm"   "normal"
    "dgamma"  "gamma"))
(defn foppl-func-for [func]
  (case func
    "sqrt"   "sqrt"))


; TODO maybe think about this derive issue but not now
; (def stochasticRelation ::stochasticRelation)
; (derive :stochasticRelation ::relation)
; (derive ::deterministicRelation :relation)
;
; (namespace)
;
; (parents :deterministicRelation)

; TODO it would be great if there would be a way to change the nth command to sth more meaningful depending ont he type of node we're visiting at the moment
(defmulti translate-node-visit (fn [node] (first node)))
(defmethod translate-node-visit :default [node] (second node))
(defmethod translate-node-visit :stochasticRelation [node]
  (list (nth node 1) "(sample" (nth node 3) ")"))
(defmethod translate-node-visit :deterministicRelation [node]
  (list (nth node 1) (nth node 3)))
(defmethod translate-node-visit :expressionList2 [node]
  (list (nth node 1) (nth node 3)))
(defmethod translate-node-visit :distribution [node]
  (list "(" (foppl-distr-for (nth node 1)) (nth node 3) ")"))
(defmethod translate-node-visit :relationList1 [node]
  (list "(let [" (nth node 1) "])"))
(defmethod translate-node-visit :relationList2 [node]
  (list "(let [" (nth node 1) "]" (nth node 2) ")"))
(defmethod translate-node-visit :function [node]
  (list "(" (foppl-func-for (nth node 1)) (nth node 3) ")"))
(defmethod translate-node-visit :exponentiation [node]
  (list "(math/expt" (nth node 1) (nth node 3) ")"))
(defmethod translate-node-visit :arithmetic [node]
  (list "(" (nth node 2) (nth node 1) (nth node 3) ")"))
(defmethod translate-node-visit :modelStatement [node]
  (list "(foppl-query" (nth node 3) ")"))
(defmethod translate-node-visit :parenExpression [node]
  (list (nth node 2)))


(defn visit-children
  "Returns a sequence of visited children."
  [visit-func node & context]
  (if (empty? context)
    (map visit-func (filter node? (rest node)))
    (map visit-func (filter node? (rest node)) (repeat (first context)))))

; sub it with edges
(defmulti v2 (fn [node context] (first node)))
(defmethod v2 :default [node context]
  (apply concat (visit-children v2 node context)))
(defmethod v2 :stochasticRelation [node context]
  (let [to-var (second (second node)) ; var name string
        context (assoc context :to-var to-var)]
    (v2 (nth node 3) context)))
(defmethod v2 :deterministicRelation [node context]
  (let [to-var (second (second node)) ; var name string
        context (assoc context :to-var to-var)]
    (v2 (nth node 3) context)))
(defmethod v2 :var [node context]
  (list (list (second node) (:to-var context))))


(defmulti v4 (fn [node] (first node)))
(defmethod v4 :default [node]
  (apply merge (visit-children v4 node)))
(defmethod v4 :stochasticRelation [node]
  (let [to-var (second (second node))] ; var name string
    {to-var node}))
(defmethod v4 :deterministicRelation [node]
  (let [to-var (second (second node))] ; var name string
    {to-var node}))

(let [tree (parse "grammars/bugs.g4" "examples/PLA2_example3")
      es (v2 tree {})
      g (apply digraph es)
      nso (topsort g)
      v2n (v4 tree)
      n2t (fn [n] (clojure.string/join " " (flatten (walk-ast postwalk translate-node-visit n))))
      nodes (fn [v2n nso] (map (partial get v2n) nso))]
  ; (n2t tree))
  (map n2t (nodes v2n nso)))

(let [tree (parse "grammars/bugs.g4" "examples/PLA2_example3")
      output (walk-ast postwalk translate-node-visit tree)]
  (println output))

(println (slurp "examples/PLA2_example3"))
