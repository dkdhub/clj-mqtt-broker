(ns clj-mqtt-broker.core
  (:gen-class)
  (:refer-clojure :exclude [send])
  (:import (io.moquette.interception InterceptHandler)
           (io.moquette BrokerConstants)
           (io.netty.handler.codec.mqtt MqttQoS)
           (com.dkdhub.mqtt_broker IBroker)
           (java.util Hashtable Map Properties)))

(defn ->QoS [qos]
  (get {:atleast MqttQoS/AT_LEAST_ONCE
        :atmost  MqttQoS/AT_MOST_ONCE
        :exactly MqttQoS/EXACTLY_ONCE
        :failure MqttQoS/FAILURE} qos
       MqttQoS/EXACTLY_ONCE))

(def default-message-size-bytes BrokerConstants/DEFAULT_NETTY_MAX_BYTES_IN_MESSAGE)
(defn mqtt-config ^Properties
  ([] (mqtt-config nil))
  ([{:keys [port-tcp port-ws host passwords-path anonymous? ws-path
            persistence-type message-size]
     :or   {port-tcp "disabled" port-ws "disabled" host "0.0.0.0" passwords-path "" anonymous? false ws-path "/"}
     :as   props}]
   (doto (Properties.)
     (.putAll (Hashtable. ^Map (cond-> {BrokerConstants/PORT_PROPERTY_NAME            (str port-tcp)
                                        BrokerConstants/WEB_SOCKET_PORT_PROPERTY_NAME (str port-ws)
                                        BrokerConstants/HOST_PROPERTY_NAME            (str host)
                                        BrokerConstants/PASSWORD_FILE_PROPERTY_NAME   (str passwords-path)
                                        BrokerConstants/ALLOW_ANONYMOUS_PROPERTY_NAME (str (Boolean/parseBoolean (str anonymous?)))
                                        BrokerConstants/WEB_SOCKET_PATH_PROPERTY_NAME (str ws-path)
                                        BrokerConstants/ENABLE_TELEMETRY_NAME         (str false)}

                                       (and (:persistence-type props) (= :h2 persistence-type))
                                       (assoc BrokerConstants/PERSISTENT_STORE_PROPERTY_NAME BrokerConstants/DEFAULT_PERSISTENT_PATH)

                                       (:message-size props)
                                       (assoc BrokerConstants/NETTY_MAX_BYTES_PROPERTY_NAME (str message-size))))))))

(defprotocol CljBroker
  (start [o ^InterceptHandler handlers])
  (open [o ^InterceptHandler handlers])
  (stop [o])
  (close [o])
  (send [o from to data qos retain?]))

(deftype Broker [^IBroker instance]
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
    this))
