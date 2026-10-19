package com.nailec.stagetents.client;

import com.nailec.stagetents.furniture.ControlTowerBlock;

/**
 * Front-of-house gear on the top deck of a control tower: two amp racks on castors against the back rail, a stack of
 * flight cases, two high stools at the open front, and the small things engineers leave around (desk lamp, radio,
 * gaffer roll, coffee cups, a coil of cable). A loom of cables runs from the racks down a back standard to the ground.
 *
 * <p>Model space of the tower: front (open side) at z = 0, back rail at z = d, deck surface at y = top.
 */
final class FohGear {
    private static final int CASE = 0xFF1E2125;
    private static final int ALU = 0xFFB9C0C6;
    private static final int CAP = 0xFFDADFE3;
    private static final int PANEL = 0xFF2B2F34;
    private static final int PANEL_LIGHT = 0xFF3B4047;
    private static final int RAIL = 0xFF55595F;
    private static final int HOLE = 0xFF101214;
    private static final int KNOB = 0xFF9AA1A8;
    private static final int RUBBER = 0xFF141618;
    private static final int GREEN = 0xFF4CFF6A;
    private static final int AMBER = 0xFFFFC43D;
    private static final int RED = 0xFFFF4A3A;
    private static final int BLUE = 0xFF6FC8FF;
    private static final int CORD = 0xFF101113;
    private static final double U = 0.0445;

    private FohGear() {}

    static void build(ControlTowerRenderer.Builder b, double w, double d, double top) {
        double rackZ = d - 0.27 - 0.62;
        rack(b, 1.35, rackZ, top, "DAAAAPBAAAAPBAAB".toCharArray());
        rack(b, 2.0, rackZ, top, "DPPAAAABAAAPBPAB".toCharArray());
        double rackTop = top + 0.11 + 1.30;

        // Things left on the racks.
        gaffer(b, 1.52, rackTop, rackZ + 0.18);
        radio(b, 1.78, rackTop, rackZ + 0.25);
        cup(b, 1.60, rackTop, rackZ + 0.44);
        cup(b, 2.18, rackTop, rackZ + 0.40);
        lamp(b, 2.42, rackTop, rackZ + 0.48);

        // Flight cases: a wheeled trunk with a smaller case on it in the back right corner, a long case on its side.
        double cx = w - 1.30;
        flightCase(b, cx, top, d - 0.88, 0.95, 0.56, 0.60, true);
        flightCase(b, cx + 0.07, top + 0.11 + 0.56, d - 0.82, 0.80, 0.40, 0.50, false);
        flightCase(b, w - 0.85, top, 1.30, 0.55, 0.45, 1.20, false);

        // High stools at the open front.
        stool(b, w * 0.40, top, 1.10);
        stool(b, w * 0.62, top, 0.95);

        // A coil of spare cable on the deck, and the loom from the racks down the back.
        coil(b, 3.05, top, d - 0.55, 0.22);
        loom(b, 1.95, top, rackZ + 0.62, d);
    }

    // ------------------------------------------------------------------ rack

    /** Shock-mount rack on castors with its front lid off. Units listed bottom to top: A amp, P processor, D power, B blank. */
    private static void rack(ControlTowerRenderer.Builder b, double x, double z, double y, char[] units) {
        double w = 0.60, dd = 0.62, h = 1.30;
        double y0 = y + 0.11;
        castors(b, x, z, w, dd, y);
        double x1 = x + w, z1 = z + dd, y1 = y0 + h;
        double t = 0.03;
        b.box(x, y0, z, x + t, y1, z1, CASE, 3);
        b.box(x1 - t, y0, z, x1, y1, z1, CASE, 3);
        b.box(x, y0, z1 - t, x1, y1, z1, CASE, 3);
        b.box(x, y1 - t, z, x1, y1, z1, CASE, 3);
        b.box(x, y0, z, x1, y0 + t, z1, CASE, 3);
        shell(b, x, y0, z, x1, y1, z1);
        // Butterfly latches and a recessed handle on each side.
        for (double sx : new double[]{x - 0.006, x1 + 0.006}) {
            latch(b, sx, y0 + 0.08, z + dd * 0.5, true);
            latch(b, sx, y1 - 0.08, z + dd * 0.5, true);
            b.box(Math.min(sx, sx + (sx < x ? 0.012 : -0.012)) - 0.004, y0 + h * 0.55, z + dd * 0.5 - 0.11,
                    Math.max(sx, sx + (sx < x ? 0.012 : -0.012)) + 0.004, y0 + h * 0.55 + 0.09, z + dd * 0.5 + 0.11, HOLE, 3);
            b.box(Math.min(sx, sx + (sx < x ? -0.018 : 0.018)), y0 + h * 0.55 + 0.03, z + dd * 0.5 - 0.08,
                    Math.max(sx, sx + (sx < x ? -0.018 : 0.018)), y0 + h * 0.55 + 0.055, z + dd * 0.5 + 0.08, ALU, 0);
        }
        // Rack rails with their square holes.
        double zr = z + 0.035;
        for (double rx : new double[]{x + 0.035, x1 - 0.06}) {
            b.box(rx, y0 + t, zr, rx + 0.025, y1 - t, zr + 0.012, RAIL, 0);
            for (double hy = y0 + t + 0.01; hy < y1 - t - 0.01; hy += U) {
                flat(b, rx + 0.008, hy, rx + 0.017, hy + 0.007, zr - 0.001, HOLE);
            }
        }
        // Units bolted to the rails.
        double uy = y0 + t + 0.01;
        for (char kind : units) {
            int n = switch (kind) {
                case 'A' -> 2;
                case 'P', 'D', 'B' -> 1;
                default -> 1;
            };
            double hgt = n * U - 0.002;
            unit(b, kind, x + 0.03, uy, zr - 0.012, x1 - 0.03, uy + hgt);
            uy += n * U;
        }
        // The rest of the rack front stays empty: dark back panel.
        b.box(x + t, uy, z1 - t - 0.002, x1 - t, y1 - t, z1 - t, HOLE, 3);
    }

