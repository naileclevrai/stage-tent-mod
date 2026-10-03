package com.nailec.stagetents.tent;

/** Openings left in closed walls, on the +x (front) end and optionally the opposite end. */
public enum Entrance {
    NONE, FRONT, BOTH,
    /** The whole front side open (control tents, bars). */
    OPEN_FRONT,
    /** Front side closed up to counter height only, with a shelf on top (front-of-house control tent). */
    COUNTER;

    /** Height of the low front wall in {@link #COUNTER} mode. */
    public static final double COUNTER_HEIGHT = 1.1;
}
