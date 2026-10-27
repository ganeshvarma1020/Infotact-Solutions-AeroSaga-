package com.infotact.aerosaga.state;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.infotact.aerosaga.domain.GeoPoint;
import com.infotact.aerosaga.domain.NoFlyZone;
import com.infotact.aerosaga.fleet.DroneSnapshot;
import com.infotact.aerosaga.fleet.DroneStatus;

/**
 * Turns simulator state into plain text and back. One line per item:
 *   D|droneId|model|status|battery|payload|lat|lon|missionId
 *   S|missionId|slotId
 *   Z|name|lat|lon|radiusKm
 * Text fields are URL-encoded so a '|' or newline inside a name cannot break the format.
 * Plain text keeps this independent of any JSON library version.
 */
public final class SimulationStateCodec {

    static final String HEADER = "aerosaga-state v1";
    private static final String NONE = "-";

    private SimulationStateCodec() {
    }

    public static String encode(SimulationState s) {
        StringBuilder b = new StringBuilder(HEADER).append('\n');
        for (DroneSnapshot d : s.drones()) {
            b.append(String.join("|", "D", enc(d.droneId()), enc(d.model()), d.status().name(),
                    Double.toString(d.batteryPct()), Double.toString(d.payloadKg()),
                    Double.toString(d.position().lat()), Double.toString(d.position().lon()),
                    d.missionId() == null ? NONE : enc(d.missionId()))).append('\n');
        }
        for (Map.Entry<String, String> e : s.slots().entrySet()) {
            b.append(String.join("|", "S", enc(e.getKey()), enc(e.getValue()))).append('\n');
        }
        for (NoFlyZone z : s.zones()) {
            b.append(String.join("|", "Z", enc(z.name()), Double.toString(z.center().lat()),
                    Double.toString(z.center().lon()), Double.toString(z.radiusKm()))).append('\n');
        }
        return b.toString();
    }

    /** Returns null for empty input (nothing saved yet). Throws IllegalArgumentException on corrupt data. */
    public static SimulationState decode(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String[] lines = text.split("\n");
        if (!HEADER.equals(lines[0].trim())) {
            throw new IllegalArgumentException("Unknown state format: " + lines[0]);
        }
        List<DroneSnapshot> drones = new ArrayList<>();
        Map<String, String> slots = new LinkedHashMap<>();
        List<NoFlyZone> zones = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] f = line.split("\\|", -1);
            try {
                switch (f[0]) {
                    case "D" -> drones.add(new DroneSnapshot(dec(f[1]), dec(f[2]), DroneStatus.valueOf(f[3]),
                            Double.parseDouble(f[4]), Double.parseDouble(f[5]),
                            new GeoPoint(Double.parseDouble(f[6]), Double.parseDouble(f[7])),
                            NONE.equals(f[8]) ? null : dec(f[8])));
                    case "S" -> slots.put(dec(f[1]), dec(f[2]));
                    case "Z" -> zones.add(new NoFlyZone(dec(f[1]),
                            new GeoPoint(Double.parseDouble(f[2]), Double.parseDouble(f[3])), Double.parseDouble(f[4])));
                    default -> throw new IllegalArgumentException("Unknown line type");
                }
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("Corrupt state at line " + (i + 1) + ": " + line, e);
            }
        }
        return new SimulationState(drones, slots, zones);
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String dec(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }
}
