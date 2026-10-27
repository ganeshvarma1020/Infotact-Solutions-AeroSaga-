package com.infotact.aerosaga.persistence;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.infotact.aerosaga.workflow.MissionEvent;
import com.infotact.aerosaga.workflow.MissionJournal;
import com.infotact.aerosaga.workflow.MissionResult;

/** Stores mission history in the database. Called from activities, never from workflow code. */
@Component
public class JpaMissionJournal implements MissionJournal {

    private static final Logger log = LoggerFactory.getLogger(JpaMissionJournal.class);

    private final MissionRepository missions;
    private final MissionEventRepository events;

    public JpaMissionJournal(MissionRepository missions, MissionEventRepository events) {
        this.missions = missions;
        this.events = events;
    }

    @Override
    @Transactional
    public void event(MissionEvent e) {
        Instant at = Instant.ofEpochMilli(e.atEpochMillis());
        missions.findById(e.missionId()).ifPresentOrElse(m -> {
            m.update(e.phase().name(), e.droneId(), at);
            events.save(new MissionEventRecord(e.missionId(), e.phase().name(), e.message(), at));
        }, () -> log.warn("Event for unknown mission {} ignored", e.missionId()));
    }

    @Override
    @Transactional
    public void outcome(MissionResult r) {
        missions.findById(r.missionId()).ifPresentOrElse(
                m -> m.finish(r.outcome().name(), r.droneId(), r.failureCode(), r.failureReason(), Instant.now()),
                () -> log.warn("Outcome for unknown mission {} ignored", r.missionId()));
    }
}