    /** One rack unit front panel, from (x0, y0) to (x1, y1) on the plane z (facing -z). */
    private static void unit(ControlTowerRenderer.Builder b, char kind, double x0, double y0, double z, double x1, double y1) {
        int face = kind == 'B' ? PANEL : (kind == 'P' ? PANEL_LIGHT : PANEL);
        b.box(x0, y0, z, x1, y1, z + 0.012, face, 3);
        // Rack ears with their screws.
        for (double ex : new double[]{x0 + 0.012, x1 - 0.012}) {
            b.box(ex - 0.006, y0 + 0.01, z - 0.003, ex + 0.006, y0 + 0.022, z, KNOB, 0);
        }
        double h = y1 - y0, cy = (y0 + y1) * 0.5;
        switch (kind) {
            case 'A' -> {
                // Amplifier: two handles, vent slots, two level meters and two gain knobs.
                for (double hx : new double[]{x0 + 0.035, x1 - 0.045}) {
                    b.box(hx, y0 + 0.012, z - 0.022, hx + 0.01, y1 - 0.012, z - 0.012, ALU, 0);
                    b.box(hx, y0 + 0.012, z - 0.022, hx + 0.01, y0 + 0.018, z, ALU, 0);
                    b.box(hx, y1 - 0.018, z - 0.022, hx + 0.01, y1 - 0.012, z, ALU, 0);
                }
                for (int i = 0; i < 9; i++) {
                    double vx = x0 + 0.07 + i * 0.016;
                    flat(b, vx, y0 + 0.015, vx + 0.008, y1 - 0.015, z - 0.001, HOLE);
                }
                for (int ch = 0; ch < 2; ch++) {
                    double mx = x0 + 0.27 + ch * 0.05;
                    for (int seg = 0; seg < 5; seg++) {
                        int col = seg < 3 ? GREEN : seg == 3 ? AMBER : RED;
                        boolean on = seg < 3 + ch;
                        double sy = y0 + 0.012 + seg * (h - 0.024) / 5;
                        b.box(mx, sy, z - 0.002, mx + 0.012, sy + (h - 0.024) / 5 - 0.003, z, on ? col : 0xFF26302A, on ? 4 : 3);
                    }
                    knob(b, x0 + 0.40 + ch * 0.06, cy, z);
                }
            }
            case 'P' -> {
                // Processor: lit screen, a row of buttons, an encoder.
                b.box(x0 + 0.06, y0 + 0.01, z - 0.002, x0 + 0.20, y1 - 0.01, z, BLUE, 4);
                for (int i = 0; i < 6; i++) {
                    double bx = x0 + 0.23 + i * 0.03;
                    boolean lit = i == 1 || i == 4;
                    b.box(bx, cy - 0.008, z - 0.005, bx + 0.018, cy + 0.008, z, lit ? AMBER : KNOB, lit ? 4 : 0);
                }
                knob(b, x1 - 0.07, cy, z);
            }
            case 'D' -> {
                // Power distribution: a row of sockets and a lit red switch.
                for (int i = 0; i < 7; i++) {
                    double sx = x0 + 0.05 + i * 0.055;
                    b.box(sx, y0 + 0.008, z - 0.003, sx + 0.04, y1 - 0.008, z, 0xFF0C0D0F, 3);
                    b.box(sx + 0.012, cy - 0.004, z - 0.004, sx + 0.016, cy + 0.004, z - 0.003, KNOB, 0);
                    b.box(sx + 0.024, cy - 0.004, z - 0.004, sx + 0.028, cy + 0.004, z - 0.003, KNOB, 0);
                }
                b.box(x1 - 0.07, y0 + 0.008, z - 0.006, x1 - 0.035, y1 - 0.008, z, RED, 4);
            }
            default -> {
                // Blank panel with a vent row.
                for (int i = 0; i < 12; i++) {
                    double vx = x0 + 0.06 + i * 0.035;
                    flat(b, vx, cy - 0.006, vx + 0.02, cy + 0.006, z - 0.001, HOLE);
                }
            }
        }
    }

