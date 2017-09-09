(ns bugs2foppl.utils
  (require [clj-antlr.coerce :as coerce]
           [clj-antlr.interpreted :as interpreted]
           [clojure.java.io :as io])
  (use [clojure.pprint :only [pprint]]
       [clojure.walk]))

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

; TODO changed order of [context node]
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
  ([visit-func context node]
   (map visit-func
        (repeat context)
        (filter node? (rest node)))))


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

(defn text [node] (walk-ast postwalk (fn [node] (clojure.string/join " " (rest node))) node))

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
