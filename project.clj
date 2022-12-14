(defproject com.dkdhub/clj-mqtt-broker "0.0.3"
  :description "A Clojure wrapper to the Moquette Broker library"
  :url "https://github.com/dkdhub/clj-mqtt-broker"
  :license {:name "Built In Project License"}
  :dependencies [[io.moquette/moquette-broker "0.15"
                  :exclusions [com.bugsnag/bugsnag
                               org.slf4j/slf4j-api
                               org.slf4j/slf4j-log4j12]]])