    /** Flat dark patch facing -z (rail holes, vent slots): one quad instead of a box. */
    private static void flat(ControlTowerRenderer.Builder b, double x0, double y0, double x1, double y1, double z, int col) {
        b.quad(x1, y0, z, x0, y0, z, x0, y1, z, x1, y1, z, col, 3);
    }

    private static void knob(ControlTowerRenderer.Builder b, double x, double y, double z) {
        b.rod(x, y, z, x, y, z - 0.018, 0.012, 0xFF1A1C1F, 3);
        b.box(x - 0.002, y + 0.004, z - 0.019, x + 0.002, y + 0.012, z - 0.018, CAP, 0);
    }

    // ------------------------------------------------------------------ flight cases

    /** Plywood flight case with aluminium extrusions, ball corners, butterfly latches and side handles. */
    private static void flightCase(ControlTowerRenderer.Builder b, double x, double y, double z, double w, double h, double dd, boolean wheels) {
        double y0 = wheels ? y + 0.11 : y;
        if (wheels) castors(b, x, z, w, dd, y);
        double x1 = x + w, y1 = y0 + h, z1 = z + dd;
        b.box(x, y0, z, x1, y1, z1, CASE, 3);
        shell(b, x, y0, z, x1, y1, z1);
        // Lid line a third of the way down.
        double ly = y1 - h * 0.3;
        b.box(x - 0.008, ly - 0.012, z - 0.008, x1 + 0.008, ly + 0.012, z1 + 0.008, ALU, 0);
        // Two latches on the front and the back, a handle on each end.
        for (double lx : new double[]{x + w * 0.25, x + w * 0.75}) {
            latch(b, lx, ly, z - 0.006, false);
            latch(b, lx, ly, z1 + 0.006, false);
        }
        for (double sx : new double[]{x - 0.004, x1 + 0.004}) {
            double hy = y0 + h * 0.45;
            b.box(Math.min(sx, sx + (sx < x ? 0.01 : -0.01)), hy, z + dd * 0.5 - 0.09,
                    Math.max(sx, sx + (sx < x ? 0.01 : -0.01)), hy + 0.07, z + dd * 0.5 + 0.09, HOLE, 3);
            b.box(Math.min(sx, sx + (sx < x ? -0.016 : 0.016)), hy + 0.025, z + dd * 0.5 - 0.06,
                    Math.max(sx, sx + (sx < x ? -0.016 : 0.016)), hy + 0.045, z + dd * 0.5 + 0.06, ALU, 0);
        }
        // Stencilled label patch on the lid.
        b.box(x + w * 0.35, y1, z + dd * 0.3, x + w * 0.65, y1 + 0.002, z + dd * 0.7, 0xFFE8E3D2, 3);
    }

    /** Aluminium angle on all twelve edges and a ball corner on each of the eight corners. */
    private static void shell(ControlTowerRenderer.Builder b, double x0, double y0, double z0, double x1, double y1, double z1) {
        double e = 0.022, o = 0.004;
        for (double y : new double[]{y0, y1}) {
            b.box(x0 - o, y - e * 0.5, z0 - o, x1 + o, y + e * 0.5, z0 + e, ALU, 0);
            b.box(x0 - o, y - e * 0.5, z1 - e, x1 + o, y + e * 0.5, z1 + o, ALU, 0);
            b.box(x0 - o, y - e * 0.5, z0 - o, x0 + e, y + e * 0.5, z1 + o, ALU, 0);
            b.box(x1 - e, y - e * 0.5, z0 - o, x1 + o, y + e * 0.5, z1 + o, ALU, 0);
        }
        for (double x : new double[]{x0, x1}) {
            for (double z : new double[]{z0, z1}) {
                b.box(x - o - (x == x0 ? 0 : e - o), y0, z - o - (z == z0 ? 0 : e - o),
                        x + e - (x == x0 ? 0 : e), y1, z + e - (z == z0 ? 0 : e), ALU, 0);
                for (double y : new double[]{y0, y1}) {
                    b.box(x - 0.028, y - 0.028, z - 0.028, x + 0.028, y + 0.028, z + 0.028, CAP, 0);
                }
            }
        }
    }

