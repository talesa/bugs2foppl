(ns bugs2foppl.utils
  (require [clj-antlr.coerce :as coerce]
           [clj-antlr.interpreted :as interpreted]
           [clojure.math.combinatorics :as combo]
           [clojure.java.io :as io])
          ;  :reload-all)
  (use [bugs2foppl.utils]
       [clojure.pprint :only [pprint]]
       [clojure.inspector :only (inspect-tree)]
       [loom.graph]
       [loom.io :only (view)]
       [loom.alg :only (topsort)]
       [clojure.walk]
       [clojure.repl]
       :reload-all))

; pass 1

; helper functions

(defn node-type [n] (first n))

(defn sigmoid [x] (/ 1. (+ 1. (Math/exp (- x)))))
(defn inv-link-fn [link-fn]
  (case link-fn
    "logit" 'sigmoid))

(defn extract-seq-from-left-recursive-rule
  "Returns a sequence from right recursive rules with a comma in the form r: n | r ',' n;"
  ([node] (extract-seq-from-left-recursive-rule node :comma))
  ([node comma]
   (let [n (if (= comma :comma) 3 2)]
     (if (= n (count (rest node)))
         (conj (nth node 1) (nth node n))
         (vector (second node))))))

; dispatched function

(defmulti pass1 (fn [_ node] (first node)))
(defmethod pass1 :default [_ node] node)

(defmethod pass1 :input [_ [_ model]] model)

(defmethod pass1 :varID [_ [_ name]] (symbol name))
(defmethod pass1 :varIndexed
  [_ [_ name _ range-list]]
  (list 'varindexed (symbol name) range-list))
(defmethod pass1 :stochasticRelation
  [_ [_ var _ distribution & t-or-i]]
  (if (empty? t-or-i)
    (list 'stochastic-relation var distribution)
    (list 'stochastic-relation var distribution t-or-i)))
(defn interval-trunc-helper [symbol stuff]
  (let [stuff (case (count stuff)
                5 [(nth stuff 1) (nth stuff 3)]
                3 [:all :all]
                4 (if (= "," (nth stuff 1))
                      [:all (nth stuff 2)]
                      [(nth stuff 1) :all]))]
    (apply list (into [symbol] stuff))))
(defmethod pass1 :interval [_ [_ & stuff]] (interval-trunc-helper 'interval stuff))
(defmethod pass1 :truncated [_ [_ & stuff]] (interval-trunc-helper 'truncated stuff))

(defmethod pass1 :varStatement [_ [_ _ declaration-list]] (list 'var-statement declaration-list))


(defmethod pass1 :declarationList [_ node]
  (apply list (cons 'declaration-list (extract-seq-from-left-recursive-rule node))))

(defmethod pass1 :nodeDeclaration [_ [_ name & rst]]
  (if (not (empty? rst))
    (list 'node-declaration name (second rst))
    (list 'node-declaration name)))

(defmethod pass1 :dimensionsList [_ node]
  (apply list (cons 'dimensions-list (extract-seq-from-left-recursive-rule node))))

(defmethod pass1 :dataStatement [_ [_ _ _ relation-list]]
  (list 'data relation-list))

(defmethod pass1 :modelStatement [_ [_ _ _ relation-list]]
  (apply list (cons 'model relation-list)))

; for loop handling

(defmethod pass1 :rangeList [_ node]
  (extract-seq-from-left-recursive-rule node))

(defmethod pass1 :rangeExpression [_ [_ expr1 _ expr2]]
  (list 'range-inclusive expr1 expr2))

(defmethod pass1 :rangeElement [_ [_ & rst]]
  (if (empty? rst)
    :all
    (first rst)))

(defmethod pass1 :counter [_ [_ _ _ var _ range-element]]
  (if (= range-element :all)
    (throw (Exception. "Do not know how to handle :all in for loop counter."))
    (list (symbol var) range-element)))

(defmethod pass1 :forLoop [_ [_ [var range] relations]]
  (list 'for var range relations))

(defmethod pass1 :deterministicRelation1 [_ [_ var _ expr]]
  (list 'deterministic-relation var expr))

(defmethod pass1 :deterministicRelationLink [_ [_ link-fn _ var _ _ expr]]
  (list 'deterministicRelation var (list 'inv-link-fn (symbol link-fn) expr)))

(defmethod pass1 :expressionList [_ node]
  (extract-seq-from-left-recursive-rule node))

(defmethod pass1 :varExpression [_ [_ var]] var)
(defmethod pass1 :exponentiation [_ [_ expr1 _ expr2]]
  (list 'Math/pow expr1 expr2))
(defmethod pass1 :arithmetic [_ [_ expr1 operator expr2]]
  (list (symbol operator) expr1 expr2))
(defmethod pass1 :negation [_ [_ _ expr]] (list '- expr))
(defmethod pass1 :atom [_ [_ atom]] (read-string atom))
(defmethod pass1 :lenExpression [_ [_ _ _ var]] (list 'length var))
(defmethod pass1 :dimExpression [_ [_ _ _ var]] (list 'dim var))
(defmethod pass1 :function [_ [_ function _ expr-list]]
  (apply list (cons (symbol function) expr-list)))
(defmethod pass1 :specialExpression [_ [_ expr1 operator expr2]]
  (throw (Exception. "Do not know how to handle specialExpression.")))
(defmethod pass1 :parenExpression [_ [_ _ expr]] expr)

(defmethod pass1 :distribution [_ [_ distr & rst]]
  (if (= 3 (count rst))
    (apply list (cons 'distribution (cons (symbol distr) (nth rst 1))))
    ; (list (symbol distr) (nth rst 1))
    (list 'distribution (symbol distr))))

(defmethod pass1 :relations [_ [_ _ relation-list]] relation-list)

(defmethod pass1 :relationList [_ node]
  (extract-seq-from-left-recursive-rule node :no-comma))

(defmethod pass1 :relation [_ [_ relation]] relation)
