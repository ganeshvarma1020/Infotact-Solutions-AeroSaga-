package com.infotact.aerosaga.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * A route broken into legs: base -> wp1 -> wp2 -> ... -> base.
 * The last leg is always the return to base.
 */
public record RoutePlan(List<Leg> legs) {

    /** One straight-line hop between two points. */
    public record Leg(int index, String fromName, GeoPoint from, String toName, GeoPoint to, double distanceKm) {
    }

    public static final String BASE_NAME = "BASE";

    public RoutePlan {
        legs = List.copyOf(legs);
    }

    public static RoutePlan of(GeoPoint base, List<Waypoint> waypoints) {
        if (base == null) {
            throw new IllegalArgumentException("Base position is required");
        }
        List<Leg> legs = new ArrayList<>();
        String prevName = BASE_NAME;
        GeoPoint prev = base;
        int i = 0;
        for (Waypoint wp : waypoints) {
            legs.add(new Leg(i++, prevName, prev, wp.name(), wp.position(), prev.distanceKmTo(wp.position())));
            prevName = wp.name();
            prev = wp.position();
        }
        if (!waypoints.isEmpty()) {
            legs.add(new Leg(i, prevName, prev, BASE_NAME, base, prev.distanceKmTo(base)));
        }
        return new RoutePlan(legs);
    }

    public double totalKm() {
        return legs.stream().mapToDouble(Leg::distanceKm).sum();
    }

    /** Legs up to and including the last waypoint (everything except the return leg). */
    public List<Leg> outboundLegs() {
        return legs.isEmpty() ? List.of() : legs.subList(0, legs.size() - 1);
    }

    public Leg returnLeg() {
        if (legs.isEmpty()) {
            throw new IllegalStateException("Empty route has no return leg");
        }
        return legs.get(legs.size() - 1);
    }
}
