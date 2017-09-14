(ns bugs2foppl.utils
  (require [clj-antlr.coerce :as coerce]
           [clj-antlr.interpreted :as interpreted]
           [clojure.java.io :as io]
           [clojure.repl :refer [pst]]
           [rhizome.viz]
           [incanter distributions])
  (use [clojure.pprint :only [pprint]]
       [clojure.walk]))

(defn count1? [coll] (= 1 (count coll)))

(defn vec-max
  "Max function which can perform element-wise vector max and handles nil arguments."
  [a b]
  (cond
    (every? number? [a b]) (clojure.core/max a b)
    (= a nil) b
    (= b nil) a
    (every? coll? [a b]) (vec (map max a b))))

(defn sum [coll] (reduce + coll))

(defn normalize
  "Normalizes a vector."
  [v]
  {:pre [(every? number? v)]}
  (let [magnitude (sum v)]
    (vec (map (partial * (/ 1. magnitude)) v))))

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

(defn node->descriptor [G n]
  (let [[V A P O] G
        body (print-str (-> P n :body))
        label (str n ": " body)
        descriptor (if (contains? O n)
                     {:label (list label (-> O n :value))
                      :fillcolor "gray"
                      :style "filled"}
                     {:label label})]
     descriptor))

(defn rhizome-helper [G]
  (let [[V A P O] G
        adj-list (into {} (map (fn [[k v]] [k (map #(second %) v)]) (group-by first A)))]
    [V adj-list :node->descriptor (partial node->descriptor G)]))

(defn plot-foppl-graph [G] (apply rhizome.viz/view-graph (rhizome-helper G)))

(defn save-foppl-graph [G output-svg-file]
  (spit output-svg-file
    (apply rhizome.viz/graph->svg (rhizome-helper G))))
