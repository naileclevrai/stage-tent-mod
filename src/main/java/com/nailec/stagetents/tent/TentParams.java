package com.nailec.stagetents.tent;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything the player can tune on a tent. Not every field matters for every {@link TentType}. Values are clamped
 * to the type's ranges on load, so a packet can't ask for a 10k block tent.
 */
public final class TentParams {
    public static final int MAX_MASTS = 8;

    public TentType type = TentType.BIG_TOP;
    public int width = 20;
    /** Big top / pagoda: straight section added between the ends. Frame tent: full length. */
    public int length = 0;
    /** Peak height above the ground (mast top, pagoda tip, frame ridge). */
    public int height = 12;
    public int wallHeight = 4;
    public int masts = 2;
    /** Side pole spacing; bay length for frame tents. */
    public int poleSpacing = 4;
    public WallMode walls = WallMode.CLOSED;
    public Entrance entrance = Entrance.FRONT;
    public boolean stripes = true;
    public boolean valance = true;
    public boolean guyRopes = true;
    public boolean flags = true;
    public int colorA = Palette.RED;
    public int colorB = Palette.WHITE;
    /** Inside face colour, or -1 to use a shaded copy of the outside. */
    public int lining = -1;
    public LightMode lights = LightMode.OFF;

    public FloorMode floor = FloorMode.NONE;
    /** Floor height in quarter blocks. */
    public int floorHeight = 0;
    public boolean stage = false;
    public int stageHeight = 1;
    public int stageDepth = 4;
    /** Black drape behind the stage. */
    public boolean curtain = true;
    /** Bit per perimeter edge (rectangular tents) joined to a neighbour: no wall, a gutter instead. */
    public int joined = 0;
    /** Rigging bars for hanging lights under the roof. */
    public boolean rigging = false;
    /** Whether the plate at the centre is drawn; a hidden plate has no collision and only shows to wrench holders. */
    public boolean showPlate = true;
    /** Sign board over the front opening (rectangular tents). */
    public boolean sign = false;
    public String signTitle = "BAR";
    public String signText = "";
    public int signColor = 0x7A1414;
    public int signTextColor = 0xFFFFFF;
    /** Curtains tied back at every front pole. */
    public boolean poleCurtains = false;
    /** Opus 4200 packed on its wheels. Other tents ignore it. */
    public boolean folded = false;

    public static final int MAX_SIGN_TITLE = 24, MAX_SIGN_TEXT = 64;
    /** Stretch tents: extra masts as world offsets from the plate {dx, dz, height}. */
    public List<int[]> stretchPoles = new ArrayList<>();

    public static final int MAX_FLOOR = 4, MIN_STAGE = 1, MAX_STAGE = 3, MIN_STAGE_DEPTH = 2, MAX_STAGE_DEPTH = 12;
    public static final int MAX_STRETCH_POLES = 16;

    public int effectiveMasts() {
        if (type != TentType.BIG_TOP) return 1;
        return length < 2 ? 1 : Mth.clamp(masts, 2, MAX_MASTS);
    }

