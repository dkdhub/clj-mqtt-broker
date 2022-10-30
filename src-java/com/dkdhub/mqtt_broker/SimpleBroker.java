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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SimpleBroker implements IBroker {
    final Server mqttBroker = new Server();
    final IConfig classPathConfig;

    private static final Logger LOG = LoggerFactory.getLogger(SimpleBroker.class);

    public SimpleBroker(final String configName) {
        IResourceLoader classpathLoader = new ClasspathResourceLoader();
        classPathConfig = new ResourceLoaderConfig(classpathLoader, configName);
    }

    @Override
    public void start(InterceptHandler handler) throws IOException {
//        List<? extends InterceptHandler> userHandlers = Collections.singletonList(handler);

        mqttBroker.startServer(classPathConfig);
        mqttBroker.addInterceptHandler(handler);
        LOG.info("SimpleBroker started. Class Path config = {}.", classPathConfig);
    }

    @Override
    public void stop() {
        LOG.info("Stopping SimpleBroker");
        mqttBroker.stopServer();
        LOG.info("SimpleBroker stopped");
    }

    @Override
    public void send(String from, String topic, byte[] data, MqttQoS qos, Boolean retained) {
        MqttPublishMessage message = MqttMessageBuilders.publish()
                .topicName(topic)
                .retained(retained)
                .qos(qos)
                .payload(Unpooled.copiedBuffer(data))
                .build();

        LOG.debug("Sending message {} from {} to {} with {} and {}", new String(data), from, topic, qos, retained);

        mqttBroker.internalPublish(message, from);
    }
}
