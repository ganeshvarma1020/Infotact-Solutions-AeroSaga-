package com.infotact.aerosaga.state;

import java.util.List;
import java.util.Map;

import com.infotact.aerosaga.domain.NoFlyZone;
import com.infotact.aerosaga.fleet.DroneSnapshot;

/** Everything the simulator must remember across restarts. */
public record SimulationState(List<DroneSnapshot> drones, Map<String, String> slots, List<NoFlyZone> zones) {
}
