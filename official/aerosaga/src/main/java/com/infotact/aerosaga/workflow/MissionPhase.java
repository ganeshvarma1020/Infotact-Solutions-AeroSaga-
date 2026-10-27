package com.infotact.aerosaga.workflow;

/** Where a mission is in its life. The last six values are final (the mission is over). */
public enum MissionPhase {
    SUBMITTED,
    VALIDATING,
    RESERVING_AIRSPACE,
    ASSIGNING_DRONE,
    LOADING_PAYLOAD,
    IN_FLIGHT,
    DELIVERING,
    RETURNING,
    COMPENSATING,

    /** Flew the route, delivered, landed, released everything. */
    COMPLETED,
    /** Something failed; every finished step was undone. */
    ROLLED_BACK,
    /** Operator pressed abort; every finished step was undone. */
    ABORTED,
    /** Plan broke a rule; nothing was reserved, so nothing to undo. */
    REJECTED,
    /** An undo step itself failed after all retries. Needs a human. */
    COMPENSATION_FAILED,
    /** Temporal was unreachable, so the workflow never started. */
    FAILED_TO_START;

    public boolean isFinal() {
        return ordinal() >= COMPLETED.ordinal();
    }
}
