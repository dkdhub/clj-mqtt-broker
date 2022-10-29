package com.dkdhub.mqtt_broker;

import io.moquette.broker.Server;
import io.moquette.broker.config.ClasspathResourceLoader;
import io.moquette.broker.config.IConfig;
import io.moquette.broker.config.IResourceLoader;
import io.moquette.broker.config.ResourceLoaderConfig;
import io.moquette.interception.InterceptHandler;

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
        List<? extends InterceptHandler> userHandlers = Collections.singletonList(handler);

        mqttBroker.startServer(classPathConfig, userHandlers);
        System.out.println("Broker started");
    }

    public void stop() {
        System.out.println("Stopping broker");
        mqttBroker.stopServer();
        System.out.println("Broker stopped");
    }
}
