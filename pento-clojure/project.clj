(defproject pento-clojure "0.1.0-SNAPSHOT"
  :description "Email pentothal server - truth if you're a person."

  :dependencies [[org.clojure/clojure "1.5.1"]  [org.clojars.amit/commons-codec "1.8.0"]
                 [org.clojure/tools.nrepl "0.2.3"]
                 [zololabs/zolo-utils "0.1.0-SNAPSHOT"]
                 
                 [compojure "1.1.5" ]
                 [ring/ring-core "1.2.0-beta1"]
                 [fuziontech/ring-json-params "0.2.0"]
                 [ring-serve "0.1.2"]
                 [clj-http "0.5.3"]

                  ;;Logging Related Stuff
                 [org.clojure/tools.logging "0.2.4"]
                 [ch.qos.logback/logback-classic "1.0.7"]
                 [ch.qos.logback/logback-core "1.0.6"]
                 [ch.qos.logback.contrib/logback-json-classic "0.1.2"]
                 [ch.qos.logback.contrib/logback-jackson "0.1.2"]
                 [org.codehaus.jackson/jackson-core-asl "1.9.12"]
                 [com.fasterxml.jackson.core/jackson-databind "2.2.2"]
                 [org.slf4j/slf4j-api "1.7.0"]
                 [clj-logging-config "1.9.10" :exclusions [log4j]]

                 [org.clojure/tools.cli "0.2.2"]]

  :exclusions [org.clojure/clojure
               org.slf4j/slf4j-log4j12
               org.slf4j/slf4j-api
               org.slf4j/slf4j-nop
               log4j/log4j
               log4j
               org.netflix.curator/curator-framework
               commons-logging/commons-logging
               org.clojure/tools.logging
               org.clojure/clojure-contrib]


  :profiles {:dev {:dependencies [[clj-stacktrace "0.2.4"]
                                  [ring-serve "0.1.2"]
                                  [zololabs/marconi "1.0.0-SNAPSHOT"]
                                  [org.clojars.runa/conjure "2.1.1"]
                                  [difform "1.1.2"]
                                  [org.clojure/data.generators "0.1.0"]
                                  [org.clojure/math.combinatorics "0.0.4"]]
                   :resource-paths [~(str (System/getProperty "user.home") "/.pento")]}}

  :main zolo.pento.app

  :bootclasspath false

  :jvm-opts ["-Xmx1g" "-server"])
