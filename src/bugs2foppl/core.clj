(ns bugs2foppl.core
  (require [zip.visit :as zv]
           [clojure.zip :as z]
           [foppl.desugar :refer :all]
           [foppl.core :as foppl :refer [foppl-query print-graph]] :reload)
  (use [bugs2foppl.utils]
       [clojure.pprint :only [pprint]]
       [clojure.inspector :only (inspect-tree)]
       [loom.graph]
       [loom.io :only (view)]
       [loom.alg :only (topsort)]
       [clojure.walk]
       [clojure.repl]))
      ;  :reload-all))

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

; DATA PASS 1
(defmulti dpass1 first)
(defmethod dpass1 :default [node] node)
(defmethod dpass1 :input [[_ var-assignment-list]]
  (list 'assignment-list var-assignment-list))
(defmethod dpass1 :varAssignmentList [node]
  (extract-seq-from-left-recursive-rule node :no-comma))
(defn strip-quotes [stringg] (clojure.string/replace stringg "\"" ""))
(defmethod dpass1 :varAssignment [[_ name _ expr]]
  (list (symbol (sanitize-var-name (strip-quotes name))) expr))
(defmethod dpass1 :sublist [node]
  (extract-seq-from-left-recursive-rule node :comma))
(defmethod dpass1 :sub [[_ rst]] rst)
(defmethod dpass1 :functionCall [[_ name _ sublist]]
  (concat ['function (symbol name)] sublist))
(defmethod dpass1 :number [[_ number]] (read-string number))
(defmethod dpass1 :numberexpr [[_ number]] number)
(defmethod dpass1 :assignment [[_ name _ expr]]
  (list 'assignment name expr))
(defmethod dpass1 :expression [[_ & rst]]
  (list 'expression rst))

; DATA PASS 2
(defmulti dpass2 first)
(defmethod dpass2 :default [node] node)
(defmethod dpass2 'function [[_ name & args]]
  (case (str name)
    "c" args
    "as.integer" (map int (first args))
    "structure" (concat [(symbol name)] args)
    "list" args))
(defmethod dpass2 'expression [[_ expr]]
  (case (first expr)
    "NA" nil
    (list 'expression expr)))

; DATA PASS 3
(defmulti dpass3 first)
(defmethod dpass3 :default [node] node)
(defmethod dpass3 'structure [[_ coll & assignments]]
  (let [dim-assgn (filter #(= (second %) ".Dim") assignments)
        dims (nnth 2 (first dim-assgn))]
    (v2m coll dims)))
(defmethod dpass3 'assignment-list  [[_ assignments]] assignments)

; DATA PASS 4
; (defmulti dpass4 first)
; (defmethod dpass4 :default [node] node)
; (defmethod dpass4 'assignment-list  [[_ assignments]]
;   (list 'let (vec (apply concat assignments))))


; PASS 1
; sanitizes var names
; changes the structure of the code from parser artifacts to LISP notation
(defmulti pass1 first)
(defmethod pass1 :default [node] node)

(defmethod pass1 :input [[_ model]] model)

(defmethod pass1 :varID [[_ name]]
  (list 'var (symbol (sanitize-var-name name))))
(defmethod pass1 :varIndexed
  [[_ name _ range-list]]
  (list 'var-indexed (symbol (sanitize-var-name name)) range-list))
; TODO possibly decrease the indeces above
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
    (list var range-element)))

(defmethod pass1 :forLoop [[_ [var range] relations]]
  (list 'for var range relations))

(defmethod pass1 :deterministicRelation1 [[_ var _ expr]]
  (list 'deterministic-relation var expr))

(defmethod pass1 :deterministicRelationLink [[_ link-fn _ var _ _ expr]]
  (list 'deterministic-relation var (list 'inv-link-fn (symbol link-fn) expr)))

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
(defmethod pass2 :default [n] n)

; TODO some distributions and functions may need to have the order of arguments or parameterization of arguments
(defmethod pass2 'distribution [[_ name & params]]
  (apply list (cons (foppl-distr-for name) params)))

(defmethod pass2 'function [[_ name & params]]
  (apply list (cons (foppl-fn-for name) params)))

(defmethod pass2 'inv-link-fn [[_ name expr]]
  (apply list (cons (foppl-inv-link-fn-for name) (list expr))))

; PASS 3
; fill in the values of the var nodes (not var-indexed) where this is necessary to evaluate the ranges for the loop
; unroll loops

; context needs to contain the static data

(defmulti pass3 (fn [context node] (first node)))
(defmethod pass3 :default [context node] node)

(defmethod pass3 'for [context [_ var range relations]]
  (let [data (:data context)
        var-symbol (second var)
        a (partial sub-var data)
        range (->> range
                   a
                   partial-eval
                   sub-range)]
    (concat ['list-of-rels]
      (apply concat
        (map
          (fn [var-value]
            (map
             (fn [relation]
               (sub-var
                (assoc data var-symbol var-value)
                relation))
             relations))
          range)))))

; PASS 4
; gather all the relations
(defmulti pass4 first)
(defmethod pass4 :default [node]
  (apply concat (visit-children pass4 node)))
(defmethod pass4 'stochastic-relation [node] (list node))
(defmethod pass4 'deterministic-relation [node] (list node))


; (defmulti pass5
;   (fn [context node] (first node))
;   :hierarchy (-> (make-hierarchy)
;                  (derive 'stochastic-relation :relation)
;                  (derive 'deterministic-relation :relation)
;                  atom))
; (defmethod pass5 :default [context node]
;   (apply concat (visit-children pass5 context node )))
;
; (defmethod pass5 :relation [context node]
;   (let [to-var (second (second node)) ; var name string
;         context (assoc context :to-var to-var)]
;     (pass5 (nth node 3) context)))
; (defmethod pass5 :varID [context node]
;   (list (list (second node) (:to-var context))))

; PASS 5
; keep track of:
; for indexed variables keep track of the maximum
; ? set of variables binded
; if an indexed variable associated to is not binded initiate it to array of nils of appropriate shape by looking at the indices used

(zv/defvisitor keep-maximum-of-indexed-vars :pre [n s]
  (if (and
       (node? n)
       (or (= (first n) 'stochastic-relation)
           (= (first n) 'deterministic-relation))
       (= (-> n second first) 'var-indexed))
    (let [[_ var] n
          [_ var-symbol var-index] var
          max-indexed-vars (:max-indexed-vars s)
          data (:data s)]
      (if (not (contains? (set (keys data)) var-symbol))
        (if (some (partial = :all) var-index)
          (throw (Exception. "Do not know how to handle :all in keep-maximum-of-indexed-vars."))
          {:state (assoc s :max-indexed-vars
                         (assoc max-indexed-vars var-symbol
                                (vec-max
                                 (get max-indexed-vars var-symbol)
                                 var-index)))})))))

(defn initiate-nil-arrays [max-indexed-vars]
  (map
   (fn [[var-symbol lens]]
     (list
      var-symbol
      ; TODO possibly may need to add vec here
      (v2m (repeat (apply * lens) nil) lens)))
   max-indexed-vars))

; PASS 6
; create the dependency graph
;   easy for nonindexed relations
;   for indexed varaibles assignment set the name of the node to var_i_j or sth like that
(defmulti pass6 (fn [context node] (first node))
  :hierarchy (-> (make-hierarchy)
                 (derive 'stochastic-relation :relation)
                 (derive 'deterministic-relation :relation)
                 (derive 'var :var)
                 (derive 'var-indexed :var)
                 atom))
(defmethod pass6 :default [context node]
  (apply concat (visit-children pass6 context node)))


(defn combine-var-index [var-symbol var-index]
  (symbol (str (str var-symbol) "_" (clojure.string/join "_" var-index))))

(defmethod pass6 'stochastic-relation [context [_ [var-type to-var-symbol var-index] expr]]
  (let [to-var-symbol (if (= var-type 'var)
                          to-var-symbol
                          (combine-var-index to-var-symbol var-index))
        context (assoc context :to-var to-var-symbol)]
    (pass6 context expr)))
; TODO expr may be a number in deterministic-relation
(defmethod pass6 'deterministic-relation [context [_ [var-type to-var-symbol var-index] expr]]
  (if (not (number? expr))
    (let [to-var-symbol (if (= var-type 'var)
                            to-var-symbol
                            (combine-var-index to-var-symbol var-index))
          context (assoc context :to-var to-var-symbol)]
      (pass6 context expr))
    (list)))

(defmethod pass6 'var [context [_ var-symbol]]
  (list var-symbol (:to-var context)))
(defmethod pass6 'var-indexed [context [_ var-symbol var-index]]
  (list (combine-var-index var-symbol var-index) (:to-var context)))

; PASS 7
; get relations to map
(defmulti pass7 (fn [node] (first node)))
(defmethod pass7 :default [node]
  (apply merge (visit-children pass7 node)))
(defmethod pass7 'stochastic-relation [[_ [var-type to-var-symbol var-index] expr :as node]]
  (let [to-var-symbol (if (= var-type 'var)
                          to-var-symbol
                          (combine-var-index to-var-symbol var-index))]
    {to-var-symbol node}))
(defmethod pass7 'deterministic-relation [[_ [var-type to-var-symbol var-index] expr :as node]]
  (let [to-var-symbol (if (= var-type 'var)
                          to-var-symbol
                          (combine-var-index to-var-symbol var-index))]
    {to-var-symbol node}))

; TODO potentially control if the same variable is assigned multiple times or just assume correct BUGS input

; PASS 8
; resolve relations according to var/var-indexed
; check whether var-indexed is nil and observe/sample appropriately
(defn is-nil-in-data?
  "Checks whether the var is nil given the data map. "
  ([data var]
   (let [[var-type var-symbol var-index] var]
     (is-nil-in-data? data var-symbol var-index)))
  ([data var-symbol var-index]
   (let [var-index (map dec var-index)]
     (nil? (if (nil? var-index)
             (get data var-symbol)
             (-> (get data var-symbol)
                 (get-in var-index)))))))
(defmulti pass8 (fn [data node] (first node)))
(defmethod pass8 'stochastic-relation [data [_ var expr]]
  (if (is-nil-in-data? data var)
    (list var (list 'sample expr))
    (list '_ (list 'observe expr var))))
(defmethod pass8 'deterministic-relation [data [_ var expr]]
  (list var expr))

; TODO in pass8 I'm not keeping track of the new variables being assigned values so I'm taking into account the situation when a variable might be transformed using deterministic relationship and then it should be observed rather than sampled and possibly other situations as well)

; TODO at which point should I decrease the indeces of the var-indexed

; PASS 9
; resolve var-indexed nodes on the left hand side of the relation so that the expr on the right hand side would be assoced into appropriate position in the array
(defn pass9 [[var expr :as node]]
  (if (not (= '_ var))
    (let [[var-type var-symbol var-index] var]
      (if (= var-type 'var-indexed)
        (list var-symbol (list 'assoc-in var-symbol (vec (map dec var-index)) expr))
        (list var-symbol expr)))
    node))

; PASS 10
; resolve var nodes
; resolve var-indexed nodes
(defmulti pass10 first)
(defmethod pass10 :default [n] n)
(defmethod pass10 'var [[_ var-symbol]] var-symbol)
(defmethod pass10 'var-indexed [[_ var-symbol var-index]] (list 'get-in var-symbol (vec (map dec var-index))))

; PASS 11
; combine data and model into final output
(defn pass11 [data-map relations]
  (list
   'let
    (vec
     (concat
      (apply concat (map identity data-map))
      (apply concat relations)))))

(defn translate-bugs-to-foppl [model-file data-file]
  (let [data-map
        (->> (parse "grammars/R_data.g4" data-file)
             (walk-ast dpass1)
             (walk-ast dpass2)
             (walk-ast dpass3)
             (map (fn [[f s]] (list f (if (seq? s) (vec s) s))))
             (map vec)
             (into {}))
        p4
        (->> (parse "grammars/bugs.g4" model-file)
             (walk-ast pass1)
             (walk-ast pass2)
             (walk-ast prewalk (partial pass3 {:data data-map}))
             pass4)
        p5
        (zv/visit (z/seq-zip p4)
              {:data data-map :max-indexed-vars {}}
              [keep-maximum-of-indexed-vars])
        data-map
        (into data-map
              (map vec
                   (initiate-nil-arrays (-> p5 :state :max-indexed-vars))))
        edges (filter (complement empty?) (map (partial pass6 {}) p4))
        ; TODO this is so ugly, fix it
        edges (partition 2 (flatten edges))
        v2n (into {} (map pass7 p4))
        g (apply digraph edges)
        ; _ (view g)
        nso (topsort g)
        nso (concat nso (clojure.set/difference (set (keys v2n)) (set nso)))
        nodes (fn [v2n nso] (map (partial get v2n) nso))
        ordered-rels1 (nodes v2n nso)
        ordered-rels (filter (complement empty?) ordered-rels1)
        p8 (map (partial pass8 data-map) ordered-rels)
        p9 (map pass9 p8)
        p10 (map (fn [[var expr]] (list var (walk-ast pass10 expr))) p9)
        output (pass11 data-map p10)
        foppl-ds (list 'foppl-query output)]
    foppl-ds))
