(ns bugs2foppl.example
  (require [foppl.desugar :refer :all]
           [foppl.core :as foppl :refer [foppl-query print-graph]]
           [bugs2foppl.core :refer [translate-bugs-to-foppl]] :reload
           [bugs2foppl.utils :refer [plot-foppl-graph save-foppl-graph]] :reload
           [clojure.repl :refer [pst]])
  (use [anglican runtime]
       [clojure.pprint :only [pprint]]))

(let [model-file "examples/examples_JAGS/classic-bugs/vol1/seeds/seeds.bug"
      data-file "examples/examples_JAGS/classic-bugs/vol1/seeds/seeds-data-short.R"
      foppl-model (translate-bugs-to-foppl model-file data-file)
      [G E] (eval foppl-model)]
  foppl-model)

(let [model-file "examples/examples_JAGS/classic-bugs/vol2/eyes/eyes2.bug"
      data-file "examples/examples_JAGS/classic-bugs/vol2/eyes/eyes-data-short.R"
      output-svg-file "examples/svg-output.svg"
      foppl-model (translate-bugs-to-foppl model-file data-file)
      [G E] (eval foppl-model)]
  foppl-model)