    /** Recessed butterfly latch. On a side face when {@code side}, else on a front or back face. */
    private static void latch(ControlTowerRenderer.Builder b, double x, double y, double z, boolean side) {
        if (side) {
            b.box(x - 0.006, y - 0.035, z - 0.04, x + 0.006, y + 0.035, z + 0.04, CAP, 0);
            b.box(x - 0.009, y - 0.012, z - 0.025, x + 0.009, y + 0.012, z + 0.025, ALU, 0);
        } else {
            b.box(x - 0.04, y - 0.035, z - 0.006, x + 0.04, y + 0.035, z + 0.006, CAP, 0);
            b.box(x - 0.025, y - 0.012, z - 0.009, x + 0.025, y + 0.012, z + 0.009, ALU, 0);
        }
    }

    /** Four swivel castors under a footprint, wheel rims and forks included. */
    private static void castors(ControlTowerRenderer.Builder b, double x, double z, double w, double dd, double y) {
        for (double cx : new double[]{x + 0.07, x + w - 0.07}) {
            for (double cz : new double[]{z + 0.07, z + dd - 0.07}) {
                b.box(cx - 0.045, y + 0.095, cz - 0.045, cx + 0.045, y + 0.11, cz + 0.045, ALU, 0);
                b.box(cx - 0.03, y + 0.05, cz - 0.022, cx - 0.02, y + 0.095, cz + 0.022, ALU, 0);
                b.box(cx + 0.02, y + 0.05, cz - 0.022, cx + 0.03, y + 0.095, cz + 0.022, ALU, 0);
                b.rod(cx - 0.018, y + 0.045, cz, cx + 0.018, y + 0.045, cz, 0.045, RUBBER, 3);
                b.rod(cx - 0.02, y + 0.045, cz, cx + 0.02, y + 0.045, cz, 0.018, CAP, 0);
            }
        }
    }

    // ------------------------------------------------------------------ seats and small things

    /** High stool: four splayed black legs, a foot ring and a round padded seat. */
    private static void stool(ControlTowerRenderer.Builder b, double x, double y, double z) {
        double seat = y + 0.74;
        double[][] feet = {{-0.16, -0.16}, {0.16, -0.16}, {0.16, 0.16}, {-0.16, 0.16}};
        for (int i = 0; i < 4; i++) {
            double[] f = feet[i], g = feet[(i + 1) % 4];
            b.rod(x + f[0], y, z + f[1], x + f[0] * 0.65, seat - 0.02, z + f[1] * 0.65, 0.014, 0xFF202326, 3);
            b.box(x + f[0] - 0.018, y, z + f[1] - 0.018, x + f[0] + 0.018, y + 0.012, z + f[1] + 0.018, RUBBER, 3);
            double k = 0.82;
            b.rod(x + f[0] * k, y + 0.28, z + f[1] * k, x + g[0] * k, y + 0.28, z + g[1] * k, 0.009, ALU, 0);
        }
        b.rod(x, seat - 0.03, z, x, seat, z, 0.15, 0xFF202326, 3);
        b.rod(x, seat, z, x, seat + 0.05, z, 0.165, 0xFF2D2F33, 3);
    }

    /** Roll of black gaffer tape on its cardboard core. */
    private static void gaffer(ControlTowerRenderer.Builder b, double x, double y, double z) {
        b.rod(x, y, z, x, y + 0.05, z, 0.055, 0xFF1C1E21, 3);
        b.rod(x, y + 0.001, z, x, y + 0.052, z, 0.036, 0xFFB08A5A, 3);
    }

    /** Two-way radio standing on the rack, aerial up and a green LED on. */
    private static void radio(ControlTowerRenderer.Builder b, double x, double y, double z) {
        b.box(x - 0.03, y, z - 0.018, x + 0.03, y + 0.15, z + 0.018, 0xFF151719, 3);
        b.box(x - 0.022, y + 0.07, z - 0.02, x + 0.022, y + 0.13, z - 0.018, 0xFF2E3237, 3);
        b.rod(x + 0.018, y + 0.15, z, x + 0.018, y + 0.29, z, 0.006, 0xFF151719, 3);
        b.rod(x - 0.012, y + 0.15, z, x - 0.012, y + 0.17, z, 0.01, 0xFF2E3237, 3);
        b.box(x - 0.004, y + 0.152, z - 0.012, x + 0.004, y + 0.16, z - 0.004, GREEN, 4);
    }

