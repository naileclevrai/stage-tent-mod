package com.nailec.stagetents.tent;

import java.util.List;
import java.util.function.Consumer;

/** Ready-made looks. A preset only changes style fields, never dimensions. */
public final class Presets {
    public record Preset(String key, Consumer<TentParams> apply) {}

    private static final int BLACK = 0x141416, CHARCOAL = 0x2A2B2E, IVORY = 0xEDE3C8, BLUE = 0x2E4FA8,
            YELLOW = 0xF2CC32, GREEN = 0x3F6B2A, NAVY = 0x1C2541, GOLD = 0xD4A62A;

    private Presets() {}

    public static List<Preset> forType(TentType type) {
        return switch (type) {
            case BIG_TOP -> List.of(
                    new Preset("classic", p -> style(p, Palette.RED, Palette.WHITE, true, -1, WallMode.CLOSED, LightMode.OFF)),
                    new Preset("blue_circus", p -> style(p, BLUE, YELLOW, true, -1, WallMode.CLOSED, LightMode.WARM)),
                    new Preset("concert", p -> style(p, BLACK, BLACK, false, BLACK, WallMode.CLOSED, LightMode.OFF)),
                    new Preset("wedding", p -> style(p, Palette.WHITE, IVORY, false, Palette.WHITE, WallMode.WINDOWS, LightMode.WARM)),
                    new Preset("cabaret", p -> style(p, NAVY, GOLD, true, NAVY, WallMode.CLOSED, LightMode.WARM)),
                    new Preset("guinguette", p -> style(p, GREEN, Palette.WHITE, true, -1, WallMode.OPEN, LightMode.PARTY)));
            case PAGODA -> withRegie(List.of(
                    new Preset("pagoda_white", p -> style(p, Palette.WHITE, Palette.WHITE, false, -1, WallMode.OPEN, LightMode.OFF)),
                    new Preset("pagoda_black", p -> style(p, BLACK, BLACK, false, BLACK, WallMode.OPEN, LightMode.OFF)),
                    new Preset("pagoda_striped", p -> style(p, Palette.RED, Palette.WHITE, true, -1, WallMode.OPEN, LightMode.OFF)),
                    new Preset("pagoda_wedding", p -> style(p, Palette.WHITE, IVORY, false, Palette.WHITE, WallMode.WINDOWS, LightMode.WARM))));
            case FRAME -> withRegie(List.of(
                    new Preset("reception", p -> style(p, Palette.WHITE, Palette.WHITE, false, Palette.WHITE, WallMode.WINDOWS, LightMode.WARM)),
                    new Preset("event_black", p -> style(p, CHARCOAL, CHARCOAL, false, BLACK, WallMode.CLOSED, LightMode.OFF)),
                    new Preset("garden", p -> style(p, GREEN, Palette.WHITE, true, -1, WallMode.OPEN, LightMode.PARTY)),
                    new Preset("seaside", p -> style(p, BLUE, Palette.WHITE, true, -1, WallMode.OPEN, LightMode.OFF))));
            case GAZEBO -> withRegie(List.of(
                    new Preset("gazebo_white", p -> style(p, Palette.WHITE, Palette.WHITE, false, -1, WallMode.OPEN, LightMode.OFF)),
                    new Preset("gazebo_black", p -> style(p, BLACK, BLACK, false, BLACK, WallMode.OPEN, LightMode.OFF)),
                    new Preset("gazebo_red", p -> style(p, Palette.RED, Palette.WHITE, false, -1, WallMode.OPEN, LightMode.OFF)),
                    new Preset("gazebo_market", p -> style(p, GREEN, Palette.WHITE, true, -1, WallMode.OPEN, LightMode.WARM))));
            case ARCH -> List.of(
                    new Preset("arch_classic", p -> style(p, Palette.RED, Palette.WHITE, true, -1, WallMode.OPEN, LightMode.OFF)),
                    new Preset("arch_start", p -> style(p, BLACK, Palette.WHITE, true, -1, WallMode.OPEN, LightMode.OFF)),
                    new Preset("arch_blue", p -> style(p, BLUE, YELLOW, true, -1, WallMode.OPEN, LightMode.OFF)));
            case DJ_ARCH -> List.of(
                    new Preset("dj_clear_black", p -> style(p, 0x252B31, 0xC5D8E4, false, -1, WallMode.OPEN, LightMode.OFF)),
                    new Preset("dj_clear_silver", p -> style(p, 0xAEB5BA, 0xDFEAF0, false, -1, WallMode.OPEN, LightMode.OFF)),
                    new Preset("dj_smoke", p -> style(p, 0x15191D, 0x56667A, false, -1, WallMode.OPEN, LightMode.OFF)),
                    new Preset("dj_blue", p -> style(p, 0x263E62, 0x9CC2E0, false, -1, WallMode.OPEN, LightMode.OFF)));
            case STRETCH -> List.of(
                    new Preset("stretch_sand", p -> style(p, 0xD2BC8F, 0xD2BC8F, false, -1, WallMode.OPEN, LightMode.OFF)),
                    new Preset("stretch_white", p -> style(p, Palette.WHITE, Palette.WHITE, false, -1, WallMode.OPEN, LightMode.WARM)),
                    new Preset("stretch_black", p -> style(p, BLACK, BLACK, false, BLACK, WallMode.OPEN, LightMode.PARTY)),
                    new Preset("stretch_terracotta", p -> style(p, 0xB5532E, 0xB5532E, false, -1, WallMode.OPEN, LightMode.WARM)));
            case TENSILE -> List.of(
                    new Preset("arena_white_navy", p -> style(p, Palette.WHITE, Palette.WHITE, false, NAVY, WallMode.OPEN, LightMode.OFF)),
                    new Preset("epicdude", p -> style(p, Palette.WHITE, NAVY, false, NAVY, WallMode.OPEN, LightMode.OFF)),
                    new Preset("arena_white", p -> style(p, Palette.WHITE, Palette.WHITE, false, Palette.WHITE, WallMode.OPEN, LightMode.WARM)),
                    new Preset("arena_black", p -> style(p, BLACK, BLACK, false, BLACK, WallMode.OPEN, LightMode.OFF)),
                    new Preset("arena_sand", p -> style(p, 0xD2BC8F, 0xD2BC8F, false, -1, WallMode.OPEN, LightMode.WARM)));
            case OPUS_4200 -> List.of(
                    new Preset("mobile_white", p -> style(p, Palette.WHITE, Palette.WHITE, false, BLACK, WallMode.CLOSED, LightMode.OFF)),
                    new Preset("mobile_black", p -> style(p, BLACK, BLACK, false, BLACK, WallMode.CLOSED, LightMode.OFF)));
        };
    }

