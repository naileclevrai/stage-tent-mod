package com.nailec.stagetents.furniture;

import net.minecraft.world.item.DyeColor;

import java.util.ArrayList;
import java.util.List;

/**
 * The three site machines and their working geometry, in model space (metres, the main block being the unit cell
 * at the origin, the front towards -z). Collision, the parts the block puts around itself and the renderer all
 * read the same numbers.
 */
public enum MachineKind {
    /** Diesel counterbalance forklift, 2.5 t, duplex mast. Lift is the fork height. */
    FORKLIFT(DyeColor.ORANGE, 14, 0.25, 0.022),
    /** Electric slab scissor lift, 6 m platform, four stages. Lift is the platform rise. */
    SCISSOR(DyeColor.RED, 10, 0.5, 0.018),
    /** Diesel rough-terrain scissor lift, 4x2, outriggers, five stages, 10 m platform. */
    RT_SCISSOR(DyeColor.YELLOW, 17, 0.5, 0.022);

    public final DyeColor color;
    /** Number of lift steps above the lowest position. */
    public final int liftSteps;
    /** Metres per lift step. */
    public final double step;
    /** Lift speed in metres per tick. */
    public final double speed;

    MachineKind(DyeColor color, int liftSteps, double step, double speed) {
        this.color = color;
        this.liftSteps = liftSteps;
        this.step = step;
        this.speed = speed;
    }

    public boolean hasTilt() {
        return this == FORKLIFT;
    }

    public boolean hasLoad() {
        return this == FORKLIFT;
    }

    public boolean hasOutriggers() {
        return this == RT_SCISSOR;
    }

    public boolean hasDeck() {
        return this != FORKLIFT;
    }

    public double maxRise() {
        return liftSteps * step;
    }

    // ------------------------------------------------------------------ forklift

    public static final int TILT_MIN = -6;
    public static final int TILT_MAX = 10;
    public static final String[] LOADS = {"none", "pallet", "boxes", "cases", "decks"};

    /** Fork face (front of the shanks) and the tips of the forks. */
    public static final double FL_FORK_FACE = 1.07;
    public static final double FL_FORK_TIP = 0.0;
    public static final double FL_MAST_Z0 = 1.12;
    public static final double FL_MAST_Z1 = 1.34;
    public static final double FL_FRONT_AXLE = 1.64;
    public static final double FL_REAR_AXLE = 3.22;
    public static final double FL_REAR = 3.64;
    public static final double FL_GUARD_TOP = 2.17;
    public static final double FL_MAST_H = 2.02;
    /** Free lift of the carriage before the inner mast starts to rise. */
    public static final double FL_FREE = 0.15;

    /** Rise of the inner mast for a fork height. */
    public static double innerRise(double fork) {
        return Math.max(0, (fork - FL_FREE) * 0.5);
    }

    /** Top of the load for the collision and the carried entities, above the fork blades. */
    public static double loadHeight(int load) {
        return switch (load) {
            case 1 -> 0.144;
            case 2 -> 0.144 + 0.90;
            case 3 -> 0.144 + 1.22;
            case 4 -> 0.144 + 0.60;
            default -> 0.045;
        };
    }

    // ------------------------------------------------------------------ scissor lifts

    public record Scissor(double x0, double x1, double chassisTop, double pivot, double stowDeck, double frame,
                          int stages, double arm, double fixedZ, double deckX0, double deckX1, double deckZ0,
                          double deckZ1, double extension, double rail) {
        public double deck(double rise) {
            return stowDeck + rise;
        }

        /** Height of one stage for a platform rise. */
        public double stage(double rise) {
            return (deck(rise) - frame - pivot) / stages;
        }

        /** Horizontal reach of one arm. */
        public double reach(double rise) {
            double s = Math.min(arm * 0.999, stage(rise));
            return Math.sqrt(arm * arm - s * s);
        }
    }

    /** Slab lift sized like a 10 m class machine: 1.17 m wide, 2.3 m long, room for a player and a half. */
    public static final Scissor SL08 = new Scissor(-0.085, 1.085, 0.50, 0.56, 0.98, 0.18, 4, 2.0, 2.12,
            -0.11, 1.11, 0.06, 2.24, 0.90, 1.10);
    /** Chassis length of the slab lift. */
    public static final double SL08_LENGTH = 2.30;

