package com.infotact.aerosaga.persistence;

import java.sql.Timestamp;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.infotact.aerosaga.airspace.AirspaceRegistry;
import com.infotact.aerosaga.fleet.DroneFleet;
import com.infotact.aerosaga.state.SimulationState;
import com.infotact.aerosaga.state.SimulationStateCodec;

/**
 * Saves the simulated world (drones, slots, temporary zones) to the database and restores it on startup.
 *
 * Why: a real drone keeps flying if our app restarts. Our simulated drones must "keep existing" too,
 * otherwise a mission that Temporal resumes after a crash would find its drone gone.
 *
 * checkpoint() is called at the end of every activity that changes the world, BEFORE the activity
 * reports success to Temporal. So anything Temporal believes happened is already saved.
 */
@Component
public class SimulationStateStore implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(SimulationStateStore.class);

    private final JdbcTemplate jdbc;
    private final DroneFleet fleet;
    private final AirspaceRegistry airspace;

    public SimulationStateStore(JdbcTemplate jdbc, DroneFleet fleet, AirspaceRegistry airspace) {
        this.jdbc = jdbc;
        this.fleet = fleet;
        this.airspace = airspace;
    }

    /** Runs once at startup, after database migrations and before the Temporal worker starts. */
    @Override
    public void afterPropertiesSet() {
        String text = jdbc.queryForObject("select state_text from sim_state where id = 1", String.class);
        try {
            SimulationState saved = SimulationStateCodec.decode(text);
            if (saved == null) {
                log.info("No saved simulator state; starting with a fresh fleet");
                checkpoint();
                return;
            }
            fleet.restore(saved.drones());
            airspace.restore(saved.slots(), saved.zones());
            log.info("Restored simulator state: {} drones, {} slots in use, {} temporary zones",
                    saved.drones().size(), saved.slots().size(), saved.zones().size());
        } catch (IllegalArgumentException e) {
            log.error("Saved simulator state is corrupt, starting fresh: {}", e.getMessage());
            checkpoint();
        }
    }

    public synchronized void checkpoint() {
        // Read fleet and airspace one after the other (never holding both locks at once)
        SimulationState state = new SimulationState(fleet.list(), airspace.slots(), airspace.temporaryZones());
        jdbc.update("update sim_state set state_text = ?, updated_at = ? where id = 1",
                SimulationStateCodec.encode(state), Timestamp.from(Instant.now()));
    }
}
