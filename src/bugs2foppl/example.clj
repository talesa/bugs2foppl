(ns bugs2foppl.example
  (require [foppl.desugar :refer :all]
           [foppl.core :as foppl :refer [foppl-query print-graph]] :reload)
  (use [bugs2foppl.core]
       [anglican runtime]
       [clojure.pprint :only [pprint]]))

(let [model-file "examples/examples_JAGS/classic-bugs/vol1/seeds/seeds.bug"
      data-file "examples/examples_JAGS/classic-bugs/vol1/seeds/seeds-data-short.R"
      foppl-model (translate-bugs-to-foppl model-file data-file)]
  (pprint foppl-model)
  (eval foppl-model))
