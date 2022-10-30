(ns basic
  (:require [clojure.test :refer :all]
            [clj-mqtt-broker.core :refer :all]
            [clojure.tools.logging :as log])
  (:import (io.moquette.interception InterceptHandler)
           (com.dkdhub.mqtt_broker SimpleBroker)
           (java.nio.charset StandardCharsets)
           (clj_mqtt_broker.core Broker)))

(defrecord BasicHandler [id]
  InterceptHandler
  (onPublish [_ msg]
    (log/info "got a message")
    (let [payload (-> msg .getPayload (.toString StandardCharsets/UTF_8))]
      (log/info "Received on topic: " + (.getTopicName msg) + " content: " + payload)))
  (getID [_] id)
  (getInterceptedMessageTypes [_] InterceptHandler/ALL_MESSAGE_TYPES))

(def config-name "moquette.conf")

(deftest check-constructs
  (testing "Checking basic constructs"
    (is (instance? InterceptHandler (BasicHandler. "1234")))
    (is (let [srv (SimpleBroker. config-name)]
          (.start srv (BasicHandler. "1234"))
          (.stop srv)
          true))
    (is (let [srv (Broker. (SimpleBroker. config-name))]
          (start srv (BasicHandler. "5678"))
          (stop srv)
          true))
    (is (with-open [srv (Broker. (SimpleBroker. config-name))]
          (start srv (BasicHandler. "9012"))
          (Thread/sleep 20000)
          (send srv "FROM" "/TEMPERATURE" "TEST" 0 false)
          (Thread/sleep 20000)
          true))))
