(ns bugs2foppl.main
  (use [clojure.pprint :only [pprint]]
       [bugs2foppl.utils]
       [loom.graph]
       [loom.io :only (view)]
       [loom.alg :only (topsort)]
       [clojure.walk]
       :reload-all))


(parse "grammars/bugs.g4" "examples/PLA2_example3")

(parse "grammars/R_data.g4" "examples/examples_JAGS/classic-bugs/vol1/blocker/blocker-data.R")

(parse "grammars/R_data2.g4" "examples/examples_JAGS/classic-bugs/vol1/blocker/bench-test2.R")

; (inspect-tree (parse "grammars/R_data2.g4" "examples/examples_JAGS/classic-bugs/vol1/blocker/bench-test2.R"))

(println (slurp "examples/PLA2_example3"))


; TODO maybe think about this derive issue but not now
; (def stochasticRelation ::stochasticRelation)
; (derive :stochasticRelation ::relation)
; (derive ::deterministicRelation :relation)
;
; (namespace)
;
; (parents :deterministicRelation)

; TODO it would be great if there would be a way to change the nth command to sth more meaningful depending ont he type of node we're visiting at the moment

(let [tree (parse "grammars/bugs.g4" "examples/PLA2_example3")
      es (get-graph-edges tree {})
      g (apply digraph es)
      nso (topsort g)
      v2n (build-relation-node-map tree)
      n2t (fn [n] (clojure.string/join " " (flatten (walk-ast postwalk (partial translate-node-visit {}) n))))
      nodes (fn [v2n nso] (map (partial get v2n) nso))
      output (map n2t (nodes v2n nso))]
  output)

(let [tree (parse "grammars/bugs.g4" "examples/PLA2_example3")
      output (walk-ast postwalk (partial translate-node-visit {}) tree)]
  (println output))

(println (slurp "examples/PLA2_example4"))

(parse "grammars/bugs.g4" "examples/PLA2_example4")
