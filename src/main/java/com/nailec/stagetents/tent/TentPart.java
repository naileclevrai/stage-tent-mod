package com.nailec.stagetents.tent;

import net.minecraft.util.StringRepresentable;

public enum TentPart implements StringRepresentable {
    ROOF("roof"), WALL("wall"), POLE("pole"), FLOOR("floor");

    private final String name;

    TentPart(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
