package com.infotact.aerosaga.config;

import java.net.Socket;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.infotact.aerosaga.workflow.MissionActivities;
import com.infotact.aerosaga.workflow.MissionWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

/**
 * Two jobs:
 * 1. Connection: a client that talks to the Temporal server (start missions, send signals, run queries).
 * 2. Worker: a background loop that picks up mission tasks from the queue and runs our workflow
 *    and activity code. In this app the API and the worker live in the same process.
 * If no external Temporal server is reachable at startup, an in-memory workflow environment is automatically started.
 */
@Configuration
public class TemporalConfig {

    private static final Logger log = LoggerFactory.getLogger(TemporalConfig.class);
    private TestWorkflowEnvironment inMemoryEnv;

    private boolean isPortReachable(String target) {
        try {
            String[] parts = target.split(":");
            String host = parts[0];
            int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 7233;
            try (Socket socket = new Socket(host, port)) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    @Bean
    @ConditionalOnProperty(prefix = "aerosaga.temporal", name = "connect", havingValue = "true", matchIfMissing = true)
    WorkflowClient workflowClient(AeroSagaProperties props) {
        String target = props.temporal().target();
        if (isPortReachable(target)) {
            log.info("Connecting to external Temporal server at {}", target);
            WorkflowServiceStubs stubs = WorkflowServiceStubs.newServiceStubs(WorkflowServiceStubsOptions.newBuilder()
                    .setTarget(target)
                    .build());
            return WorkflowClient.newInstance(stubs, WorkflowClientOptions.newBuilder()
                    .setNamespace(props.temporal().namespace())
                    .build());
        } else {
            log.info("External Temporal server at {} is not reachable. Starting in-memory Temporal workflow environment...", target);
            inMemoryEnv = TestWorkflowEnvironment.newInstance();
            return inMemoryEnv.getWorkflowClient();
        }
    }

    @Bean
    @ConditionalOnProperty(prefix = "aerosaga.temporal", name = "worker-enabled", havingValue = "true", matchIfMissing = true)
    WorkerFactory workerFactory(WorkflowClient client, MissionActivities activities, AeroSagaProperties props) {
        WorkerFactory factory;
        if (inMemoryEnv != null) {
            factory = inMemoryEnv.getWorkerFactory();
        } else {
            factory = WorkerFactory.newInstance(client);
        }
        Worker worker = factory.newWorker(props.temporal().taskQueue());
        worker.registerWorkflowImplementationTypes(MissionWorkflowImpl.class);
        worker.registerActivitiesImplementations(activities);
        return factory;
    }

    /** Starts the worker after the app is ready, and stops it cleanly on shutdown. */
    @Bean
    @ConditionalOnProperty(prefix = "aerosaga.temporal", name = "worker-enabled", havingValue = "true", matchIfMissing = true)
    SmartLifecycle temporalWorkerLifecycle(WorkerFactory factory, AeroSagaProperties props) {
        return new SmartLifecycle() {
            private volatile boolean running;

            @Override
            public void start() {
                if (inMemoryEnv != null) {
                    inMemoryEnv.start();
                } else {
                    factory.start();
                }
                running = true;
                log.info("Temporal worker polling task queue '{}'", props.temporal().taskQueue());
            }

            @Override
            public void stop() {
                if (inMemoryEnv != null) {
                    inMemoryEnv.close();
                } else {
                    factory.shutdown();
                    try {
                        factory.awaitTermination(10, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        log.warn("Worker did not stop cleanly: {}", e.getMessage());
                    }
                }
                running = false;
            }

            @Override
            public boolean isRunning() {
                return running;
            }
        };
    }
}
