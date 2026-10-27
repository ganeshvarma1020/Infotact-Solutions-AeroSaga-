package com.infotact.aerosaga.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** All "aerosaga.*" settings from application.yml, typed. */
@ConfigurationProperties("aerosaga")
public record AeroSagaProperties(Temporal temporal, Simulation simulation, Airspace airspace) {

    /**
     * @param target        host:port of the Temporal server
     * @param namespace     Temporal namespace (like a database name)
     * @param taskQueue     queue the worker listens on; client and worker must use the same name
     * @param connect       false only in tests that supply their own in-memory Temporal
     * @param workerEnabled false to run the API without processing missions
     */
    public record Temporal(String target, String namespace, String taskQueue, boolean connect, boolean workerEnabled) {
    }

    /** @param timeScale 1.0 = real flight time, 0.05 = 20x faster, 0 = instant */
    public record Simulation(double timeScale) {
    }

    /** @param capacity max missions airborne at the same time */
    public record Airspace(int capacity) {
    }
}
