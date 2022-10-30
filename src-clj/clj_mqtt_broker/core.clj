(ns clj-mqtt-broker.core
  (:gen-class)
  (:import (io.moquette.interception InterceptHandler)
           (io.netty.handler.codec.mqtt MqttQoS)))

(defn ->QoS [qos]
  (get {:atleast MqttQoS/AT_LEAST_ONCE
        :atmost  MqttQoS/AT_MOST_ONCE
        :exactly MqttQoS/EXACTLY_ONCE} qos
       MqttQoS/EXACTLY_ONCE))

(defprotocol IBroker
  (start [o ^InterceptHandler handlers])
  (stop [o])
  (send [o from to data qos retain?]))

(deftype Broker [instance]
  IBroker
  (start [_ handlers] (.start instance handlers))
  (stop [_] (.stop instance))
  (send [_ from to data qos retain?]
    (.send instance
           (if (string? from) from (name from))
           (if (string? to) to (name to))
           (if (bytes? data) data (.getBytes data))
           (if (keyword? qos) (->QoS qos) (MqttQoS/valueOf (int qos)))
           (if (boolean? retain?) retain? false))))
