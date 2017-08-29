(ns zippers.domain
  (require [clojure.set :as set])
  (use [bugs2foppl.utils]
       [anglican core runtime emit stat]))

(defn- expected-keys? [map expected-key-set]
  (not (seq (set/difference (set (keys map)) expected-key-set))))

(defmacro defnode
  [node-type [& fields]]
  (let [constructor-name (symbol (str "new-" node-type))]
    `(defn ~constructor-name [nv-map#]
       {:pre [(map? nv-map#)
              [expected-keys? nv-map# ~(set (map keyword fields))]]}
       (assoc nv-map# :type (keyword '~node-type)))))

(defn call-fn [this & that]
  (cond
    (seq? this)     (apply (resolve (symbol (clojure.string/join "-" this))) that)
    (keyword? this) (apply (resolve (symbol (name this))) that)
    (string? this)  (apply (resolve (symbol this)) that)
    (fn? this)      (apply this that)
    :else          (conj that this)))


; EXPERIMENTS WITH QUOTING FOPPL

(use '[anglican core runtime emit stat])

(let [b '(foppl-query (let [a (sample (normal 0 1))]))
      [G E] (eval b)]
  b)

(let []
  (defquery one-flip [outcome]
    (let [bias (sample (uniform-continuous 0.0 1.0))]
      (observe (flip bias) outcome)
      bias))
  (def samples
    (doquery :importance one-flip [true]))
  (empirical-mean
    (collect-results
      (take 10 samples))))

(macroexpand
 '(defquery one-flip [outcome]
   (let [bias (sample (eval (list (foppl-distr-for "dunif") 0.0 1.0)))]
     (observe (flip bias) outcome)
     bias)))


; experiments with quoting/passing things to for
(let [v ['b (range 2)]]
   (for v b))

(let [v '[b (range 2)]]
   (for v b))

(type (range 2))

(let []
   (for [b (list 0 1)] b))

()

(let [v '[b (range 2)]]
  (eval `(for ~v ~'b)))

(defmacro forv [seq-exprs body]
  `(vec (for ~seq-exprs ~body)))

(let [v '[b (range 2)]
      body 'b]
  (macroexpand '(forv v body)))

(defmacro forv2 [seq-exprs body]
  `(vec (for seq-exprs body)))

(let [v '[b (range 2)]
      body 'b]
  (macroexpand '(forv2 v body)))


(binding [v ['b (range 2)]
          body 'b]
  (forv v body))


(let []
   (for [b (range 2)] b))

(take 100 (for [x (range 100000000) y (range 1000000) :while (< y x)] [x y]))
