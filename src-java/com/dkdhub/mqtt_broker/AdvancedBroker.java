package com.dkdhub.mqtt_broker;

import io.moquette.broker.Server;
import io.moquette.broker.config.IConfig;
import io.moquette.broker.config.MemoryConfig;
import io.moquette.interception.InterceptHandler;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class AdvancedBroker implements IBroker {

    final Server m_server;
    final IConfig m_config;
    private static final Logger LOG = LoggerFactory.getLogger(AdvancedBroker.class);

    public AdvancedBroker(Properties config) {
        LOG.info("Constructing MQTT Broker with configuration {}", config);
        this.m_config = new MemoryConfig(config);
        this.m_server = new Server();
        LOG.info("MQTT Broker instance created");
    }

    public void start() throws IOException {
        m_server.startServer(m_config);
        LOG.info("MQTT Broker started with no interceptors enabled");
    }

    @Override
    public void start(InterceptHandler interceptor) throws IOException {
        m_server.startServer(m_config);
        m_server.addInterceptHandler(interceptor);
        LOG.info("MQTT Broker started with interceptor ID {}", interceptor.getID());
    }

    public void start(List<? extends InterceptHandler> interceptors) throws IOException {
        m_server.startServer(m_config, interceptors);
        LOG.info("MQTT Broker started with interceptor IDs {}",
                Arrays.toString(interceptors.stream().map(InterceptHandler::getID).toArray()));
    }

    @Override
    public void stop() {
        LOG.info("Stopping MQTT Broker");
        m_server.stopServer();
        LOG.info("MQTT Broker stopped");
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

        m_server.internalPublish(message, from);
    }

    public List<Map<String, ? extends Serializable>> clients() {
        try {
            return m_server.listConnectedClients().parallelStream()
                    .map(cl -> Map.of(
                            "id", cl.getClientID(),
                            "address", cl.getAddress(),
                            "port", cl.getPort()))
                    .collect(Collectors.toUnmodifiableList());
        } catch (IllegalStateException ise) {
            LOG.warn(ise.getLocalizedMessage());
            return null;
        } catch (NullPointerException npe) {
            // This actually should not happen. Since this is not the !initialized case,
            // let's suppose that server is actually running, so the error emitted from
            // one of the elements during the execution. Such a case is unclear, so consider
            // clients list is unavailable, so []
            LOG.error(npe.getLocalizedMessage());
            return List.of();
        }
    }

    public boolean disconnect(String client, boolean flush) {
        if (client == null) return false;

        LOG.debug("Will disconnect client {}, flush state: {}", client, flush);
        boolean res = flush ? m_server.disconnectAndPurgeClientState(client) : m_server.disconnectClient(client);
        LOG.debug("Disconnect result: {}", res);
        return res;
    }
}
