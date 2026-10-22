package com.infotact.aerosaga.config;

import com.infotact.aerosaga.activity.MissionActivities;
import com.infotact.aerosaga.workflow.DroneMissionWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemporalConfig {

    private static final Logger log = LoggerFactory.getLogger(TemporalConfig.class);

    @Value("${temporal.target:127.0.0.1:7233}")
    private String target;

    @Value("${temporal.namespace:default}")
    private String namespace;

    @Value("${temporal.task-queue:AeroSagaMissionQueue}")
    private String taskQueue;

    @Value("${temporal.use-embedded-test-server:true}")
    private boolean useEmbeddedTestServer;

    private TestWorkflowEnvironment testEnv;
    private WorkerFactory workerFactory;

    @Bean
    public WorkflowClient workflowClient(MissionActivities missionActivities) {
        if (useEmbeddedTestServer) {
            log.info("Initializing in-memory Temporal TestWorkflowEnvironment...");
            testEnv = TestWorkflowEnvironment.newInstance();
            Worker worker = testEnv.newWorker(taskQueue);

            worker.registerWorkflowImplementationTypes(DroneMissionWorkflowImpl.class);
            worker.registerActivitiesImplementations(missionActivities);

            testEnv.start();
            log.info("In-memory Temporal workflow engine running on task queue: {}", taskQueue);
            return testEnv.getWorkflowClient();
        } else {
            log.info("Connecting to external Temporal server at {}...", target);
            WorkflowServiceStubs service = WorkflowServiceStubs.newServiceStubs(
                    WorkflowServiceStubsOptions.newBuilder().setTarget(target).build()
            );
            WorkflowClient client = WorkflowClient.newInstance(service,
                    WorkflowClientOptions.newBuilder().setNamespace(namespace).build());

            workerFactory = WorkerFactory.newInstance(client);
            Worker worker = workerFactory.newWorker(taskQueue);
            worker.registerWorkflowImplementationTypes(DroneMissionWorkflowImpl.class);
            worker.registerActivitiesImplementations(missionActivities);

            workerFactory.start();
            log.info("External Temporal worker listening on task queue: {}", taskQueue);
            return client;
        }
    }

    public String getTaskQueue() {
        return taskQueue;
    }

    @PreDestroy
    public void cleanup() {
        log.info("Stopping Temporal workflow engine...");
        if (testEnv != null) {
            testEnv.close();
        }
        if (workerFactory != null) {
            workerFactory.shutdown();
        }
    }
}
