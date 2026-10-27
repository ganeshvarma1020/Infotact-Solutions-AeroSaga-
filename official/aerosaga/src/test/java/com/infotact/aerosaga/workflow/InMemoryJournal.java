package com.infotact.aerosaga.workflow;

import java.util.ArrayList;
import java.util.List;

/** Test journal: keeps everything in lists so tests can inspect the order of events. */
public class InMemoryJournal implements MissionJournal {

    private final List<MissionEvent> events = new ArrayList<>();
    private final List<MissionResult> outcomes = new ArrayList<>();

    @Override
    public synchronized void event(MissionEvent event) {
        events.add(event);
    }

    @Override
    public synchronized void outcome(MissionResult result) {
        outcomes.add(result);
    }

    public synchronized List<String> messages(String missionId) {
        return events.stream().filter(e -> e.missionId().equals(missionId)).map(MissionEvent::message).toList();
    }

    public synchronized List<MissionResult> outcomes() {
        return List.copyOf(outcomes);
    }

    /** Position of the first message containing the fragment, or -1. */
    public synchronized int indexOf(String missionId, String fragment) {
        List<String> m = messages(missionId);
        for (int i = 0; i < m.size(); i++) {
            if (m.get(i).contains(fragment)) {
                return i;
            }
        }
        return -1;
    }
}
