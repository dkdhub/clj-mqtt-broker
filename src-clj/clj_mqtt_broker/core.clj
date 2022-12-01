(ns clj-mqtt-broker.core
  (:gen-class)
  (:import (io.moquette.interception InterceptHandler)
           (io.netty.handler.codec.mqtt MqttQoS)
           (com.dkdhub.mqtt_broker IBroker)))

(defn ->QoS [qos]
  (get {:atleast MqttQoS/AT_LEAST_ONCE
        :atmost  MqttQoS/AT_MOST_ONCE
        :exactly MqttQoS/EXACTLY_ONCE
        :failure MqttQoS/FAILURE} qos
       MqttQoS/EXACTLY_ONCE))

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
