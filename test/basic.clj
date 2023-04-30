(ns basic
  (:require [clojure.java.io :as io]
            [clojure.test :refer :all]
            [clj-mqtt-broker.core :refer :all]
            [clojure.tools.logging :as log])
  (:import (io.moquette.interception InterceptHandler)
           (io.moquette BrokerConstants)
           (io.netty.handler.codec.mqtt MqttQoS)
           (com.dkdhub.mqtt_broker AdvancedBroker SimpleBroker)
           (java.io File)
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

(defrecord TraceHandlers [id]
  InterceptHandler
  (onPublish [_ msg]
    (log/info "got a message")
    (let [payload (-> msg .getPayload (.toString StandardCharsets/UTF_8))]
      (log/info "Received on topic: " + (.getTopicName msg) + " content: " + payload)))
  (onConnect [_ msg]
    (log/debug "MQTT: client " + (.getClientID msg) + " connected"))
  (onDisconnect [_ msg]
    (log/debug "MQTT: client " + (.getClientID msg) + " disconnected"))
  (onConnectionLost [_ msg]
    (log/debug "MQTT: client " + (.getClientID msg) + " disconnected (lost)"))
  (onSubscribe [_ msg]
    (log/debug "MQTT: subscribed " + (.getClientID msg) + " on " + (.getTopicFilter msg) + " with QoS=" + (.getRequestedQos msg)))
  (onUnsubscribe [_ msg]
    (log/debug "MQTT: unsubscribed " + (.getClientID msg) + " from " + (.getTopicFilter msg)))
  (onMessageAcknowledged [_ msg]
    (log/debug "MQTT: acknowledged " + (.getMsg msg)))
  (getID [_] id)
  (getInterceptedMessageTypes [_] InterceptHandler/ALL_MESSAGE_TYPES))

(def advanced-config (mqtt-config {:port-tcp 10883 :anonymous? true}))

(deftest check-advanced-constructs
  (testing "Checking advanced constructs")

  (log/info "--------- MQTT Advanced Broker empty loop ---------")
  (is (let [b (Broker. (AdvancedBroker. advanced-config))]
        (with-open [srv (open b (TraceHandlers. "3456"))]
          (Thread/sleep 2000)
          (send srv "FROM" "/TEMPERATURE" "TEST" 1 false)
          (Thread/sleep 2000))
        true))

  (log/info "--------- MQTT Advanced Broker check clients listing ---------")
  (is (let [b (Broker. (SimpleBroker. config-name))]
        (with-open [srv (open b (TraceHandlers. "3456"))]
          (nil? (clients srv)))))

  (is (let [b (Broker. (AdvancedBroker. advanced-config))
            clients (clients b)]
        (and ((complement nil?) clients)
             (sequential? clients)
             (empty? clients))))

  (is (let [b (Broker. (AdvancedBroker. advanced-config))]
        (with-open [srv (open b (TraceHandlers. "3456"))]
          (sequential? (clients srv))))))

(deftest check-advanced-disconnects
  (testing "Checking advanced client for disconnects")
  (log/info "--------- MQTT Advanced Broker check disconnect ---------")
  (is (let [b (Broker. (AdvancedBroker. advanced-config))
            results (atom [])]
        (start b (TraceHandlers. "3456"))

        (println "======> MQTT Advanced Broker wait .........")
        (Thread/sleep 10000)
        (println "======> MQTT Clients:" (clients b))
        (println "======> MQTT Advanced Broker disconnecting .........")

        (doseq [client (clients b)
                :let [client-id (:id client)]
                :when client-id]
          (swap! results conj (disconnect b client-id)))

        (println "======> MQTT Advanced Broker goes down .........")
        (println "======> MQTT Clients:" (clients b))
        (Thread/sleep 1000)

        (stop b)
        (println "======> MQTT Results:" @results)
        (or (empty? @results)
            (every? true? @results)))))

(deftest check-multimethod-create
  (testing "Checking multimethod constructor")

  (is (nil? (clients (create-broker config-name))))
  (is (sequential? (clients (create-broker advanced-config)))))

(deftest check-persistence
  (testing "Checking H2 persistence")

  (is (let [config (mqtt-config {:port-tcp         10883
                                 :anonymous?       true
                                 :persistence-type :h2})
            b (create-broker config)
            data-name (get config BrokerConstants/DATA_PATH_PROPERTY_NAME)
            data-dir (str (System/getProperty "user.dir") (File/separator) data-name)
            h2-path (str data-dir (File/separator)
                         BrokerConstants/DEFAULT_MOQUETTE_STORE_H2_DB_FILENAME)]
        (println "======> MQTT data path:" data-dir)
        (println "======> MQTT persistence:" h2-path)
        (start b (TraceHandlers. "3456"))
        (Thread/sleep 10000)
        (stop b)
        (and (.isDirectory (io/file data-dir))
             (.exists (io/file h2-path))))))
