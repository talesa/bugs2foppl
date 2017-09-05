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

; PASS 1
; sanitizes var names
; changes the structure of the code from parser artifacts to LISP notation

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

(defmulti pass1 first)
(defmethod pass1 :default [node] node)

(defmethod pass1 :input [[_ model]] model)

(defmethod pass1 :varID [[_ name]] (symbol (sanitize-var-name name)))
(defmethod pass1 :varIndexed
  [[_ name _ range-list]]
  (list 'varindexed (symbol (sanitize-var-name name)) range-list))
(defmethod pass1 :stochasticRelation
  [[_ var _ distribution & t-or-i]]
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
(defmethod pass1 :interval [[_ & stuff]] (interval-trunc-helper 'interval stuff))
(defmethod pass1 :truncated [[_ & stuff]] (interval-trunc-helper 'truncated stuff))

(defmethod pass1 :varStatement [[_ _ declaration-list]] (list 'var-statement declaration-list))


(defmethod pass1 :declarationList [node]
  (apply list (cons 'declaration-list (extract-seq-from-left-recursive-rule node))))

(defmethod pass1 :nodeDeclaration [[_ name & rst]]
  (if (not (empty? rst))
    (list 'node-declaration name (second rst))
    (list 'node-declaration name)))

(defmethod pass1 :dimensionsList [node]
  (apply list (cons 'dimensions-list (extract-seq-from-left-recursive-rule node))))

(defmethod pass1 :dataStatement [[_ _ _ relation-list]]
  (list 'data relation-list))

(defmethod pass1 :modelStatement [[_ _ _ relation-list]]
  (apply list (cons 'model relation-list)))

; for loop handling

(defmethod pass1 :rangeList [node]
  (extract-seq-from-left-recursive-rule node))

(defmethod pass1 :rangeExpression [[_ expr1 _ expr2]]
  (list 'range-inclusive expr1 expr2))

(defmethod pass1 :rangeElement [[_ & rst]]
  (if (empty? rst)
    :all
    (first rst)))

(defmethod pass1 :counter [[_ _ _ var _ range-element]]
  (if (= range-element :all)
    (throw (Exception. "Do not know how to handle :all in for loop counter."))
    (list (symbol var) range-element)))

(defmethod pass1 :forLoop [[_ [var range] relations]]
  (list 'for var range relations))

(defmethod pass1 :deterministicRelation1 [[_ var _ expr]]
  (list 'deterministic-relation var expr))

(defmethod pass1 :deterministicRelationLink [[_ link-fn _ var _ _ expr]]
  (list 'deterministicRelation var (list 'inv-link-fn (symbol link-fn) expr)))

(defmethod pass1 :expressionList [node]
  (extract-seq-from-left-recursive-rule node))

(defmethod pass1 :varExpression [[_ var]] var)
(defmethod pass1 :exponentiation [[_ expr1 _ expr2]]
  (list 'Math/pow expr1 expr2))
(defmethod pass1 :arithmetic [[_ expr1 operator expr2]]
  (list (symbol operator) expr1 expr2))
(defmethod pass1 :negation [[_ _ expr]] (list '- expr))
(defmethod pass1 :atom [[_ atom]] (read-string atom))
(defmethod pass1 :lenExpression [[_ _ _ var]] (list 'length var))
(defmethod pass1 :dimExpression [[_ _ _ var]] (list 'dim var))
(defmethod pass1 :function [[_ function _ expr-list]]
  (apply list (cons 'function (cons (symbol function) expr-list))))
(defmethod pass1 :specialExpression [[_ expr1 operator expr2]]
  (throw (Exception. "Do not know how to handle specialExpression.")))
(defmethod pass1 :parenExpression [[_ _ expr]] expr)

(defmethod pass1 :distribution [[_ distr & rst]]
  (if (= 3 (count rst))
    (apply list (cons 'distribution (cons (symbol distr) (nth rst 1))))
    ; (list (symbol distr) (nth rst 1))
    (list 'distribution (symbol distr))))

(defmethod pass1 :relations [[_ _ relation-list]] relation-list)

(defmethod pass1 :relationList [node]
  (extract-seq-from-left-recursive-rule node :no-comma))

(defmethod pass1 :relation [[_ relation]] relation)



; PASS 2
; substitutes the names of the functions, distributions and inverse link functions; changes the order and the parameterization of the arguments of the functions where necessary

(defmulti pass2 first)
(defmethod pass2 :default [n] (let [] (pprint n) n))

; TODO some distributions and functions may need to have the order of arguments or parameterization of arguments
(defmethod pass2 'distribution [[_ name & params]]
  (apply list (cons (foppl-distr-for name) params)))

(defmethod pass2 'function [[_ name & params]]
  (apply list (cons (foppl-fn-for name) params)))

(defmethod pass2 'inv-link-fn [[_ name expr]]
  (apply list (cons (foppl-inv-link-fn-for name) expr)))


; PASS 3
; 


(let [input (parse "grammars/bugs.g4" "examples/v1_seeds")
      p1 (walk-ast postwalk pass1 input)
      p2 (walk-ast postwalk pass2 p1)]
  p2)
