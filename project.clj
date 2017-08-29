(defproject bugs2foppl "0.1.0"
  :description "BUGS to FOPPL transpiler"
  :url "https://github.com/talesa/bugs2foppl"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-antlr "0.2.5-SNAPSHOT"]
                 [aysylu/loom "1.0.0"]
                 [foppl "0.1.0-SNAPSHOT"]
                 [anglican "1.0.0"]]
  :repositories [["anglican" "https://anglican.s3-eu-west-1.amazonaws.com/"]]
  :jvm-opts ["-Xmx6g" "-Xms4g"])