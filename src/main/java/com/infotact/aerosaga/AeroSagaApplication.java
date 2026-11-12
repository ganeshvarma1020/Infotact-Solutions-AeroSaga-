package com.infotact.aerosaga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AeroSagaApplication {

    private static final Logger logger = LoggerFactory.getLogger(AeroSagaApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(AeroSagaApplication.class, args);
        logger.info("==========================================================================");
        logger.info("🚀 AeroSaga: Temporal.io Autonomous Drone Mission Control is online!");
        logger.info("🌐 Mission Control Dashboard: http://localhost:8080/");
        logger.info("📚 Swagger OpenAPI UI:        http://localhost:8080/swagger-ui.html");
        logger.info("💾 H2 In-Memory Database:     http://localhost:8080/h2-console");
        logger.info("==========================================================================");
    }
}
