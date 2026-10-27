package com.infotact.aerosaga;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Entry point. Starts the web server, database migrations and the Temporal worker. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class AeroSagaApplication {

    public static void main(String[] args) {
        SpringApplication.run(AeroSagaApplication.class, args);
    }
}