    public TentParams clamp() {
        TentType.Ranges r = type.ranges;
        width = Mth.clamp(width, r.minWidth(), r.maxWidth());
        length = Mth.clamp(length, r.minLength(), r.maxLength());
        if (r.lengthStep() > 1) length -= length % r.lengthStep();
        height = Mth.clamp(height, r.minHeight(), r.maxHeight());
        wallHeight = Mth.clamp(wallHeight, r.minWall(), r.maxWall());
        poleSpacing = Mth.clamp(poleSpacing, r.minSpacing(), r.maxSpacing());
        masts = Mth.clamp(masts, 1, MAX_MASTS);
        colorA &= 0xFFFFFF;
        colorB &= 0xFFFFFF;
        if (lining != -1) lining &= 0xFFFFFF;
        if (type == TentType.GAZEBO && length < width) length = width;
        if (type == TentType.DJ_ARCH) {
            guyRopes = false;
            walls = WallMode.OPEN;
            entrance = Entrance.NONE;
            floor = FloorMode.NONE;
            stage = false;
            lights = LightMode.OFF;
            flags = false;
            stripes = false;
            valance = false;
            sign = false;
            poleCurtains = false;
            joined = 0;
        }
        if (type == TentType.OPUS_4200) {
            walls = WallMode.CLOSED;
            entrance = Entrance.OPEN_FRONT;
            floor = FloorMode.BLACK;
            floorHeight = 4;
            stage = false;
            rigging = false;
            lights = LightMode.OFF;
            flags = false;
            sign = false;
            poleCurtains = false;
            joined = 0;
            showPlate = false;
            guyRopes = false;
            valance = false;
        }
        floorHeight = Mth.clamp(floorHeight, 0, MAX_FLOOR);
        stageHeight = Mth.clamp(stageHeight, MIN_STAGE, MAX_STAGE);
        stageDepth = Mth.clamp(stageDepth, MIN_STAGE_DEPTH, MAX_STAGE_DEPTH);
        joined &= 0xF;
        signTitle = trim(signTitle, MAX_SIGN_TITLE);
        signText = trim(signText, MAX_SIGN_TEXT);
        signColor &= 0xFFFFFF;
        signTextColor &= 0xFFFFFF;
        if (stretchPoles.size() > MAX_STRETCH_POLES) stretchPoles = new ArrayList<>(stretchPoles.subList(0, MAX_STRETCH_POLES));
        for (int[] sp : stretchPoles) {
            sp[0] = Mth.clamp(sp[0], -32, 32);
            sp[1] = Mth.clamp(sp[1], -32, 32);
            sp[2] = Mth.clamp(sp[2], 2, 20);
        }
        return this;
    }

    public TentParams copy() {
        return load(save(), type);
    }

    /** Copies the look (colours, walls, accessories) of {@code o} but keeps this tent's type and dimensions. */
    public void copyStyle(TentParams o) {
        walls = o.walls;
        entrance = o.entrance;
        stripes = o.stripes;
        valance = o.valance;
        guyRopes = o.guyRopes;
        flags = o.flags;
        colorA = o.colorA;
        colorB = o.colorB;
        lining = o.lining;
        lights = o.lights;
        floor = o.floor;
        curtain = o.curtain;
        sign = o.sign;
        signColor = o.signColor;
        signTextColor = o.signTextColor;
        poleCurtains = o.poleCurtains;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("Type", type.id);
        t.putInt("Width", width);
        t.putInt("Length", length);
        t.putInt("Height", height);
        t.putInt("WallHeight", wallHeight);
        t.putInt("Masts", masts);
        t.putInt("PoleSpacing", poleSpacing);
        t.putString("Walls", walls.name());
        t.putString("Entrance", entrance.name());
        t.putBoolean("Stripes", stripes);
        t.putBoolean("Valance", valance);
        t.putBoolean("GuyRopes", guyRopes);
        t.putBoolean("Flags", flags);
        t.putInt("ColorA", colorA);
        t.putInt("ColorB", colorB);
        t.putInt("Lining", lining);
        t.putString("Lights", lights.name());
        t.putString("Floor", floor.name());
        t.putInt("FloorHeight", floorHeight);
        t.putBoolean("Stage", stage);
        t.putInt("StageHeight", stageHeight);
        t.putInt("StageDepth", stageDepth);
        t.putBoolean("Curtain", curtain);
        t.putInt("Joined", joined);
        t.putBoolean("Rigging", rigging);
        t.putBoolean("ShowPlate", showPlate);
        t.putBoolean("Sign", sign);
        t.putString("SignTitle", signTitle);
        t.putString("SignText", signText);
        t.putInt("SignColor", signColor);
        t.putInt("SignTextColor", signTextColor);
        t.putBoolean("PoleCurtains", poleCurtains);
        t.putBoolean("Folded", folded);
        int[] flat = new int[stretchPoles.size() * 3];
        for (int i = 0; i < stretchPoles.size(); i++) System.arraycopy(stretchPoles.get(i), 0, flat, i * 3, 3);
        t.putIntArray("StretchPoles", flat);
        return t;
    }

