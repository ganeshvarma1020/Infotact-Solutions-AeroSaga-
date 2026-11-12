package com.infotact.aerosaga.controller;

import com.infotact.aerosaga.model.AuthRequest;
import com.infotact.aerosaga.model.AuthResponse;
import com.infotact.aerosaga.model.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication API", description = "Role-based authentication for Administrators and Drone Operators")
@CrossOrigin(origins = "*")
public class AuthController {

    @PostMapping("/login")
    @Operation(summary = "Authenticate user with username and password (ADMIN or USER)")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        if (request == null || request.getUsername() == null || request.getPassword() == null) {
            return ResponseEntity.badRequest().body(new AuthResponse(
                    false, null, null, null, null, "Username and password are required."));
        }

        String username = request.getUsername().trim().toLowerCase();
        String password = request.getPassword().trim();

        // Admin Credentials
        if (("admin".equals(username) || "director".equals(username)) && "admin123".equals(password)) {
            String token = "TOKEN-ADMIN-" + UUID.randomUUID();
            return ResponseEntity.ok(new AuthResponse(
                    true,
                    "admin",
                    UserRole.ADMIN.name(),
                    "Commander / Flight Director",
                    token,
                    "Welcome back, Administrator. Full mission control authorized."
            ));
        }

        // Operator / User Credentials
        if (("user".equals(username) || "operator".equals(username) || "pilot".equals(username)) && "user123".equals(password)) {
            String token = "TOKEN-USER-" + UUID.randomUUID();
            return ResponseEntity.ok(new AuthResponse(
                    true,
                    username,
                    UserRole.USER.name(),
                    "Flight Operator (Observer)",
                    token,
                    "Authenticated as Flight Operator. Telemetry and radar monitoring enabled."
            ));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponse(
                false,
                request.getUsername(),
                null,
                null,
                null,
                "Invalid credentials. Hint: admin/admin123 or user/user123"
        ));
    }

    @GetMapping("/demo-accounts")
    @Operation(summary = "Get list of available pre-configured demo credentials")
    public ResponseEntity<List<Map<String, String>>> getDemoAccounts() {
        return ResponseEntity.ok(List.of(
                Map.of(
                        "username", "admin",
                        "password", "admin123",
                        "role", "ADMIN",
                        "description", "Flight Director: Launch missions, dispatch in-flight signals, recharge & manage fleet"
                ),
                Map.of(
                        "username", "user",
                        "password", "user123",
                        "role", "USER",
                        "description", "Mission Observer: Real-time telemetry monitoring, radar tracking, flight reporting"
                )
        ));
    }
}
