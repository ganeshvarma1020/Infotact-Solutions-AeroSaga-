package com.infotact.aerosaga.fleet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.infotact.aerosaga.domain.GeoPoint;
import com.infotact.aerosaga.domain.MissionException;
import com.infotact.aerosaga.domain.MissionException.Code;

/**
 * In-memory registry of simulated drones.
 *
 * Every state-changing method is idempotent for the same missionId.
 * Why: Temporal may run an activity again if the worker crashed after doing the work
 * but before reporting success. Running "assign drone for M-1" twice must not book two drones.
 *
 * All methods are synchronized: simple and correct for a single-process demo.
 */
public class DroneFleet {

    private static final class Drone {
        final DroneSpec spec;
        DroneStatus status = DroneStatus.AVAILABLE;
        double batteryPct = 100.0;
        double payloadKg = 0.0;
        GeoPoint position;
        String missionId;

        Drone(DroneSpec spec, GeoPoint home) {
            this.spec = spec;
            this.position = home;
        }

        DroneSnapshot snapshot() {
            return new DroneSnapshot(spec.droneId(), spec.model(), status, round1(batteryPct), payloadKg, position, missionId);
        }
    }

    private final Map<String, Drone> drones = new LinkedHashMap<>();
    private final GeoPoint home;

    public DroneFleet(GeoPoint home, List<DroneSpec> specs) {
        this.home = home;
        for (DroneSpec s : specs) {
            if (drones.put(s.droneId(), new Drone(s, home)) != null) {
                throw new IllegalArgumentException("Duplicate droneId " + s.droneId());
            }
        }
    }

    /** Three demo airframes with different trade-offs. */
    public static List<DroneSpec> demoSpecs() {
        return List.of(
                new DroneSpec("DR-01", "Garuda Lite", 2.0, 30.0, 50.0),
                new DroneSpec("DR-02", "Garuda Cargo", 5.0, 25.0, 40.0),
                new DroneSpec("DR-03", "Garuda Range", 3.0, 45.0, 60.0));
    }

    public synchronized List<DroneSnapshot> list() {
        return drones.values().stream().map(Drone::snapshot).toList();
    }

    public synchronized Optional<DroneSnapshot> find(String droneId) {
        return Optional.ofNullable(drones.get(droneId)).map(Drone::snapshot);
    }

    public synchronized DroneSpec spec(String droneId) {
        return get(droneId).spec;
    }

    /**
     * Book the best available drone for a route.
     * Picks the drone with the most battery to spare among those that can lift the payload
     * and fly the route while keeping the reserve.
     */
    public synchronized DroneSnapshot assign(String missionId, double payloadKg, double routeKm, double reservePct) {
        Optional<Drone> existing = drones.values().stream().filter(d -> missionId.equals(d.missionId)).findFirst();
        if (existing.isPresent()) {
            return existing.get().snapshot();
        }
        List<String> reasons = new ArrayList<>();
        Drone best = null;
        double bestSpare = Double.NEGATIVE_INFINITY;
        for (Drone d : drones.values()) {
            if (d.status != DroneStatus.AVAILABLE) {
                reasons.add(d.spec.droneId() + ": busy");
                continue;
            }
            if (payloadKg > d.spec.maxPayloadKg()) {
                reasons.add(d.spec.droneId() + ": payload too heavy");
                continue;
            }
            double needed = BatteryModel.percentFor(d.spec, payloadKg, routeKm);
            double spare = d.batteryPct - needed - reservePct;
            if (spare < 0) {
                reasons.add(String.format("%s: needs %.1f%% + %.0f%% reserve, has %.1f%%",
                        d.spec.droneId(), needed, reservePct, d.batteryPct));
                continue;
            }
            if (spare > bestSpare) {
                best = d;
                bestSpare = spare;
            }
        }
        if (best == null) {
            throw new MissionException(Code.NO_DRONE_AVAILABLE, "No drone can fly this mission: " + String.join("; ", reasons));
        }
        best.status = DroneStatus.ASSIGNED;
        best.missionId = missionId;
        return best.snapshot();
    }

    /** Which drone (if any) is working for this mission. */
    public synchronized Optional<String> droneFor(String missionId) {
        return drones.values().stream().filter(d -> missionId.equals(d.missionId))
                .map(d -> d.spec.droneId()).findFirst();
    }

    /** Release whatever drone this mission holds. No-op if it holds none. */
    public synchronized void releaseByMission(String missionId) {
        droneFor(missionId).ifPresent(id -> release(id, missionId));
    }

    /** Undo of assign. Safe to call many times, and never touches a drone owned by another mission. */
    public synchronized void release(String droneId, String missionId) {
        Drone d = get(droneId);
        if (missionId.equals(d.missionId)) {
            d.status = DroneStatus.AVAILABLE;
            d.missionId = null;
            d.payloadKg = 0.0;
            d.position = home;
        }
    }

    public synchronized void loadPayload(String droneId, String missionId, double kg) {
        Drone d = owned(droneId, missionId);
        d.payloadKg = kg;
    }

    public synchronized void unloadPayload(String droneId, String missionId) {
        Drone d = get(droneId);
        if (missionId.equals(d.missionId)) {
            d.payloadKg = 0.0;
        }
    }

    public synchronized void setStatus(String droneId, String missionId, DroneStatus status) {
        owned(droneId, missionId).status = status;
    }

    /** Apply the result of flying a leg. Returns remaining battery. */
    public synchronized double recordFlight(String droneId, String missionId, GeoPoint newPosition, double batteryUsedPct) {
        Drone d = owned(droneId, missionId);
        d.position = newPosition;
        d.batteryPct = Math.max(0, d.batteryPct - batteryUsedPct);
        return d.batteryPct;
    }

    /** Swap in a fresh battery. Only allowed on the ground. */
    public synchronized void recharge(String droneId) {
        Drone d = get(droneId);
        if (d.status == DroneStatus.IN_FLIGHT || d.status == DroneStatus.RETURNING) {
            throw new IllegalStateException(droneId + " is flying");
        }
        d.batteryPct = 100.0;
    }

    public GeoPoint home() {
        return home;
    }

    /**
     * Put drones back into a saved state (after an app restart).
     * Unknown drone ids in the saved data are ignored; drones missing from it keep their defaults.
     */
    public synchronized void restore(List<DroneSnapshot> saved) {
        for (DroneSnapshot s : saved) {
            Drone d = drones.get(s.droneId());
            if (d == null) {
                continue;
            }
            d.status = s.status();
            d.batteryPct = s.batteryPct();
            d.payloadKg = s.payloadKg();
            d.position = s.position();
            d.missionId = s.missionId();
        }
    }

    private Drone get(String droneId) {
        Drone d = drones.get(droneId);
        if (d == null) {
            throw new IllegalArgumentException("Unknown drone " + droneId);
        }
        return d;
    }

    private Drone owned(String droneId, String missionId) {
        Drone d = get(droneId);
        if (!missionId.equals(d.missionId)) {
            throw new IllegalStateException(droneId + " is not assigned to " + missionId);
        }
        return d;
    }

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
