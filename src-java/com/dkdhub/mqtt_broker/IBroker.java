package com.dkdhub.mqtt_broker;

import io.moquette.interception.InterceptHandler;
import io.netty.handler.codec.mqtt.MqttQoS;

import java.io.IOException;

public interface IBroker {
    void start(InterceptHandler handler) throws IOException;

    void stop();

    void send(String from, String topic, byte[] data, MqttQoS qos, Boolean retained);
}
