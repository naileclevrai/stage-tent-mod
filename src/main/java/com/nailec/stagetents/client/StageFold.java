package com.nailec.stagetents.client;

import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;

/**
 * Folds the deployed Opus 4200 back onto its trailer. {@code t} is 0 when the stage is open and 1 when it is packed.
 * The motion is a few overlapping beats: jacks tuck in, the cloth is gathered, the stairs and the front truss fold,
 * the awnings flip onto the roof, the wings stand up, and the roof lowers onto them.
 */
final class StageFold {
    static final int BODY = 0, WING_POS = 1, WING_NEG = 2, ROOF = 3, AWNING_POS = 4, AWNING_NEG = 5;
    static final int FACADE = 6, CURTAIN = 7, STAIRS = 8, JACK = 9, JACK_POS = 10, JACK_NEG = 11, POST = 12, STRUT = 13;

    /** Pose sampled while the mesh is built. The renderer writes it just before the build. */
    static float current;

    private static final float DURATION = 3.4F;
    /** How far the roof cassette drops, in blocks, so it lands on the folded wings. */
    private static final double DROP = 5.20;
    private static final double HINGE_X = 2.50, HINGE_Y = 1.84;
    private static final double ROOF_HINGE_Y = 11.28, ROOF_HINGE_X = 2.44;

    private static final Map<Long, Anim> ANIMS = new HashMap<>();

    private StageFold() {}

    /** Animated pose for one placed stage. The first time a stage is seen it is already at its saved pose. */
    static float amount(long key, boolean folded) {
        float target = folded ? 1F : 0F;
        long now = System.nanoTime();
        Anim anim = ANIMS.get(key);
        if (anim == null) {
            anim = new Anim();
            anim.shown = target;
            anim.target = target;
            anim.from = target;
            anim.t0 = now;
            ANIMS.put(key, anim);
            return target;
        }
        if (anim.target != target) {
            anim.from = anim.shown;
            anim.target = target;
            anim.t0 = now;
        }
        float u = Mth.clamp((now - anim.t0) / 1.0e9F / DURATION, 0F, 1F);
        // Zero speed at both ends, so the trailer eases out of one pose and into the other.
        float ease = u * u * u * (u * (u * 6F - 15F) + 10F);
        anim.shown = anim.from + (anim.target - anim.from) * ease;
        return anim.shown;
    }

    static void apply(double[][] quad, int part, float t) {
        if (t < 0.001F || part == BODY) return;
        float jack = span(t, 0.00F, 0.20F);
        float curtain = span(t, 0.00F, 0.30F);
        float stair = span(t, 0.06F, 0.36F);
        float facade = span(t, 0.12F, 0.50F);
        float strut = span(t, 0.08F, 0.28F);
        float awning = span(t, 0.22F, 0.58F);
        float wing = span(t, 0.42F, 0.78F);
        float roof = span(t, 0.58F, 1.00F);
        switch (part) {
            case JACK -> squash(quad, jack);
            case JACK_POS -> {
                squash(quad, jack);
                rotZ(quad, HINGE_X, HINGE_Y, 90F * wing);
            }
            case JACK_NEG -> {
                squash(quad, jack);
                rotZ(quad, -HINGE_X, HINGE_Y, -90F * wing);
            }
            case WING_POS -> {
                rotZ(quad, HINGE_X, HINGE_Y, 90F * wing);
                tuckSides(quad, wing);
            }
            case WING_NEG -> {
                rotZ(quad, -HINGE_X, HINGE_Y, -90F * wing);
                tuckSides(quad, wing);
            }
            case STAIRS -> {
                rotZ(quad, -6.60, 1.76, -85F * stair);
                for (double[] v : quad) v[0] += 6.0 * stair;
            }
            case CURTAIN -> {
                // Pile every cloth, skirt included, on the deck. Gathering it up to the roof
                // left the hems in the air once the stage was open, and under the lid once it was shut.
                for (double[] v : quad) {
                    if (v[0] > 2.35) v[0] += (2.35 - v[0]) * curtain;
                    if (v[0] < -2.35) v[0] += (-2.35 - v[0]) * curtain;
                    if (v[1] > 2.05) v[1] += (2.05 - v[1]) * curtain;
                }
            }
            case FACADE -> {
                // Lay the front truss down on the deck and draw it inside the trailer.
                // Folding it up onto the roof left it floating under the lid.
                rotZ(quad, 6.20, 2.04, 90F * facade);
                for (double[] v : quad) {
                    v[0] += (2.20 - v[0]) * facade;
                    if (v[1] > 2.20) v[1] += (2.05 - v[1]) * facade;
                }
            }
            case AWNING_POS -> {
                rotZ(quad, ROOF_HINGE_X, ROOF_HINGE_Y, 180F * awning);
                drop(quad, roof);
                seatOnRoof(quad, roof);
            }
            case AWNING_NEG -> {
                rotZ(quad, -ROOF_HINGE_X, ROOF_HINGE_Y, -180F * awning);
                drop(quad, roof);
                seatOnRoof(quad, roof);
            }
            case ROOF -> drop(quad, roof);
            case POST -> {
                double base = 1.64, span = 10.86 - base;
                double shrink = (DROP / span) * roof;
                for (double[] v : quad) v[1] = base + (v[1] - base) * (1.0 - shrink);
            }
            case STRUT -> {
                shrinkToCentre(quad, strut);
                // A collapsed strut would otherwise stay as a speck up in the air.
                for (double[] v : quad) v[1] += (1.15 - v[1]) * strut;
            }
            default -> {
            }
        }
        // Anything still proud of the packed box is pulled onto the trailer at the end of the motion.
        // Cloth and the front truss already sit on the deck; seating them on the lid would lift them again.
        float pack = span(t, 0.82F, 1F);
        if (pack > 0F && part != STRUT && part != CURTAIN && part != FACADE) {
            tuckSides(quad, pack);
            seatOnRoof(quad, pack);
        }
    }

