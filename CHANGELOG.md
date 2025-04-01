# 2025-03-31

Successfully upgraded the Moquette MQTT broker dependency from version 0.17 to version 0.18.0, which required:

- Changing the dependency from the Maven Central artifact io.moquette/moquette-broker to the JitPack artifact com.github.moquette-io/moquette
- Adding the JitPack repository to the project configuration with :repositories [["jitpack" "https://jitpack.io"]]

This update brings in the latest Moquette broker version that includes MQTT5 protocol support (with some limitations) and updated dependencies like Netty (now using 4.1.116.Final) and H2 database.
