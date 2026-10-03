package com.nailec.stagetents.tent;

/** The tent families. Each has its own size limits and default look. */
public enum TentType {
    BIG_TOP("big_top", new Ranges(8, 64, 0, 64, 2, 6, 40, 2, 12, 3, 8)),
    PAGODA("pagoda", new Ranges(3, 16, 0, 24, 1, 4, 16, 2, 5, 2, 5)),
    FRAME("frame", new Ranges(4, 30, 4, 64, 1, 4, 16, 2, 6, 3, 6)),
    /** Folding gazebo: small scissor-frame canopy. */
    GAZEBO("gazebo", new Ranges(2, 8, 2, 12, 1, 3, 8, 2, 3, 3, 6)),
    /** Inflatable arch: width = span, height = top, wall height = tube thickness. */
    ARCH("arch", new Ranges(4, 24, 0, 0, 1, 3, 14, 1, 3, 3, 6)),
    /** Stretch tent: free canvas over masts. Width = overhang past the masts, wall height = edge pole height. */
    STRETCH("stretch", new Ranges(2, 10, 0, 0, 1, 4, 16, 1, 5, 3, 8));

    public record Ranges(int minWidth, int maxWidth, int minLength, int maxLength, int lengthStep,
                         int minHeight, int maxHeight, int minWall, int maxWall, int minSpacing, int maxSpacing) {}

    public final String id;
    public final Ranges ranges;

    TentType(String id, Ranges ranges) {
        this.id = id;
        this.ranges = ranges;
    }

    public static TentType byId(String id) {
        for (TentType t : values()) if (t.id.equals(id)) return t;
        return BIG_TOP;
    }

    public TentParams defaults() {
        TentParams p = new TentParams();
        p.type = this;
        switch (this) {
            case BIG_TOP -> {
                p.width = 20; p.length = 0; p.height = 12; p.wallHeight = 4; p.poleSpacing = 4;
                p.walls = WallMode.CLOSED; p.entrance = Entrance.FRONT;
                p.stripes = true; p.guyRopes = true; p.flags = true;
                p.colorA = Palette.RED; p.colorB = Palette.WHITE;
            }
            case PAGODA -> {
                p.width = 5; p.length = 0; p.height = 6; p.wallHeight = 3; p.poleSpacing = 3;
                p.walls = WallMode.OPEN; p.entrance = Entrance.FRONT;
                p.stripes = false; p.guyRopes = false; p.flags = false;
                p.colorA = Palette.WHITE; p.colorB = Palette.WHITE;
            }
            case FRAME -> {
                p.width = 10; p.length = 15; p.height = 6; p.wallHeight = 3; p.poleSpacing = 5;
                p.walls = WallMode.WINDOWS; p.entrance = Entrance.FRONT;
                p.stripes = false; p.guyRopes = false; p.flags = false;
                p.colorA = Palette.WHITE; p.colorB = Palette.WHITE;
            }
            case GAZEBO -> {
                p.width = 3; p.length = 3; p.height = 4; p.wallHeight = 2; p.poleSpacing = 3;
                p.walls = WallMode.OPEN; p.entrance = Entrance.FRONT;
                p.stripes = false; p.guyRopes = false; p.flags = false;
                p.colorA = Palette.WHITE; p.colorB = Palette.WHITE;
            }
            case ARCH -> {
                p.width = 8; p.length = 0; p.height = 6; p.wallHeight = 2; p.poleSpacing = 4;
                p.walls = WallMode.OPEN; p.entrance = Entrance.NONE; p.valance = false;
                p.stripes = true; p.guyRopes = true; p.flags = false;
                p.colorA = Palette.RED; p.colorB = Palette.WHITE;
            }
            case STRETCH -> {
                p.width = 5; p.length = 0; p.height = 7; p.wallHeight = 2; p.poleSpacing = 4;
                p.walls = WallMode.OPEN; p.entrance = Entrance.NONE; p.valance = false;
                p.stripes = false; p.guyRopes = true; p.flags = false;
                p.colorA = 0xD2BC8F; p.colorB = 0xD2BC8F;
            }
        }
        return p;
    }

    /** Types built on a rectangular frame, which can be joined side by side with gutters. */
    public boolean isModular() {
        return this == PAGODA || this == FRAME || this == GAZEBO;
    }

    /** Types with an enclosed floor area (floor, stage, walls). */
    public boolean hasInterior() {
        return this != ARCH && this != STRETCH;
    }
}