    /** Paper coffee cup with its lid. */
    private static void cup(ControlTowerRenderer.Builder b, double x, double y, double z) {
        b.rod(x, y, z, x, y + 0.1, z, 0.034, 0xFFF1EEE7, 3);
        b.rod(x, y + 0.04, z, x, y + 0.07, z, 0.036, 0xFFA0603A, 3);
        b.rod(x, y + 0.1, z, x, y + 0.112, z, 0.038, 0xFF2A2522, 3);
    }

    /** Gooseneck desk lamp clamped on the rack, head bent forward, lit underneath. */
    private static void lamp(ControlTowerRenderer.Builder b, double x, double y, double z) {
        b.box(x - 0.05, y, z - 0.04, x + 0.05, y + 0.02, z + 0.04, 0xFF1A1C1F, 3);
        double[][] neck = {{x, y + 0.02, z}, {x, y + 0.15, z - 0.01}, {x, y + 0.26, z - 0.06}, {x, y + 0.31, z - 0.15}, {x, y + 0.30, z - 0.22}};
        for (int i = 0; i + 1 < neck.length; i++) {
            double[] p = neck[i], q = neck[i + 1];
            b.rod(p[0], p[1], p[2], q[0], q[1], q[2], 0.008, 0xFF1A1C1F, 3);
        }
        double[] h = neck[neck.length - 1];
        b.box(h[0] - 0.03, h[1] - 0.045, h[2] - 0.05, h[0] + 0.03, h[1], h[2] + 0.03, 0xFF1A1C1F, 3);
        b.box(h[0] - 0.024, h[1] - 0.047, h[2] - 0.044, h[0] + 0.024, h[1] - 0.044, h[2] + 0.024, 0xFFFFF0C8, 4);
    }

    /** A loose coil of cable lying on the deck, three turns with a tail. */
    private static void coil(ControlTowerRenderer.Builder b, double x, double y, double z, double r) {
        int n = 16;
        for (int turn = 0; turn < 3; turn++) {
            double rr = r - turn * 0.012, yy = y + 0.012 + turn * 0.012;
            for (int i = 0; i < n; i++) {
                double a0 = Math.PI * 2 * i / n, a1 = Math.PI * 2 * (i + 1) / n;
                b.tube(x + rr * Math.cos(a0), yy, z + rr * Math.sin(a0) * 0.85,
                        x + rr * Math.cos(a1), yy, z + rr * Math.sin(a1) * 0.85, 0.011, CORD, 3);
            }
        }
        b.rod(x + r, y + 0.012, z, x + r + 0.35, y + 0.012, z + 0.12, 0.011, CORD, 3);
    }

    /**
     * Three cables from the back of the racks across the deck to the nearest back standard, then down it to the
     * ground, cable-tied every 60 cm.
     */
    private static void loom(ControlTowerRenderer.Builder b, double x, double y, double z, double d) {
        double bay = ControlTowerBlock.BAY;
        double sx = Math.round(x / bay) * bay;
        double out = 0.07;
        for (int i = 0; i < 3; i++) {
            double o = (i - 1) * 0.025;
            int col = i == 1 ? 0xFF2C3A55 : CORD;
            double r = i == 1 ? 0.016 : 0.011;
            b.rod(x + o, y + 0.13, z, x + o, y + 0.015 + i * 0.01, z + 0.06, r, col, 3);
            b.rod(x + o, y + 0.015 + i * 0.01, z + 0.06, sx + o, y + 0.015 + i * 0.01, d - 0.02, r, col, 3);
            b.rod(sx + o, y + 0.015 + i * 0.01, d - 0.02, sx + o, y + 0.05, d + out, r, col, 3);
            b.rod(sx + o, y + 0.05, d + out, sx + o, 0.25, d + out, r, col, 3);
            b.rod(sx + o, 0.25, d + out, sx + o + 0.3, 0.012, d + out + 0.25, r, col, 3);
        }
        for (double ty = 0.6; ty < y; ty += 0.6) {
            b.box(sx - 0.05, ty - 0.008, d + out - 0.022, sx + 0.05, ty + 0.008, d + out + 0.022, 0xFFEDEDED, 3);
        }
    }
}
