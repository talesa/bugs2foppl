(ns bugs2foppl.utils
  (require [clj-antlr.coerce :as coerce]
           [clj-antlr.interpreted :as interpreted]
           [clojure.math.combinatorics :as combo]
           [clojure.java.io :as io])
          ;  :reload-all)
  (use [clojure.pprint :only [pprint]]
       [clojure.inspector :only (inspect-tree)]
       [loom.graph]
       [loom.io :only (view)]
       [loom.alg :only (topsort)]
       [clojure.walk]
      ;  [clojure.tools.trace]
       [clojure.repl]
       :reload-all))
      ;  [foppl.core]))
      ;  [anglican core runtime emit stat]
      ;  :reload-all))

(defn count1? [coll] (= 1 (count coll)))

(defn vec-max
  "Element-wise vector max function."
  [a b]
  (if (= a nil)
    b
    (if (= b nil)
      a
      (vec (map max a b)))))

(defn v2m
  "Forms an nested vector array of dimensions dims from a vector coll."
  [coll lens]
  {:pre [(= (apply * lens) (count coll))]}
  (if (count1? lens)
    (vec coll)
    (let [partitioned (partition (apply * (rest lens)) coll)]
      (vec (map #(v2m % (rest lens)) partitioned)))))

(defn node? [n] (and (seq? n) ((some-fn keyword? symbol?) (first n))))

(defn walk-ast
  "Uses walker to apply rule-node-visit only to rule nodes of the tree, rather than token nodes."
  ([rule-node-visit tree] (walk-ast clojure.walk/postwalk rule-node-visit tree))
  ([walker rule-node-visit tree]
   (let [f (fn [n] (if (node? n)
                     (rule-node-visit n)
                     n))]
     (let [new-tree (do (walker f tree))]
       new-tree))))

(defn parse
  [grammar-file input-file-or-string]
  (let [input-string (if (.exists (io/file input-file-or-string))
                       (slurp input-file-or-string)
                       input-file-or-string)
        grammar-str (slurp grammar-file)
        grammar (interpreted/grammar grammar-str)
        m (interpreted/parse grammar {} input-string)
        e (coerce/tree->sexpr m)]
    e))

; (defn visit-children
;   "Returns a sequence of visited children."
;   [visit-func node & context]
;   (if (empty? context)
;     (map visit-func (filter node? (rest node)))
;     (map visit-func (filter node? (rest node)) (repeat (first context)))))

(defn visit-children
  "Returns a sequence of visited children."
  ([visit-func node]
   (map visit-func (filter node? (rest node))))
  ([visit-func node context]
   (map visit-func
        (filter node? (rest node))
        (repeat context))))

; TODO possibly change to tail recursion?
(defn find-nodes
  "Returns seq of type nodes found within root."
  [type root]
  (if (= (first root) type)
    (list root)
    (apply concat
           (visit-children (partial find-nodes type) root))))

(defn find-first
  "Returns first type node within root."
  [type root]
  (first (find-nodes type root)))

(defn unsugar-var [var-name-str index] (str var-name-str "_" index))

(defn nnth [index coll] (nth coll index))

(defn unroll-data [data]
  ; there might be some obscure side effects with seq? here such as breaking down strings into sequences of chars
  (let [seqs (filter (comp seq? val) data)
        f1 (fn [kv]
             (for [i (range (count (val kv)))]
              {(unsugar-var (key kv) (inc i)) (nth (val kv) i)}))]
    (apply merge data (flatten (map f1 seqs)))))

(defn sanitize-var-name [var-name] (clojure.string/replace var-name "." "-"))

(def vars (unroll-data {"r" '(10, 23, 23, 26, 17, 5, 53, 55, 32, 46, 10, 8, 10, 8, 23, 0, 3, 22, 15, 32, 3)
                        "n" '(39, 62, 81, 51, 39, 6, 74, 72, 51, 79, 13, 16, 30, 28, 45, 4, 12, 41, 30, 51, 7)
                        "x1" '(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
                        "x2" '(0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1)
                        "N" 4}))

(defn foppl-distr-for [distr]
  (case (str distr)
    "dnorm"   'normal
    "dgamma"  'gamma
    "dunif"   'uniform-continuous
    "dbin"    'binomial))

(defn foppl-fn-for [func]
  (case (str func)
    "sqrt"   'sqrt))

(defn foppl-inv-link-fn-for [func]
  (case (str func)
    "logit"  'sigmoid))

; sub it with edges
(defmulti get-graph-edges (fn [node context] (first node)))
(defmethod get-graph-edges :default [node context]
  (apply concat (visit-children get-graph-edges node context)))

(defmethod get-graph-edges :stochasticRelation [node context]
  (let [to-var (second (second node)) ; var name string
        context (assoc context :to-var to-var)]
    (get-graph-edges (nth node 3) context)))
(defmethod get-graph-edges :deterministicRelation1 [node context]
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
(defmethod build-relation-node-map :deterministicRelation1 [node]
  (let [to-var (second (second node))] ; var name string
    {to-var node}))


; TODO macro to make those definitions shorter

(defmulti translate-node-visit (fn [data node] (first node)))
(defmethod translate-node-visit :default [data node] (second node))
(defmethod translate-node-visit :stochasticRelation [data node]
  (let [var-str (second (find-first :varID (nth node 1)))
        var-str (nth node 1)] ; these 2 should be equivalent
    (pprint var-str)
    (if (contains? data var-str)
      (list "(observe" (nth node 3) (get data var-str) ")")
      (list var-str "(sample" (nth node 3) ")"))))


; UNROLLING FORLOOPS
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

(defn text [node] (walk-ast postwalk (fn [node] (clojure.string/join " " (rest node))) node))

; TODO possibly change this to loop recur
(defn create-expression-list
  "Returns a syntax tree consisting of :relationList nodes given sequence of relations."
  [relations]
  (if (= 1 (count relations))
    (list
     :relationList1
     (list :relation (first relations)))
    (list
     :relationList2
     (list :relation (first relations))
     (create-expression-list (rest relations)))))

(defn vars-ranges-from-counter [counter-node]
  (let [var-name-str  (nth counter-node 3)
        ; var-name-sym  (symbol var-name-str)
        range-expr    (find-first :rangeExpression counter-node)
        ; TODO these two below may need to be evaluated or read from the var list etc, for now the assumption is these are numbers in string format
        var-min       (var-or-int (text (nth range-expr 1)) vars)
        var-max       (var-or-int (text (nth range-expr 3)) vars)
        var-range     (range var-min (+ 1 var-max))]
    {var-name-str var-range}))

(defmulti substitute-varindexed-with-varid (fn [vars-binding node] (first node)))
(defmethod substitute-varindexed-with-varid :default [vars-binding node] node)
(defmethod substitute-varindexed-with-varid :varIndexed [vars-binding node]
  (let [var-name-str  (nnth 1 node)
        vars          (keys vars-binding)
        values        (vals vars-binding)
        range-element (nnth 1 (nnth 3 node))
        ; assuming range-element is just a single varID for now
        index-str    (nnth 1 (nnth 1 (nnth 1 range-element)))
        index-val     (get vars-binding index-str)]
    (list
     :varID
     (unsugar-var var-name-str index-val))))


(defn unroll-loop-relation [node vars-ranges]
  ; (let [vars-ranges ["i" (range 1 11)
  ;                    "j" (range 1 11)]])
  (let [relation (second node)
        vars (keys vars-ranges)
        ranges (vals vars-ranges)
        tuples (apply combo/cartesian-product ranges)
        vars-bindings (map (partial into {}) (map (partial map vector) (repeat vars) tuples))
        ; (let [vars-binding ["i" 2
        ;                     "j" 3]])
        f (fn [vars-binding node] (walk-ast postwalk (partial substitute-varindexed-with-varid vars-binding) node))]
    (map f vars-bindings (repeat relation))))
    ; TODO probably there will be a need for some quoting magic here because I will need to evaluate the expressions in the indeces
    ; I am traversing this node to substitute for each of the iterations of the unrolling - this is inefficient, surely there is a better way

; we need vars-ranges here as the argument because it may be a nested loop
(defmulti unroll-loop (fn [vars-ranges node] (first node)))
(defmethod unroll-loop :default [vars-ranges node] node)

; (let [vars-ranges [x (range 2)
;                    y (range 2)])
; each nested loop will add the appropriate range to vars-ranges
; for now a single variable
(defmethod unroll-loop :forLoop [vars-ranges node]
  (let [counter (nth node 1)
        vars-ranges (merge vars-ranges (vars-ranges-from-counter counter))
        ; TODO this will fail with nested for loops because it will also traverse into the next :forLoop, I need a way to terminate the descent
        relations (find-nodes :relation (nth node 2))]
    (create-expression-list (apply concat (map unroll-loop-relation relations (repeat vars-ranges))))))

; TODO not sure if I can use syntax quote here because let evaluates to clojure.core/let and not sure if that will work within foppl compiler
(defn relations-to-sugared-let
  "Transforms a sequence of untranslated relations to a sugared foppl let statement with translated relations."
  [relations]
  ; (list 'let (read-string (str "[" (clojure.string/join " " relations) "]")))
  `(~'let ~relations))

(defmacro node-traversal-method [type body]
  (let [name (gensym)]
    `(let []
       (defmulti ~name first)
       (defmethod ~name :default [n#] n#)
       (defmethod ~name ~type [m#] (~body m#))
       ~name)))

(defn sub-var
  [bindings-map node]
  (walk-ast
   (node-traversal-method 'var
    #(let [var (second %)]
       (if (contains? bindings-map var)
           (get bindings-map var)
           %)))
   node))

(defn sub-range
  [node]
  (walk-ast
   (node-traversal-method 'range-inclusive
    #(let [min (nnth 1 %)
           max (nnth 2 %)]
       (if (and (number? min) (number? max))
           (range min (inc max))
           %)))
   node))

(defn partial-eval
  [node]
  (walk-ast
   #(let [output
          (try (eval %)
               (catch Exception e false))]
      (if output output %))
   node))
