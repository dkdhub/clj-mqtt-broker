package com.dkdhub.mqtt_broker;

import io.moquette.broker.Server;
import io.moquette.broker.config.ClasspathResourceLoader;
import io.moquette.broker.config.IConfig;
import io.moquette.broker.config.IResourceLoader;
import io.moquette.broker.config.ResourceLoaderConfig;
import io.moquette.interception.InterceptHandler;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class SimpleBroker {
    final Server mqttBroker = new Server();
    final IConfig classPathConfig;

    public SimpleBroker() {
        IResourceLoader classpathLoader = new ClasspathResourceLoader();
        classPathConfig = new ResourceLoaderConfig(classpathLoader, "moquette.conf");
    }

    public void start(InterceptHandler handler) throws IOException {
//        List<? extends InterceptHandler> userHandlers = Collections.singletonList(handler);

        mqttBroker.startServer(classPathConfig);
        mqttBroker.addInterceptHandler(handler);
        System.out.println("Broker started");
    }

    public void stop() {
        System.out.println("Stopping broker");
        mqttBroker.stopServer();
        System.out.println("Broker stopped");
    }

    public void send(String from, String topic, byte[] data, MqttQoS qos, Boolean retained) {
        MqttPublishMessage message = MqttMessageBuilders.publish()
                .topicName(topic)
                .retained(retained)
                .qos(qos)
                .payload(Unpooled.copiedBuffer(data))
                .build();

        System.out.println("Sending message " + new String(data) + " from " + from + " to " + topic + " with " + qos + " and " + retained);

        mqttBroker.internalPublish(message, from);
    }
}
