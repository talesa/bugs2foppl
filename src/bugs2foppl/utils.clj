(ns bugs2foppl.utils)

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
