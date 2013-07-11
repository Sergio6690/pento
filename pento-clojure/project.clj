(defproject pento-clojure "0.1.0-SNAPSHOT"
  :description "Email pentothal server - truth if you're a person."
  :dependencies [[org.clojure/clojure "1.5.1"]  [org.clojars.amit/commons-codec "1.8.0"]
                 [org.clojure/tools.nrepl "0.2.3"]
                 [zololabs/zolo-utils "0.1.0-SNAPSHOT"]
                 
                 [compojure "1.1.5" ]
                 [ring/ring-core "1.2.0-beta1"]
                 [fuziontech/ring-json-params "0.2.0"]
[ring-serve "0.1.2"]
                 [clj-http "0.5.3"]]
    :profiles {:dev {:dependencies [[clj-stacktrace "0.2.4"]
                                  [ring-serve "0.1.2"]
                                  [zololabs/marconi "1.0.0-SNAPSHOT"]
                                  [org.clojars.runa/conjure "2.1.1"]
                                  [difform "1.1.2"]
                                  [org.clojure/data.generators "0.1.0"]
                                  [org.clojure/math.combinatorics "0.0.4"]]
                   :resource-paths [~(str (System/getProperty "user.home") "/.pento")]}})
