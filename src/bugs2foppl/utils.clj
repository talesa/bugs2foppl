(ns bugs2foppl.utils
  (require [clj-antlr.coerce :as coerce]
           [clj-antlr.interpreted :as interpreted]
           :reload-all)
  (use [clojure.pprint :only [pprint]]
       [clojure.inspector :only (inspect-tree)]
       [loom.graph]
       [loom.io :only (view)]
       [loom.alg :only (topsort)]
       [clojure.walk]
       [foppl.core]
       [anglican core runtime emit stat]
       :reload-all))


(defn v2m [a dim]
  (if (empty? dim)
    a
    (let [l (/ (count a) (first dim))]
      (loop [a a
             b []]
        (if (empty? a)
          b
          (recur (nthrest a l)
                 (conj b (v2m (take l a) (rest dim)))))))))


(defn parse
  [grammar-file input-file]
  (let [input-string (slurp input-file)
        grammar-str (slurp grammar-file)
        grammar (interpreted/grammar grammar-str)
        m (interpreted/parse grammar {} input-string)
        e (coerce/tree->sexpr m)]
    e))


(defn node? [n] (and (seq? n) (keyword? (first n))))


(defn walk-ast
  "Uses walker to apply rule-node-visit only to rule nodes of the tree, rather than token nodes."
  [walker rule-node-visit tree]
  (let [f (fn [n] (if (node? n)
                    (rule-node-visit n)
                    n))]
    (let [new-tree (do (walker f tree))]
      new-tree)))