    /** Half width of the gate opening in the back rail. */
    public double gate() {
        return this == RT_SCISSOR ? 0.42 : 0.37;
    }
    public static final Scissor SL1418 = new Scissor(0.18, 1.82, 1.10, 1.18, 1.80, 0.22, 5, 2.45, 3.25,
            0.08, 1.92, 0.12, 3.78, 1.50, 1.10);

    public Scissor scissor() {
        return this == RT_SCISSOR ? SL1418 : SL08;
    }

    // ------------------------------------------------------------------ collision

    /** Collision boxes in model space, metres. */
    public List<double[]> boxes(double rise, int load, boolean deck) {
        List<double[]> out = new ArrayList<>();
        switch (this) {
            case FORKLIFT -> {
                out.add(new double[]{-0.07, 0.10, 1.40, 1.07, 1.12, FL_REAR});
                out.add(new double[]{-0.07, 0, 1.28, 1.07, 0.69, 1.98});
                out.add(new double[]{-0.07, 2.10, 1.36, 1.07, FL_GUARD_TOP, 3.30});
                double top = Math.max(FL_MAST_H + innerRise(rise), rise + 1.30);
                out.add(new double[]{0.08, 0.05, FL_MAST_Z0 - 0.06, 0.92, top, FL_MAST_Z1});
                double y = rise + 0.025;
                double h = loadHeight(load);
                if (load == 0) {
                    out.add(new double[]{0.20, y, FL_FORK_TIP, 0.34, y + h, FL_FORK_FACE});
                    out.add(new double[]{0.66, y, FL_FORK_TIP, 0.80, y + h, FL_FORK_FACE});
                } else {
                    double x0 = load == 4 ? -0.48 : 0.10, x1 = load == 4 ? 1.48 : 0.90;
                    out.add(new double[]{x0, Math.max(0, rise), FL_FORK_TIP - 0.04, x1, y + h, FL_FORK_FACE});
                }
            }
            case SCISSOR, RT_SCISSOR -> {
                Scissor s = scissor();
                boolean rt = this == RT_SCISSOR;
                out.add(new double[]{rt ? -0.15 : s.x0, rt ? 0.30 : 0.09, rt ? 0.30 : 0, rt ? 2.15 : s.x1, s.chassisTop,
                        rt ? 3.60 : SL08_LENGTH});
                if (rt) {
                    out.add(new double[]{-0.15, 0, 0.40, 2.15, 0.70, 1.10});
                    out.add(new double[]{-0.15, 0, 2.80, 2.15, 0.70, 3.50});
                    out.add(new double[]{-0.15, 0.30, 0.02, 2.15, 1.05, 0.30});
                    out.add(new double[]{-0.15, 0.30, 3.60, 2.15, 1.05, 3.88});
                }
                double d = s.deck(rise);
                double reach = s.reach(rise);
                out.add(new double[]{s.deckX0 + 0.04, s.chassisTop, s.fixedZ - reach - 0.06, s.deckX1 - 0.04,
                        d - s.frame, s.fixedZ + 0.08});
                double ext = deck ? s.extension : 0;
                double z0 = s.deckZ0 - ext;
                out.add(new double[]{s.deckX0, d - 0.08, z0, s.deckX1, d, s.deckZ1});
                double t = 0.05, r = d + s.rail;
                out.add(new double[]{s.deckX0, d, z0, s.deckX1, r, z0 + t});
                out.add(new double[]{s.deckX0, d, z0, s.deckX0 + t, r, s.deckZ1});
                out.add(new double[]{s.deckX1 - t, d, z0, s.deckX1, r, s.deckZ1});
                // Back rail with the gate opening in the middle.
                double mid = (s.deckX0 + s.deckX1) * 0.5;
                double gate = gate();
                out.add(new double[]{s.deckX0, d, s.deckZ1 - t, mid - gate, r, s.deckZ1});
                out.add(new double[]{mid + gate, d, s.deckZ1 - t, s.deckX1, r, s.deckZ1});
            }
        }
        return out;
    }

    /** Where carried entities stand (x0, z0, x1, z1, top) in model space. */
    public double[] floor(double rise, int load, boolean deck) {
        if (this == FORKLIFT) {
            double x0 = load == 4 ? -0.48 : 0.10, x1 = load == 4 ? 1.48 : 0.90;
            return new double[]{x0, FL_FORK_TIP - 0.04, x1, FL_FORK_FACE, rise + 0.025 + loadHeight(load)};
        }
        Scissor s = scissor();
        double ext = deck ? s.extension : 0;
        return new double[]{s.deckX0, s.deckZ0 - ext, s.deckX1, s.deckZ1, s.deck(rise)};
    }
}
