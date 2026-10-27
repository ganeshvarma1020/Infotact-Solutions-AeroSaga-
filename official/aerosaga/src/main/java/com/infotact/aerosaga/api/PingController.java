package com.infotact.aerosaga.api;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Smallest possible endpoint. Proves the app starts and serves HTTP. */
@RestController
public class PingController {

    @GetMapping("/api/ping")
    public Map<String, Object> ping() {
        return Map.of(
                "service", "aerosaga",
                "status", "UP",
                "time", Instant.now().toString());
    }
}