(defn foppl-distr-for [distr]
  (case distr
    "dnorm"   'normal
    "dgamma"  'gamma
    "dunif"    'uniform-continuous))

(defn foppl-func-for [func]
  (case func
    "sqrt"   "sqrt"))


(defn visit-children
  "Returns a sequence of visited children."
  [visit-func node & context]
  (if (empty? context)
    (map visit-func (filter node? (rest node)))
    (map visit-func (filter node? (rest node)) (repeat (first context)))))


; sub it with edges
(defmulti get-graph-edges (fn [node context] (first node)))
(defmethod get-graph-edges :default [node context]
  (apply concat (visit-children get-graph-edges node context)))

(defmethod get-graph-edges :stochasticRelation [node context]
  (let [to-var (second (second node)) ; var name string
        context (assoc context :to-var to-var)]
    (get-graph-edges (nth node 3) context)))
(defmethod get-graph-edges :deterministicRelation [node context]
  (let [to-var (second (second node)) ; var name string
        context (assoc context :to-var to-var)]
    (get-graph-edges (nth node 3) context)))
(defmethod get-graph-edges :varID [node context]
  (list (list (second node) (:to-var context))))


(defmulti build-relation-node-map (fn [node] (first node)))
(defmethod build-relation-node-map :default [node]

  (apply merge (visit-children build-relation-node-map node)))
(defmethod build-relation-node-map :stochasticRelation [node]
  (let [to-var (second (second node))] ; var name string
    {to-var node}))
(defmethod build-relation-node-map :deterministicRelation [node]
  (let [to-var (second (second node))] ; var name string
    {to-var node}))


; TODO macro to make those definitions shorter

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

(defn find-nodes [type node]
  (if (= (first node) type)
    (list node)
    (apply concat
           (visit-children (partial find-first-node type) node))))

(defn find-first [type node] (first (find-nodes type node)))

; UNROLLING FORLOOPS
(def vars {
           "r" '(10, 23, 23, 26, 17, 5, 53, 55, 32, 46, 10, 8, 10, 8, 23, 0, 3, 22, 15, 32, 3)
           "n" '(39, 62, 81, 51, 39, 6, 74, 72, 51, 79, 13, 16, 30, 28, 45, 4, 12, 41, 30, 51, 7)
           "x1" '(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
           "x2" '(0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1)
           "N" 21})

(let [] vars)

(defn var-or-int
  "val is a string. If val is a number returns that number, if val is a key in coll, return the value of that key."
  [val coll]
  {:pre [(string? val)]} ; val should always be a string
  (if (contains? coll val)
    (get coll val)
    (let [n (read-string val)]
       (if (number? n)
         n
         (throw (Exception. "max value of a for loop counter is neither a declared variable nor a number"))))))


;lower bound always 1
;upper bound always variable in data
(defmethod translate-node-visit :rangeExpression [node]
  {:min (nth node 1)
   :max (var-or-int (nth node 3) vars)})

(defmethod translate-node-visit :counter [node]
  (merge {:var (nth node 3)} (nth node 5)))

(defmulti forloop-change-names (fn [node] (first node)))
(defmethod forloop-change-names :varIndexed [node]
  (if (= :varID (nth node 3)) node))

(defmethod translate-node-visit :forLoop [node]
  (let [params (nth node 1)
        relations (nth node 2)]
    (list params relations)))

(defmethod translate-node-visit :relations [node]
  (nth node 2))

(parse "grammars/bugs.g4" "examples/PLA2_example4")


(let [tree (parse "grammars/bugs.g4" "examples/PLA2_example4")
      forloop (first (find-nodes :forLoop tree))]
  (walk-ast postwalk translate-node-visit forloop))

(let [tree (parse "grammars/bugs.g4" "examples/PLA2_example4")
      forloop (first (find-nodes :forLoop tree))]
  forloop)

; first I will have function which only hits forloop and then unrolls it


; e.g.
; (let [vars-ranges [x (range 2)
;                    y (range 2)])
; each nested loop will add the appropriate range to vars-ranges
; for now a single variable
(defmethod fl :forloop [node context]
  (let [output-relations (list)
        counter (nth node 1)
        context (update-vars-ranges-context context counter)
        ; TODO this will fail with nested for loops because it will also traverse into the next :forloop, I need a way to terminate the descent
        relations (find-nodes :relation (nth node 2))]
    (map unroll-loop-relation relations (repeat context))))

(defn update-vars-ranges-context [context counter]
  (let [vars-ranges (:vars-ranges context)
        vars-ranges (merge vars-ranges (vars-ranges-from-counter counter))
        context (assoc context
                       :vars-ranges vars-ranges)]
    context))

(defn unroll-loop-relation [relation context]
  (let [relation (second node)
        vars-ranges (:vars-ranges context)
        ; substitute func must know what variables its supposed to substitute
        vars (take-nth 2 vars-ranges)]
    ; TODO probably there will be a need for some quoting magic here
    (for vars-ranges
         ; I am traversing this node to substitute for each of the iterations of the unrolling - this is inefficient, surely there is a better way
         (substitute-varid-for-varindexed relation vars))))

(defmulti substitute-varid-for-varindexed [node] (first node))
(defmethod substitute-varid-for-varindexed :default [node] node
  (let [f (fn [node])]))



(defn text [node] (walk-ast postwalk (fn [node] (str (rest node)) node)))

(defn vars-ranges-from-counter [counter-node]
  (let [var-name-str  (nth counter-node 3)
        var-name-sym  (symbol var-name-str)
        range-expr    (find-first :rangeExpression counter-node)
        ; TODO these two below may need to be evaluated or read from the var list etc, for now the assumption is these are numbers in string format
        var-min       (number (text (nth range-expr 1)))
        var-max       (number (text (nth range-expr 3)))
        var-range     (range var-min (+ 1 var-max))]
    (vector var-name-sym var-range)))


; (defmethod fl :relation [node context]
;   (let [relation (second node)
;         varIndexed (second relation)
;         vars-ranges (:vars-ranges context)
;         vars  (take-nth 2 vars-ranges)
;         index (second (first (find-nodes :varID varIndexed)))
;         relation-type (first relation)
;         output-relations (list)]
;     (if (contains? vars index)
;       (for vars-ranges
;            (list :relation
;                  (list relation-type
;                        (merge
;                         (list :varID (unsugar-var (second varIndexed)))
;                         (nthrest relation 2)))))
;       node)))

(defn unsugar-var [var index] (str var "_" index))

(let [output-relations (list)])


; probably need to put recursive generation of :relationList2 here
; it can be a seperate function
; or I don't because it is just a list of relationships
