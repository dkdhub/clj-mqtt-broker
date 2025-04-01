(defproject com.dkdhub/clj-mqtt-broker "0.0.7-SNAPSHOT"
  :description "A Clojure wrapper to the Moquette Broker library"
  :url "https://github.com/dkdhub/clj-mqtt-broker"
  :license {:name "Built In Project License"}
  :repositories [["jitpack" "https://jitpack.io"]]
  :dependencies [[com.github.moquette-io/moquette "0.18.0"
                  :exclusions [com.bugsnag/bugsnag
                               org.slf4j/slf4j-api
                               org.slf4j/slf4j-log4j12]]])
