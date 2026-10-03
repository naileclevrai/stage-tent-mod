package com.nailec.stagetents.tent;

/** Openings left in closed walls, on the +x (front) end and optionally the opposite end. */
public enum Entrance {
    NONE, FRONT, BOTH,
    /** The whole front side open (control tents, bars). */
    OPEN_FRONT,
    /** Front side closed up to counter height only, with a shelf on top (front-of-house control tent). */
    COUNTER,
    /** Counter front and the whole back side open, to walk in behind the desk. */
    COUNTER_BACK;

    public boolean hasCounter() {
        return this == COUNTER || this == COUNTER_BACK;
    }

    /** Modes that open a whole side rather than a door; they get no door curtains. */
    public boolean opensWholeSide() {
        return this == OPEN_FRONT || this == COUNTER || this == COUNTER_BACK;
    }

    /** Height of the low front wall in {@link #COUNTER} mode. */
    public static final double COUNTER_HEIGHT = 1.1;
}
