package com.infotact.aerosaga.airspace;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.infotact.aerosaga.domain.MissionException;
import com.infotact.aerosaga.domain.MissionException.Code;
import com.infotact.aerosaga.domain.NoFlyZone;

/**
 * Simulated air traffic control.
 * 1. Limits how many missions may be airborne at once (slots).
 * 2. Holds temporary no-fly zones that can appear while a drone is flying.
 *
 * reserve/release are idempotent per missionId, for the same reason as DroneFleet.
 */
public class AirspaceRegistry {

    private final int capacity;
    private final Map<String, String> slotsByMission = new LinkedHashMap<>();
    private final List<NoFlyZone> temporaryZones = new ArrayList<>();

    public AirspaceRegistry(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be at least 1");
        }
        this.capacity = capacity;
    }

    /** Returns the slot id. Calling again for the same mission returns the same slot. */
    public synchronized String reserve(String missionId) {
        String existing = slotsByMission.get(missionId);
        if (existing != null) {
            return existing;
        }
        if (slotsByMission.size() >= capacity) {
            throw new MissionException(Code.AIRSPACE_FULL,
                    "Airspace full: " + slotsByMission.size() + "/" + capacity + " slots in use");
        }
        String slotId = "SLOT-" + missionId;
        slotsByMission.put(missionId, slotId);
        return slotId;
    }

    /** Undo of reserve. Safe to call when nothing is reserved. */
    public synchronized void release(String missionId) {
        slotsByMission.remove(missionId);
    }

    public synchronized boolean holds(String missionId) {
        return slotsByMission.containsKey(missionId);
    }

    public synchronized int inUse() {
        return slotsByMission.size();
    }

    public int capacity() {
        return capacity;
    }

    public synchronized void addTemporaryZone(NoFlyZone zone) {
        temporaryZones.removeIf(z -> z.name().equals(zone.name()));
        temporaryZones.add(zone);
    }

    public synchronized void clearTemporaryZone(String name) {
        temporaryZones.removeIf(z -> z.name().equals(name));
    }

    public synchronized List<NoFlyZone> temporaryZones() {
        return List.copyOf(temporaryZones);
    }

    /** missionId -> slotId, copy. */
    public synchronized Map<String, String> slots() {
        return new LinkedHashMap<>(slotsByMission);
    }

    /** Put reservations and temporary zones back into a saved state (after an app restart). */
    public synchronized void restore(Map<String, String> slots, List<NoFlyZone> zones) {
        slotsByMission.clear();
        slotsByMission.putAll(slots);
        temporaryZones.clear();
        temporaryZones.addAll(zones);
    }
}
