package com.nailec.stagetents.tent;

import java.util.List;

/** Canvas colours offered by the settings screen. Any other RGB value can still be typed in as hex. */
public final class Palette {
    public static final int WHITE = 0xF4F2EC;
    public static final int RED = 0xB02E26;

    public record Entry(String key, int rgb) {}

    public static final List<Entry> COLORS = List.of(
            new Entry("white", WHITE),
            new Entry("ivory", 0xEDE3C8),
            new Entry("sand", 0xD2BC8F),
            new Entry("light_gray", 0x9D9D97),
            new Entry("gray", 0x55585C),
            new Entry("charcoal", 0x2A2B2E),
            new Entry("black", 0x141416),
            new Entry("red", RED),
            new Entry("bordeaux", 0x6B1522),
            new Entry("orange", 0xE07A1F),
            new Entry("gold", 0xD4A62A),
            new Entry("yellow", 0xF2CC32),
            new Entry("lime", 0x80C71F),
            new Entry("green", 0x3F6B2A),
            new Entry("teal", 0x169C9C),
            new Entry("light_blue", 0x3AB3DA),
            new Entry("blue", 0x2E4FA8),
            new Entry("navy", 0x1C2541),
            new Entry("purple", 0x7232A8),
            new Entry("magenta", 0xC74EBD),
            new Entry("pink", 0xF38BAA),
            new Entry("brown", 0x7A5232));

    private Palette() {}

    public static int indexOf(int rgb) {
        for (int i = 0; i < COLORS.size(); i++) if (COLORS.get(i).rgb == rgb) return i;
        return -1;
    }

    /** Next palette colour after {@code rgb}; a custom colour jumps to the first entry. */
    public static int next(int rgb, int dir) {
        int i = indexOf(rgb), n = COLORS.size();
        if (i < 0) return COLORS.get(0).rgb;
        return COLORS.get(Math.floorMod(i + dir, n)).rgb;
    }
}
