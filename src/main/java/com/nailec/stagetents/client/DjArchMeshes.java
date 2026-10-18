package com.nailec.stagetents.client;

import com.nailec.stagetents.tent.DjArchShape;
import net.minecraft.util.Mth;

import java.util.Arrays;

/**
 * Saddlespan-style DJ cover: square plinths and straight legs, then two identical tubes leaning out front and back
 * into a leaf. The clear PVC saddle between them has a welded hem with eyelets, laced to the tubes by black straps,
 * and its points are strapped down to the fork sleeves. Steel couplings join the tube sections.
 */
final class DjArchMeshes {
    private static final int RUBBER = 0xFF14181C;
    private static final int STEEL = 0xFF5E656B;
    private static final int GALV = 0xFF9EA4A8;
    private static final int PIN = 0xFFC9CED2;
    private static final int LAMP = 0xFFFFF1C2;
    /** Welded PVC hem round the sheet. */
    private static final int HEM = 0xFFE4E8EA;
    /** Spacing of the eyelets along the hem, and so of the lacing straps. */
    private static final double EYELET_STEP = 0.45;

    private DjArchMeshes() {}

    static void build(MeshBuilder m, DjArchShape g) {
        int pipe = MeshBuilder.opaque(m.p.colorA);
        int seam = 0xB4000000 | (MeshBuilder.shade(0xFF000000 | m.p.colorB, 0.80F) & 0xFFFFFF);
        double tubeR = Mth.clamp(0.08 + g.span * 0.003, 0.09, 0.14);
        int n = Mth.clamp((int) Math.round((g.span + 2 * g.H) / 0.22), 40, 96);

        // ---- The two arches. Each lies in a plane through the feet tilted by the lean; the sweep's side vector is
        // that plane's normal.
        double rise = g.H - g.leg;
        double[][][] arches = new double[2][n + 1][];
        double[][] sides = new double[2][];
        for (int s = 0; s < 2; s++) {
            int side = s == 0 ? 1 : -1;
            double l = Math.hypot(g.lean, rise);
            sides[s] = new double[]{rise / l, -side * g.lean / l, 0};
            for (int k = 0; k <= n; k++) arches[s][k] = g.arch(-1 + 2.0 * k / n, side);
            sweep(m, arches[s], tubeR, pipe, 16, sides[s]);
        }

        // ---- Feet: square plinth, bolted flange, straight leg, base collar, and the fork sleeve where the arches split.
        double pl = DjArchShape.PLINTH;
        for (double zf : new double[]{0, g.span}) {
            m.box(-0.45, 0.0, zf - 0.45, 0.45, pl - 0.03, zf + 0.45, STEEL, false);
            m.box(-0.47, pl - 0.03, zf - 0.47, 0.47, pl, zf + 0.47, GALV, false);
            m.box(-0.30, pl, zf - 0.30, 0.30, pl + 0.04, zf + 0.30, GALV, false);
            for (double bx : new double[]{-0.22, 0.22}) {
                for (double bz : new double[]{-0.22, 0.22}) {
                    m.box(bx - 0.03, pl + 0.04, zf + bz - 0.03, bx + 0.03, pl + 0.06, zf + bz + 0.03, STEEL, false);
                    m.box(bx - 0.012, pl + 0.06, zf + bz - 0.012, bx + 0.012, pl + 0.085, zf + bz + 0.012, PIN, false);
                }
            }
            // Four gussets between the leg and the flange.
            double gr = tubeR - 0.01;
            m.box(gr, pl + 0.04, zf - 0.012, 0.29, pl + 0.16, zf + 0.012, STEEL, false);
            m.box(-0.29, pl + 0.04, zf - 0.012, -gr, pl + 0.16, zf + 0.012, STEEL, false);
            m.box(-0.012, pl + 0.04, zf + gr, 0.012, pl + 0.16, zf + 0.29, STEEL, false);
            m.box(-0.012, pl + 0.04, zf - 0.29, 0.012, pl + 0.16, zf - gr, STEEL, false);
            m.tube(0, pl + 0.04, zf, 0, g.leg + 0.05, zf, tubeR, 16, pipe);
            m.tube(0, pl + 0.04, zf, 0, pl + 0.20, zf, tubeR + 0.04, 16, STEEL);
            m.tube(0, g.leg - 0.30, zf, 0, g.leg + 0.02, zf, tubeR + 0.045, 16, STEEL);
        }
        for (int s = 0; s < 2; s++) {
            for (int end = 0; end < 2; end++) {
                int k0 = end == 0 ? 0 : n, k1 = end == 0 ? 2 : n - 2;
                double[] a = arches[s][k0], b = arches[s][k1];
                m.tube(a[0], a[1], a[2], b[0], b[1], b[2], tubeR + 0.035, 16, STEEL);
            }
        }

        // ---- Sheet: the saddle between the arches, inside the lacing gaps, seen from both sides.
        int pvc = 0x8C000000 | m.p.colorB;
        int underside = 0x64000000 | m.p.colorB;
        int across = Mth.clamp((int) Math.round(g.lean * 6), 10, 24);
        for (int k = 0; k < n; k++) {
            double u0 = -1 + 2.0 * k / n, u1 = -1 + 2.0 * (k + 1) / n;
            for (int j = 0; j < across; j++) {
                double t0 = j / (double) across, t1 = (j + 1.0) / across;
                double[] p00 = g.sheet(u0, t0), p01 = g.sheet(u0, t1);
                double[] p11 = g.sheet(u1, t1), p10 = g.sheet(u1, t0);
                double[] nrm = normal(p00, p01, p11, p10);
                double[][] q = {
                        MeshBuilder.v(p00[0], p00[1], p00[2], t0, k / (double) n, nrm[0], nrm[1], nrm[2]),
                        MeshBuilder.v(p01[0], p01[1], p01[2], t1, k / (double) n, nrm[0], nrm[1], nrm[2]),
                        MeshBuilder.v(p11[0], p11[1], p11[2], t1, (k + 1.0) / n, nrm[0], nrm[1], nrm[2]),
                        MeshBuilder.v(p10[0], p10[1], p10[2], t0, (k + 1.0) / n, nrm[0], nrm[1], nrm[2])
                };
                m.twoSided(q, pvc, underside, MeshBuilder.GLASS);
            }
        }

        // ---- Welded seams across the sheet, about every 1.5 m between the feet, and one down the middle.
        int seams = Mth.clamp((int) Math.round(g.span / 1.5), 4, 16);
        for (int i = 1; i < seams; i++) acrossLine(m, g, -1 + 2.0 * i / seams, 0.012, 0.010, 3, seam);
        alongLine(m, g, 0.5, 0.012, 0.010, 3, seam, n);

        // ---- Hem, eyelets and lacing: a white welded hem round the sheet, a steel eyelet every EYELET_STEP, and a
        // black strap from each eyelet that wraps the tube.
        for (int s = 0; s < 2; s++) {
            int side = s == 0 ? 1 : -1;
            double edge = s == 0 ? 0 : 1;
            double[] prev = null;
            double run = 0, next = EYELET_STEP * 0.5;
            for (int k = 0; k <= 4 * n; k++) {
                double u = -1 + 2.0 * k / (4 * n);
                if (g.inset(u) >= 0.5) { prev = null; continue; }
                double[] h = g.sheet(u, edge);
                if (prev != null) {
                    m.tube(prev[0], prev[1], prev[2], h[0], h[1], h[2], 0.022, 6, HEM);
                    run += dist(prev, h);
                }
                prev = h;
                if (run < next) continue;
                next += EYELET_STEP;
                double[] a = g.arch(u, side);
                double[] in = unit(new double[]{h[0] - a[0], h[1] - a[1], h[2] - a[2]});
                // Eyelet ring on the hem, then the strap up to the tube and its wrap round the tube.
                m.box(h[0] - 0.028, h[1] - 0.012, h[2] - 0.028, h[0] + 0.028, h[1] + 0.024, h[2] + 0.028, PIN, false);
                double[] onTube = {a[0] + in[0] * tubeR, a[1] + in[1] * tubeR, a[2] + in[2] * tubeR};
                m.tube(h[0], h[1] + 0.01, h[2], onTube[0], onTube[1], onTube[2], 0.012, 4, RUBBER);
                double[] ta = g.arch(u - 0.004, side), tb = g.arch(u + 0.004, side);
                double[] dir = unit(new double[]{tb[0] - ta[0], tb[1] - ta[1], tb[2] - ta[2]});
                double[] w0 = {a[0] - dir[0] * 0.022, a[1] - dir[1] * 0.022, a[2] - dir[2] * 0.022};
                double[] w1 = {a[0] + dir[0] * 0.022, a[1] + dir[1] * 0.022, a[2] + dir[2] * 0.022};
                sweep(m, new double[][]{w0, w1}, tubeR + 0.006, RUBBER, 12, sides[s]);
            }
        }

        // ---- Tips: where the sheet narrows to a point near each fork, a D-ring and a strap down to the sleeve.
        for (int end = 0; end < 2; end++) {
            double u = end == 0 ? -1 : 1, du = end == 0 ? 0.002 : -0.002;
            while (g.inset(u) >= 0.5 && Math.abs(u) > 0.01) u += du;
            double[] tip = g.sheet(u, 0.5);
            double zf = end == 0 ? 0 : g.span;
            m.box(tip[0] - 0.035, tip[1] - 0.035, tip[2] - 0.035, tip[0] + 0.035, tip[1] + 0.035, tip[2] + 0.035, PIN, false);
            m.tube(tip[0], tip[1], tip[2], 0, g.leg + 0.02, zf, 0.018, 4, RUBBER);
        }

        // ---- Couplings: a steel sleeve on each tube every "joint spacing" along the arch.
        double step = 0.5 + 0.5 * m.p.poleSpacing;
        for (int s = 0; s < 2; s++) {
            double run = 0, next = step;
            for (int k = 1; k < n; k++) {
                double[] p = arches[s][k - 1], q = arches[s][k];
                run += dist(p, q);
                if (run < next) continue;
                next += step;
                if (k < 6 || k > n - 6) continue;
                double[] dir = unit(new double[]{arches[s][k + 1][0] - p[0], arches[s][k + 1][1] - p[1], arches[s][k + 1][2] - p[2]});
                double[] b0 = {q[0] - dir[0] * 0.012, q[1] - dir[1] * 0.012, q[2] - dir[2] * 0.012};
                double[] b1 = {q[0] + dir[0] * 0.012, q[1] + dir[1] * 0.012, q[2] + dir[2] * 0.012};
                double[] c0 = {q[0] - dir[0] * 0.06, q[1] - dir[1] * 0.06, q[2] - dir[2] * 0.06};
                double[] c1 = {q[0] + dir[0] * 0.06, q[1] + dir[1] * 0.06, q[2] + dir[2] * 0.06};
                sweep(m, new double[][]{c0, c1}, tubeR + 0.014, STEEL, 16, sides[s]);
                sweep(m, new double[][]{b0, b1}, tubeR + 0.020, STEEL, 16, sides[s]);
            }
        }

        if (m.p.rigging) {
            for (int s = 0; s < 2; s++) {
                for (int j = n / 6; j < n - n / 6; j += Math.max(4, n / 8)) {
                    double[] p = arches[s][j];
                    double y = p[1] - tubeR;
                    m.box(p[0] - 0.05, y - 0.16, p[2] - 0.06, p[0] + 0.05, y - 0.02, p[2] + 0.06, RUBBER, false);
                    m.box(p[0] - 0.035, y - 0.22, p[2] - 0.045, p[0] + 0.035, y - 0.12, p[2] + 0.045, LAMP, true);
                }
            }
        }
    }

