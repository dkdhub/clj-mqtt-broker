(ns basic
  (:require [clojure.test :refer :all]
            [clj-mqtt-broker.core :refer :all]
            [clojure.tools.logging :as log])
  (:import (io.moquette.interception InterceptHandler)
           (io.moquette BrokerConstants)
           (io.netty.handler.codec.mqtt MqttQoS)
           (com.dkdhub.mqtt_broker AdvancedBroker SimpleBroker)
           (java.nio.charset StandardCharsets)
           (clj_mqtt_broker.core Broker)
           (java.util Properties)))

(defrecord BasicHandler [id]
  InterceptHandler
  (onPublish [_ msg]
    (log/info "got a message")
    (let [payload (-> msg .getPayload (.toString StandardCharsets/UTF_8))]
      (log/info "Received on topic: " + (.getTopicName msg) + " content: " + payload)))
  (getID [_] id)
  (getInterceptedMessageTypes [_] InterceptHandler/ALL_MESSAGE_TYPES))

(def config-name "moquette.conf")

(deftest check-basic-constructs
  (testing "Checking basic constructs"

    (log/info "--------- instance? -------------")
    (is (instance? InterceptHandler (BasicHandler. "1234")))

    (log/info "--------- Java interface ---------")
    (is (let [srv (SimpleBroker. config-name)]
          (.start srv (BasicHandler. "1234"))
          (.stop srv)
          true))

    (log/info "--------- Clojure interface [start/stop] ---------")
    (is (let [srv (Broker. (SimpleBroker. config-name))]
          (start srv (BasicHandler. "5678"))
          (stop srv)
          true))

    (log/info "--------- Clojure interface [with-open + start] ---------")
    (is (with-open [srv (Broker. (SimpleBroker. config-name))]
          (start srv (BasicHandler. "9012"))
          (Thread/sleep 10000)
          (send srv "FROM" "/TEMPERATURE" "TEST" 0 false)
          (Thread/sleep 10000)
          true))

    (log/info "--------- Clojure interface [with-open + open] ---------")
    (is (let [b (Broker. (SimpleBroker. config-name))]
          (with-open [srv (open b (BasicHandler. "3456"))]
            (Thread/sleep 10000)
            (send srv "FROM" "/TEMPERATURE" "TEST" 1 false)
            (Thread/sleep 10000))
          true))))

(deftest check-helpers
  (testing "Checking helpers and stuff"

    (log/info "--------- MQTT config definitions ---------")
    (log/info "  the default message size is:" default-message-size-bytes)
    (is (= default-message-size-bytes BrokerConstants/DEFAULT_NETTY_MAX_BYTES_IN_MESSAGE)
        "The max size defined and a default for MQTT messages is differs from Broker's constants!")

    (log/info "  the default QoS is" MqttQoS/EXACTLY_ONCE)
    (is (= MqttQoS/EXACTLY_ONCE (->QoS nil)))

    (log/info "--------- MQTT config creation ---------")
    (log/info "  the defaults for AdvancedBroker are:" (mqtt-config))
    (let [config (mqtt-config)]
      (is (instance? Properties config) "Generated config is not a Property class instance")
      (is (= "disabled" (.getProperty config BrokerConstants/PORT_PROPERTY_NAME)))
      (is (= "disabled" (.getProperty config BrokerConstants/WEB_SOCKET_PORT_PROPERTY_NAME)))
      (is (= "0.0.0.0" (.getProperty config BrokerConstants/HOST_PROPERTY_NAME)))
      (is (empty? (.getProperty config BrokerConstants/PASSWORD_FILE_PROPERTY_NAME)))
      (is (= (str (Boolean/parseBoolean "false")) (.getProperty config BrokerConstants/ALLOW_ANONYMOUS_PROPERTY_NAME)))
      (is (= "/" (.getProperty config BrokerConstants/WEB_SOCKET_PATH_PROPERTY_NAME))))))

(deftest check-advanced-constructs
  (testing "Checking advanced constructs")

  (log/info "--------- MQTT Advanced Broker empty loop ---------")
  (is (let [b (Broker. (AdvancedBroker. (mqtt-config)))]
        (with-open [srv (open b (BasicHandler. "3456"))]
          (Thread/sleep 2000)
          (send srv "FROM" "/TEMPERATURE" "TEST" 1 false)
          (Thread/sleep 2000))
        true))

  (is (let [b (Broker. (SimpleBroker. config-name))]
        (with-open [srv (open b (BasicHandler. "3456"))]
          (nil? (clients srv)))))

  (is (let [b (Broker. (AdvancedBroker. (mqtt-config)))]
        (nil? (clients b))))

  (is (let [b (Broker. (AdvancedBroker. (mqtt-config)))]
        (with-open [srv (open b (BasicHandler. "3456"))]
          (sequential? (clients srv))))))
