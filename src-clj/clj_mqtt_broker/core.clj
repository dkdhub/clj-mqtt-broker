(ns clj-mqtt-broker.core
  (:gen-class)
  (:import (io.moquette.interception InterceptHandler)
           (java.nio.charset StandardCharsets)))

(defrecord BasicHandler [id]
  InterceptHandler
  (onPublish [_ msg]
    (let [payload (-> msg .getPayload (.toString StandardCharsets/UTF_8))]
      (println "Received on topic: " + (.getTopicName msg) + " content: " + payload)))
  (getID [_] id)
  (getInterceptedMessageTypes [_] InterceptHandler/ALL_MESSAGE_TYPES))
