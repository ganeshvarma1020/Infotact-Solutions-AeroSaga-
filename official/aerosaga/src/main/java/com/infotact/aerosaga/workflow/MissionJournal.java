package com.infotact.aerosaga.workflow;

/**
 * Where mission history is stored. The real one writes to the database;
 * tests use an in-memory one. The workflow never talks to this directly:
 * it goes through an activity, because workflow code must not do I/O.
 */
public interface MissionJournal {

    void event(MissionEvent event);

    void outcome(MissionResult result);
}
