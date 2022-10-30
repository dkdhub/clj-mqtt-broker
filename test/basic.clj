(ns basic
  (:require [clojure.test :refer :all]
            [clj-mqtt-broker.core :refer :all])
  (:import (io.moquette.interception InterceptHandler)
           (com.dkdhub.mqtt_broker SimpleBroker)
           (java.nio.charset StandardCharsets)
           (clj_mqtt_broker.core Broker)))

(defrecord BasicHandler [id]
  InterceptHandler
  (onPublish [_ msg]
    (println "got a message")
    (let [payload (-> msg .getPayload (.toString StandardCharsets/UTF_8))]
      (println "Received on topic: " + (.getTopicName msg) + " content: " + payload)))
  (getID [_] id)
  (getInterceptedMessageTypes [_] InterceptHandler/ALL_MESSAGE_TYPES))

(deftest check-constructs
  (testing "Checking basic constructs"
    (is (instance? InterceptHandler (BasicHandler. "1234")))
    (is (let [srv (SimpleBroker.)]
          (.start srv (BasicHandler. "1234"))
          (.stop srv)
          true))
    (is (let [srv (Broker. (SimpleBroker.))]
          (start srv (BasicHandler. "1234"))
          (send srv "FROM" "TO" "TEST" 0 false)
          (Thread/sleep 20000)
          (stop srv)
          true))))
