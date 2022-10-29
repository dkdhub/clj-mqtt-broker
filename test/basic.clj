(ns basic
  (:require [clojure.test :refer :all]
            [clj-mqtt-broker.core :refer :all])
  (:import (io.moquette.interception InterceptHandler)
           (clj_mqtt_broker.core BasicHandler)
           (com.dkdhub.mqtt_broker SimpleBroker)))

(deftest check-constructs
  (testing "Checking basic constructs"
    (is (instance? InterceptHandler (BasicHandler. "1234")))
    (is (let [srv (SimpleBroker.)]
          (.start srv (BasicHandler. "1234"))
          (.stop srv)
          true))))
