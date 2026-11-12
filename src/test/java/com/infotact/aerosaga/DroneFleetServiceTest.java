package com.infotact.aerosaga;

import com.infotact.aerosaga.model.Drone;
import com.infotact.aerosaga.model.DroneStatus;
import com.infotact.aerosaga.repository.DroneRepository;
import com.infotact.aerosaga.service.DroneFleetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DroneFleetServiceTest {

    @Autowired
    private DroneFleetService droneFleetService;

    @Autowired
    private DroneRepository droneRepository;

    @BeforeEach
    void setup() {
        droneFleetService.initFleet();
    }

    @Test
    @DisplayName("Verify default fleet is registered with 3 operational drones")
    void testFleetInitialization() {
        List<Drone> drones = droneFleetService.getAllDrones();
        assertNotNull(drones);
        assertTrue(drones.size() >= 3, "Fleet should contain at least 3 initialized drones");

        Drone alpha = droneFleetService.getDroneById("DRONE-ALPHA-01");
        assertNotNull(alpha);
        assertEquals("AeroSky Valkyrie V4", alpha.getModel());
    }

    @Test
    @DisplayName("Verify dynamic drone registration")
    void testRegisterDrone() {
        Drone newDrone = new Drone("DRONE-DELTA-99", "Interceptor Stealth VTOL", 6.0, 12.9716, 80.2446);
        Drone registered = droneFleetService.registerDrone(newDrone);

        assertNotNull(registered);
        assertEquals("DRONE-DELTA-99", registered.getId());
        assertEquals(DroneStatus.IDLE, registered.getStatus());
        assertEquals(100.0, registered.getBatteryPercentage());
    }

    @Test
    @DisplayName("Verify drone battery recharge clears maintenance and restores 100%")
    void testRechargeDrone() {
        Drone alpha = droneFleetService.getDroneById("DRONE-ALPHA-01");
        alpha.setBatteryPercentage(12.5);
        alpha.setStatus(DroneStatus.MAINTENANCE_REQUIRED);
        droneRepository.save(alpha);

        Drone recharged = droneFleetService.rechargeDrone("DRONE-ALPHA-01");
        assertEquals(100.0, recharged.getBatteryPercentage());
        assertEquals(DroneStatus.IDLE, recharged.getStatus());
    }
}
