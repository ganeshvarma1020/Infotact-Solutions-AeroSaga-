package com.infotact.aerosaga.config;

import com.infotact.aerosaga.model.Drone;
import com.infotact.aerosaga.model.User;
import com.infotact.aerosaga.repository.DroneRepository;
import com.infotact.aerosaga.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final DroneRepository droneRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           DroneRepository droneRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.droneRepository = droneRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedUsers();
        seedDrones();
    }

    private void seedUsers() {
        if (userRepository.count() == 0) {
            log.info("Seeding default users...");

            User admin = new User("admin", passwordEncoder.encode("admin123"), User.Role.ADMIN);
            User operator = new User("operator", passwordEncoder.encode("operator123"), User.Role.OPERATOR);

            userRepository.saveAll(Arrays.asList(admin, operator));
            log.info("Default users seeded: admin / admin123, operator / operator123");
        }
    }

    private void seedDrones() {
        if (droneRepository.count() == 0) {
            log.info("Seeding initial drone fleet...");

            Drone d1 = new Drone("Alpha-1", "DJI Matrice 350 RTK", 100.0, Drone.Status.AVAILABLE, 12.9715987, 77.5945627);
            Drone d2 = new Drone("Beta-2", "Skydio X2D Enterprise", 88.0, Drone.Status.AVAILABLE, 12.9750000, 77.5980000);
            Drone d3 = new Drone("Falcon-X", "Autel EVO Max 4T", 94.0, Drone.Status.AVAILABLE, 12.9800000, 77.6050000);
            Drone d4 = new Drone("SkyRanger-4", "Teledyne FLIR R80D", 42.0, Drone.Status.MAINTENANCE, 12.9650000, 77.5850000);

            droneRepository.saveAll(Arrays.asList(d1, d2, d3, d4));
            log.info("Initial drone fleet seeded (4 drones).");
        }
    }
}