    /** Under-deck steel swings out past the wall when a wing stands up. Bring it back onto the side. */
    private static void tuckSides(double[][] quad, float k) {
        if (k <= 0F) return;
        for (double[] v : quad) {
            // The pull is zero right on the side, so a vertex slides in instead of jumping when it crosses the line.
            if (v[0] > 2.50) v[0] += (2.50 - v[0]) * k;
            if (v[0] < -2.50) v[0] += (-2.50 - v[0]) * k;
        }
    }

    /** Flipped awnings land a little above the lid. Sit them on it. */
    private static void seatOnRoof(double[][] quad, float k) {
        if (k <= 0F) return;
        for (double[] v : quad) if (v[1] > 6.02) v[1] += (6.02 - v[1]) * k;
    }

    private static void drop(double[][] quad, float roof) {
        for (double[] v : quad) v[1] -= DROP * roof;
    }

    /** Pulls a jack up into a nub under the deck before the wing it hangs from stands up. */
    private static void squash(double[][] quad, float k) {
        for (double[] v : quad) v[1] += (1.65 - v[1]) * (0.82 * k);
    }

    private static void shrinkToCentre(double[][] quad, float k) {
        double cx = (quad[0][0] + quad[1][0] + quad[2][0]) / 3.0;
        double cy = (quad[0][1] + quad[1][1] + quad[2][1]) / 3.0;
        double cz = (quad[0][2] + quad[1][2] + quad[2][2]) / 3.0;
        for (double[] v : quad) {
            v[0] += (cx - v[0]) * k;
            v[1] += (cy - v[1]) * k;
            v[2] += (cz - v[2]) * k;
        }
    }

    /** Right-handed rotation around Z. Positions and normals both turn; uvs stay. */
    private static void rotZ(double[][] quad, double cx, double cy, float degrees) {
        if (degrees == 0F) return;
        double rad = Math.toRadians(degrees);
        double c = Math.cos(rad), s = Math.sin(rad);
        for (double[] v : quad) {
            double x = v[0] - cx, y = v[1] - cy;
            v[0] = cx + x * c - y * s;
            v[1] = cy + x * s + y * c;
            double nx = v[5], ny = v[6];
            v[5] = nx * c - ny * s;
            v[6] = nx * s + ny * c;
        }
    }

    private static float span(float t, float start, float end) {
        float u = Mth.clamp((t - start) / (end - start), 0F, 1F);
        return u * u * (3F - 2F * u);
    }

    private static final class Anim {
        float shown, from, target;
        long t0;
    }
}