    private static void sweep(MeshBuilder m, double[][] path, double r, int col, int sides, double[] side) {
        double[] radius = new double[path.length];
        int[] colors = new int[path.length];
        Arrays.fill(radius, r);
        Arrays.fill(colors, col);
        m.sweep(path, radius, colors, sides, side[0], side[1], side[2]);
    }

    /** Line lying on the sheet from the front hem to the back hem at one point of the span. */
    private static void acrossLine(MeshBuilder m, DjArchShape g, double u, double lift, double r, int sides, int col) {
        int n = 12;
        double[] p = g.sheet(u, 0);
        for (int j = 1; j <= n; j++) {
            double[] q = g.sheet(u, j / (double) n);
            m.tube(p[0], p[1] + lift, p[2], q[0], q[1] + lift, q[2], r, sides, col);
            p = q;
        }
    }

    /** Line lying on the sheet from tip to tip at one point across. */
    private static void alongLine(MeshBuilder m, DjArchShape g, double t, double lift, double r, int sides, int col, int n) {
        double[] p = g.sheet(-1, t);
        for (int k = 1; k <= n; k++) {
            double[] q = g.sheet(-1 + 2.0 * k / n, t);
            m.tube(p[0], p[1] + lift, p[2], q[0], q[1] + lift, q[2], r, sides, col);
            p = q;
        }
    }

    /** Upward normal of a membrane quad. */
    private static double[] normal(double[] p00, double[] p01, double[] p11, double[] p10) {
        double ax = p11[0] - p00[0], ay = p11[1] - p00[1], az = p11[2] - p00[2];
        double bx = p10[0] - p01[0], by = p10[1] - p01[1], bz = p10[2] - p01[2];
        double nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
        double l = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (l < 1e-9) return new double[]{0, 1, 0};
        if (ny < 0) l = -l;
        return new double[]{nx / l, ny / l, nz / l};
    }

    private static double dist(double[] a, double[] b) {
        return Math.sqrt((a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1]) + (a[2] - b[2]) * (a[2] - b[2]));
    }

    private static double[] unit(double[] v) {
        double l = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return l < 1e-9 ? new double[]{0, 1, 0} : new double[]{v[0] / l, v[1] / l, v[2] / l};
    }
}