    /** Loads params for a tent of type {@code type}; missing keys fall back to that type's defaults. */
    public static TentParams load(CompoundTag t, TentType type) {
        TentParams p = type.defaults();
        if (t.contains("Width")) p.width = t.getInt("Width");
        if (t.contains("Length")) p.length = t.getInt("Length");
        if (t.contains("Height")) p.height = t.getInt("Height");
        if (t.contains("WallHeight")) p.wallHeight = t.getInt("WallHeight");
        if (t.contains("Masts")) p.masts = t.getInt("Masts");
        if (t.contains("PoleSpacing")) p.poleSpacing = t.getInt("PoleSpacing");
        p.walls = enumOr(t, "Walls", WallMode.class, p.walls);
        p.entrance = enumOr(t, "Entrance", Entrance.class, p.entrance);
        if (t.contains("Stripes")) p.stripes = t.getBoolean("Stripes");
        if (t.contains("Valance")) p.valance = t.getBoolean("Valance");
        if (t.contains("GuyRopes")) p.guyRopes = t.getBoolean("GuyRopes");
        if (t.contains("Flags")) p.flags = t.getBoolean("Flags");
        if (t.contains("ColorA")) p.colorA = t.getInt("ColorA");
        if (t.contains("ColorB")) p.colorB = t.getInt("ColorB");
        if (t.contains("Lining")) p.lining = t.getInt("Lining");
        p.lights = enumOr(t, "Lights", LightMode.class, p.lights);
        p.floor = enumOr(t, "Floor", FloorMode.class, p.floor);
        if (t.contains("FloorHeight")) p.floorHeight = t.getInt("FloorHeight");
        if (t.contains("Stage")) p.stage = t.getBoolean("Stage");
        if (t.contains("StageHeight")) p.stageHeight = t.getInt("StageHeight");
        if (t.contains("StageDepth")) p.stageDepth = t.getInt("StageDepth");
        if (t.contains("Curtain")) p.curtain = t.getBoolean("Curtain");
        if (t.contains("Joined")) p.joined = t.getInt("Joined");
        if (t.contains("Rigging")) p.rigging = t.getBoolean("Rigging");
        if (t.contains("ShowPlate")) p.showPlate = t.getBoolean("ShowPlate");
        if (t.contains("Sign")) p.sign = t.getBoolean("Sign");
        if (t.contains("SignTitle")) p.signTitle = t.getString("SignTitle");
        if (t.contains("SignText")) p.signText = t.getString("SignText");
        if (t.contains("SignColor")) p.signColor = t.getInt("SignColor");
        if (t.contains("SignTextColor")) p.signTextColor = t.getInt("SignTextColor");
        if (t.contains("PoleCurtains")) p.poleCurtains = t.getBoolean("PoleCurtains");
        if (t.contains("Folded")) p.folded = t.getBoolean("Folded");
        int[] flat = t.getIntArray("StretchPoles");
        for (int i = 0; i + 2 < flat.length; i += 3) p.stretchPoles.add(new int[]{flat[i], flat[i + 1], flat[i + 2]});
        return p.clamp();
    }

    private static String trim(String s, int max) {
        if (s == null) return "";
        s = s.replaceAll("[\\p{Cntrl}]", "");
        return s.length() > max ? s.substring(0, max) : s;
    }

    public static TentType typeOf(CompoundTag t, TentType fallback) {
        return t.contains("Type") ? TentType.byId(t.getString("Type")) : fallback;
    }

    private static <E extends Enum<E>> E enumOr(CompoundTag t, String key, Class<E> cls, E fallback) {
        if (!t.contains(key)) return fallback;
        try {
            return Enum.valueOf(cls, t.getString(key));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
