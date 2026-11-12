package com.infotact.aerosaga.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Signal payload for mid-air dynamic flight path redirection.
 */
public class RerouteRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<Waypoint> newWaypoints = new ArrayList<>();
    private String reason;

    public RerouteRequest() {
    }

    public RerouteRequest(List<Waypoint> newWaypoints, String reason) {
        this.newWaypoints = newWaypoints;
        this.reason = reason;
    }

    public List<Waypoint> getNewWaypoints() {
        return newWaypoints;
    }

    public void setNewWaypoints(List<Waypoint> newWaypoints) {
        this.newWaypoints = newWaypoints;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