    /** Front-of-house control tent: black, counter front, raised black floor. */
    private static Preset regie() {
        return new Preset("regie", p -> {
            style(p, BLACK, BLACK, false, BLACK, WallMode.CLOSED, LightMode.OFF);
            p.entrance = Entrance.COUNTER;
            p.floor = FloorMode.BLACK;
            p.floorHeight = 2;
            p.valance = true;
            p.guyRopes = false;
        });
    }

    /** Festival bar: white canvas, counter front open at the back, red sign board, curtains at the poles. */
    private static Preset bar() {
        return new Preset("bar", p -> {
            style(p, Palette.WHITE, Palette.WHITE, false, -1, WallMode.CLOSED, LightMode.WARM);
            p.entrance = Entrance.COUNTER_BACK;
            p.sign = true;
            p.signTitle = "BAR";
            p.signColor = 0x7A1414;
            p.signTextColor = 0xFFFFFF;
            p.poleCurtains = true;
            p.valance = false;
            p.guyRopes = false;
        });
    }

    private static List<Preset> withRegie(List<Preset> list) {
        java.util.ArrayList<Preset> out = new java.util.ArrayList<>(list);
        out.add(0, regie());
        out.add(1, bar());
        return out;
    }

    private static void style(TentParams p, int a, int b, boolean stripes, int lining, WallMode walls, LightMode lights) {
        p.colorA = a;
        p.colorB = b;
        p.stripes = stripes;
        p.lining = lining;
        p.walls = walls;
        p.lights = lights;
    }
}
