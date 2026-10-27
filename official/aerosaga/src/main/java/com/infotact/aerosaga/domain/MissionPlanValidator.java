package com.infotact.aerosaga.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks a mission request against {@link OperatingLimits} before anything is reserved.
 * Pure logic: no I/O, no clock, no randomness. Same input always gives same answer.
 */
public class MissionPlanValidator {

    /** Waypoints closer than this to the previous point are treated as duplicates. */
    static final double MIN_LEG_KM = 0.05;

    private final OperatingLimits limits;

    public MissionPlanValidator(OperatingLimits limits) {
        this.limits = limits;
    }

    public ValidationResult validate(MissionRequest req) {
        List<String> errors = new ArrayList<>();
        if (req == null) {
            return new ValidationResult(List.of("Mission request is missing"));
        }
        if (isBlank(req.missionId())) {
            errors.add("missionId is required");
        }
        if (isBlank(req.operator())) {
            errors.add("operator is required");
        }
        if (req.base() == null) {
            errors.add("base position is required");
        }
        if (req.waypoints().isEmpty()) {
            errors.add("at least 1 waypoint is required");
        }
        if (req.waypoints().size() > limits.maxWaypoints()) {
            errors.add("too many waypoints: " + req.waypoints().size() + " (max " + limits.maxWaypoints() + ")");
        }
        if (req.payloadKg() < 0 || Double.isNaN(req.payloadKg())) {
            errors.add("payloadKg cannot be negative");
        }
        if (req.payloadKg() > limits.maxPayloadKg()) {
            errors.add(String.format("payload %.2f kg exceeds fleet max %.2f kg", req.payloadKg(), limits.maxPayloadKg()));
        }
        if (req.base() != null) {
            checkPoint("base", req.base(), errors);
        }
        for (Waypoint wp : req.waypoints()) {
            checkPoint("waypoint '" + wp.name() + "'", wp.position(), errors);
        }
        if (req.base() != null && !req.waypoints().isEmpty()) {
            RoutePlan route = req.route();
            for (RoutePlan.Leg leg : route.outboundLegs()) {
                if (leg.distanceKm() < MIN_LEG_KM) {
                    errors.add("waypoint '" + leg.toName() + "' duplicates the previous point");
                }
            }
            for (RoutePlan.Leg leg : route.legs()) {
                for (NoFlyZone zone : limits.noFlyZones()) {
                    if (!zone.contains(leg.from()) && !zone.contains(leg.to()) && zone.crossedBy(leg.from(), leg.to())) {
                        errors.add("leg " + leg.fromName() + " -> " + leg.toName()
                                + " crosses no-fly zone '" + zone.name() + "'");
                    }
                }
            }
            if (route.totalKm() > limits.maxRouteKm()) {
                errors.add(String.format("route %.2f km exceeds max %.2f km", route.totalKm(), limits.maxRouteKm()));
            }
        }
        return new ValidationResult(errors);
    }

    private void checkPoint(String label, GeoPoint p, List<String> errors) {
        double fromCenter = limits.operatingCenter().distanceKmTo(p);
        if (fromCenter > limits.operatingRadiusKm()) {
            errors.add(String.format("%s is %.2f km from the operating center (max %.2f km)",
                    label, fromCenter, limits.operatingRadiusKm()));
        }
        for (NoFlyZone zone : limits.noFlyZones()) {
            if (zone.contains(p)) {
                errors.add(label + " is inside no-fly zone '" + zone.name() + "'");
            }
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
