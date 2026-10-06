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
    /** Clear PVC sheet on two poles and one curved tube. Width = span, length = depth. */
    DJ_ARCH("dj_arch", new Ranges(4, 12, 2, 6, 1, 4, 8, 1, 1, 2, 5)),
    /** Stretch tent: free canvas over masts. Width = overhang past the masts, wall height = edge pole height. */
    STRETCH("stretch", new Ranges(2, 10, 0, 0, 1, 4, 16, 1, 5, 3, 8)),
    /** Tensile arena: membrane on two lines of lattice masts. Width = span, length = full length, wall = edge arch. */
    TENSILE("tensile", new Ranges(24, 90, 24, 160, 2, 12, 40, 3, 14, 8, 24)),
    /** Ready to use mobile concert stage, shown only in its deployed position. */
    OPUS_4200("opus_4200", new Ranges(16, 16, 13, 13, 1, 12, 12, 10, 10, 3, 3));

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
            case DJ_ARCH -> {
                p.width = 8; p.length = 6; p.height = 6; p.wallHeight = 1; p.poleSpacing = 2;
                p.walls = WallMode.OPEN; p.entrance = Entrance.NONE; p.valance = false;
                p.stripes = false; p.guyRopes = false; p.flags = false;
                p.colorA = 0x252B31; p.colorB = 0xC5D8E4;
                p.lights = LightMode.OFF; p.floor = FloorMode.NONE; p.stage = false;
                p.showPlate = false;
            }
            case STRETCH -> {
                p.width = 5; p.length = 0; p.height = 7; p.wallHeight = 2; p.poleSpacing = 4;
                p.walls = WallMode.OPEN; p.entrance = Entrance.NONE; p.valance = false;
                p.stripes = false; p.guyRopes = true; p.flags = false;
                p.colorA = 0xD2BC8F; p.colorB = 0xD2BC8F;
            }
            case TENSILE -> {
                // Real proportions of the large touring arenas: 86 x 160 m, 25 m masts, 10 masts in two lines.
                p.width = 86; p.length = 160; p.height = 25; p.wallHeight = 9; p.poleSpacing = 16; p.masts = 5;
                p.walls = WallMode.OPEN; p.entrance = Entrance.NONE; p.valance = false;
                p.stripes = false; p.guyRopes = true; p.flags = false;
                p.colorA = Palette.WHITE; p.colorB = Palette.WHITE; p.lining = 0x1C2541;
                p.stage = true; p.stageDepth = 12; p.stageHeight = 2; p.rigging = true;
            }
            case OPUS_4200 -> {
                p.width = 16; p.length = 13; p.height = 12; p.wallHeight = 10; p.poleSpacing = 3;
                p.walls = WallMode.CLOSED; p.entrance = Entrance.OPEN_FRONT;
                p.stripes = false; p.valance = false; p.guyRopes = false; p.flags = false;
                p.colorA = Palette.WHITE; p.colorB = Palette.WHITE; p.lining = 0x24262A;
                p.floor = FloorMode.BLACK; p.floorHeight = 4;
                p.lights = LightMode.OFF;
                p.rigging = false;
                p.showPlate = false;
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
        return this != ARCH && this != DJ_ARCH && this != STRETCH;
    }
}
