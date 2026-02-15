(ns clj-mqtt-broker.core
  (:gen-class)
  (:refer-clojure :exclude [send])
  (:require [clojure.walk])
  (:import (io.moquette.interception InterceptHandler)
           (io.moquette BrokerConstants)
           (io.moquette.broker.config IConfig)
           (io.netty.handler.codec.mqtt MqttQoS)
           (com.dkdhub.mqtt_broker AdvancedBroker IBroker SimpleBroker)
           (java.util Properties)))

(defn ->QoS [qos]
  (get {:atleast MqttQoS/AT_LEAST_ONCE
        :atmost  MqttQoS/AT_MOST_ONCE
        :exactly MqttQoS/EXACTLY_ONCE
        :failure MqttQoS/FAILURE} qos
       MqttQoS/EXACTLY_ONCE))

(def default-message-size-bytes IConfig/DEFAULT_NETTY_MAX_BYTES_IN_MESSAGE)
(defn mqtt-config ^Properties
  ([] (mqtt-config nil))
  ([{:keys [port-tcp port-ws host passwords-path anonymous? ws-path
            persistence-type data-path message-size
            allow-zero-byte-client-id? buffer-flush-ms netty-native?]
     :or   {port-tcp  "disabled" port-ws "disabled" host "0.0.0.0" passwords-path "" anonymous? false ws-path "/"
            data-path "mqtt-data"}
     :as   props}]
   (let [m (cond-> {IConfig/PORT_PROPERTY_NAME                (str port-tcp)
                    IConfig/WEB_SOCKET_PORT_PROPERTY_NAME     (str port-ws)
                    IConfig/HOST_PROPERTY_NAME                (str host)
                    IConfig/PASSWORD_FILE_PROPERTY_NAME       (str passwords-path)
                    IConfig/ALLOW_ANONYMOUS_PROPERTY_NAME     (str (Boolean/parseBoolean (str anonymous?)))
                    IConfig/WEB_SOCKET_PATH_PROPERTY_NAME     (str ws-path)
                    IConfig/ENABLE_TELEMETRY_NAME             (str false)
                    IConfig/PERSISTENCE_ENABLED_PROPERTY_NAME (str false)}

                   (and (:persistence-type props) (#{:h2 :segmented} persistence-type))
                   (assoc IConfig/DATA_PATH_PROPERTY_NAME (str data-path)
                          IConfig/PERSISTENCE_ENABLED_PROPERTY_NAME (str true)
                          IConfig/PERSISTENT_QUEUE_TYPE_PROPERTY_NAME (name persistence-type))

                   (:message-size props)
                   (assoc IConfig/NETTY_MAX_BYTES_PROPERTY_NAME (str message-size))

                   (some? (:allow-zero-byte-client-id? props))
                   (assoc BrokerConstants/ALLOW_ZERO_BYTE_CLIENT_ID_PROPERTY_NAME
                          (str (boolean allow-zero-byte-client-id?)))

                   (:buffer-flush-ms props)
                   (assoc IConfig/BUFFER_FLUSH_MS_PROPERTY_NAME (str buffer-flush-ms))

                   (some? (:netty-native? props))
                   (assoc BrokerConstants/NETTY_NATIVE_PROPERTY_NAME
                          (str (boolean netty-native?))))]
     (doto (Properties.) (.putAll ^java.util.Map m)))))

(defprotocol CljBroker
  (start [o ^InterceptHandler handlers])
  (open [o ^InterceptHandler handlers])
  (stop [o])
  (close [o])
  (send [o from to data qos retain?])
  (clients [o])
  (disconnect
    [o ^String client]
    [o ^String client ^Boolean flush?]))

(deftype Broker [^IBroker instance]
  java.io.Closeable

  CljBroker
  (start [this handlers] (.start ^IBroker instance handlers) this)
  (open [this handlers] (.start ^IBroker instance handlers) this)
  (stop [this] (.stop ^IBroker instance) this)
  (close [this] (.stop ^IBroker instance) this)
  (send [this from to data qos retain?]
    (.send ^IBroker instance
           (if (string? from) from (name from))
           (if (string? to) to (name to))
           (if (bytes? data) data (.getBytes ^String data))
           (if (keyword? qos) (->QoS qos) (MqttQoS/valueOf (int qos)))
           (if (boolean? retain?) retain? false))
    this)
  (clients [_]
    (if (instance? AdvancedBroker instance)
      (map #(->> % (into {}) clojure.walk/keywordize-keys) (.clients ^AdvancedBroker instance))
      nil))
  (disconnect [this client] (disconnect this client false))
  (disconnect [_ client flush?]
    (if (instance? AdvancedBroker instance)
      (.disconnect ^AdvancedBroker instance client (boolean flush?))
      false)))

(defmulti create-broker (fn [x] (class x)))

(defmethod create-broker String [config]
  (Broker. (SimpleBroker. config)))

(defmethod create-broker Properties [config]
  (Broker. (AdvancedBroker. config)))
