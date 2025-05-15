package app.api.diagnosticruntime.config;

import org.testcontainers.containers.MongoDBContainer;

public class MongoDBTestContainer {
    private static final MongoDBContainer MONGO_CONTAINER;

    static {
        MONGO_CONTAINER = new MongoDBContainer("mongo:8.0.4"); // You can use another version if needed
        MONGO_CONTAINER.withReuse(true).start();
        System.setProperty("spring.data.mongodb.uri", MONGO_CONTAINER.getReplicaSetUrl());
    }

    public static void start() {
        // Ensure the container starts only once
        if (!MONGO_CONTAINER.isRunning()) {
            MONGO_CONTAINER.withReuse(true).start();
        }
    }

    public static void stop() {
        MONGO_CONTAINER.stop();
    }
}

