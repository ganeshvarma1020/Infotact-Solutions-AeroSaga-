package com.infotact.aerosaga.workflow;

import com.infotact.aerosaga.domain.MissionRequest;

/**
 * Workflow input. One object instead of many parameters, so new fields can be added later
 * without breaking running workflows.
 *
 * @param timeScale how simulated flight time maps to real waiting time.
 *                  1.0 = real time, 0.05 = 20x faster, 0 = no waiting at all (tests).
 */
public record MissionInput(MissionRequest request, double timeScale) {
}
