package com.nailec.stagetents.tent;

/** Floor laid inside the tent. */
public enum FloorMode {
    NONE(0), WOOD(0xC9A36B), DARK_WOOD(0x5C3D24), CARPET(0x8E1B1B), BLACK(0x1E1E20), WHITE(0xECECEA);

    /** Tint applied to the floor texture. */
    public final int tint;

    FloorMode(int tint) {
        this.tint = tint;
    }

    public boolean isWood() {
        return this == WOOD || this == DARK_WOOD;
    }
}
