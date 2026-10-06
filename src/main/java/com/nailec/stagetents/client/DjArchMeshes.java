package com.nailec.stagetents.client;

import com.nailec.stagetents.tent.DjArchShape;
import net.minecraft.util.Mth;

import java.util.Arrays;

/**
 * Two poles, one curved tube between them, and a PVC sail off both sides of that tube.
 */
final class DjArchMeshes {
    private static final int RUBBER = 0xFF14181C;
    private static final int LAMP = 0xFFFFF1C2;

    private DjArchMeshes() {}

    static void build(MeshBuilder m, DjArchShape g) {
        int pipe = MeshBuilder.opaque(m.p.colorA);
        int seam = MeshBuilder.shade(0xFF000000 | m.p.colorB, 0.72F);
        double rise = Math.max(0.6, g.H - g.legHeight);
        double poleR = 0.10;
        double lipR = Mth.clamp(0.16 + g.span * 0.018, 0.18, 0.28);
        int bays = Mth.clamp((int) Math.round(Math.PI * 0.5 * (g.span + rise) / 0.42), 16, 32);
        int rings = bays + 1;
        // The tube stands between the poles. The sail leaves it toward the front and the back.
        double xPole = 0;
        double xPos = g.ax - 0.06;
        double xNeg = -g.ax + 0.06;
        double cut = Math.asin(DjArchShape.SAIL_SIN);

        double[][] lip = new double[rings][3];
        double[] lipRadius = new double[rings];
        int[] lipColor = new int[rings];
        Arrays.fill(lipRadius, lipR);
        Arrays.fill(lipColor, pipe);
        for (int k = 0; k < rings; k++) {
            double a = Math.PI * k / bays;
            lip[k][0] = xPole;
            lip[k][1] = g.y(a);
            lip[k][2] = g.z(a);
        }
        m.sweep(lip, lipRadius, lipColor, 8, 1, 0, 0);

        for (int sz : new int[]{-1, 1}) {
            double z = sz * g.span;
            m.tube(xPole, 0.05, z, xPole, g.legHeight, z, poleR, 8, pipe);
            m.box(xPole - 0.26, 0.02, z - 0.26, xPole + 0.26, 0.07, z + 0.26, pipe, false);
            m.tube(xPole, g.legHeight - 0.05, z, xPole, g.legHeight + 0.02, z, poleR + 0.025, 8, pipe);
        }

        hem(m, g, bays, xPos, cut, Math.PI - cut, pipe);
        hem(m, g, bays, xNeg, cut, Math.PI - cut, pipe);
        edge(m, g, xNeg, xPos, cut, pipe);
        edge(m, g, xNeg, xPos, Math.PI - cut, pipe);

        int pvc = 0x8C000000 | m.p.colorB;
        int underside = 0x64000000 | m.p.colorB;
        int depth = Mth.clamp((int) Math.round(g.ax * 4.0), 8, 16);
        for (int k = 0; k < bays; k++) {
            double a0 = Math.PI * k / bays, a1 = Math.PI * (k + 1) / bays;
            if (!g.sailCovers(a0) || !g.sailCovers(a1)) continue;
            for (int j = 0; j < depth; j++) {
                double u0 = j / (double) depth, u1 = (j + 1.0) / depth;
                double x0 = xNeg + (xPos - xNeg) * u0;
                double x1 = xNeg + (xPos - xNeg) * u1;
                double y0a = g.sailY(x0, a0), y1a = g.sailY(x1, a0);
                double y1b = g.sailY(x1, a1), y0b = g.sailY(x0, a1);
                double dx = x1 - x0;
                double dz = g.z(a1) - g.z(a0);
                double dyx = ((y1a - y0a) + (y1b - y0b)) * 0.5 / dx;
                double dyz = Math.abs(dz) < 1e-4 ? 0 : ((y0b - y0a) + (y1b - y1a)) * 0.5 / dz;
                double nx = -dyx, ny = 1, nz = -dyz;
                double nl = Math.sqrt(nx * nx + ny * ny + nz * nz);
                double[][] q = {
                        MeshBuilder.v(x0, y0a, g.z(a0), u0, k / (double) bays, nx / nl, ny / nl, nz / nl),
                        MeshBuilder.v(x1, y1a, g.z(a0), u1, k / (double) bays, nx / nl, ny / nl, nz / nl),
                        MeshBuilder.v(x1, y1b, g.z(a1), u1, (k + 1.0) / bays, nx / nl, ny / nl, nz / nl),
                        MeshBuilder.v(x0, y0b, g.z(a1), u0, (k + 1.0) / bays, nx / nl, ny / nl, nz / nl)
                };
                m.twoSided(q, pvc, underside, MeshBuilder.GLASS);
            }
        }

        int seams = Mth.clamp((int) Math.round(g.span * 0.9), 3, 6);
        for (int i = 1; i < seams; i++) {
            double a = cut + (Math.PI - 2 * cut) * i / seams;
            double px = xNeg, py = g.sailY(xNeg, a) + 0.02, pz = g.z(a);
            for (int j = 1; j <= depth; j++) {
                double x = xNeg + (xPos - xNeg) * j / depth;
                double y = g.sailY(x, a) + 0.02;
                m.tube(px, py, pz, x, y, g.z(a), 0.012, 3, seam);
                px = x;
                py = y;
                pz = g.z(a);
            }
        }

        if (m.p.rigging) {
            for (int j = 2; j < bays; j += 4) {
                double[] p = lip[j];
                m.box(p[0] - 0.05, p[1] - lipR - 0.16, p[2] - 0.06, p[0] + 0.05, p[1] - lipR - 0.02, p[2] + 0.06, RUBBER, false);
                m.box(p[0] - 0.035, p[1] - lipR - 0.22, p[2] - 0.045, p[0] + 0.035, p[1] - lipR - 0.12, p[2] + 0.045, LAMP, true);
            }
        }
    }

    /** Curved hem along one end of the sail, between the two open edges. */
    private static void hem(MeshBuilder m, DjArchShape g, int steps, double x, double a0, double a1, int color) {
        int rings = steps + 1;
        double[][] path = new double[rings][3];
        double[] radius = new double[rings];
        int[] colors = new int[rings];
        Arrays.fill(radius, 0.04);
        Arrays.fill(colors, color);
        for (int k = 0; k < rings; k++) {
            double a = a0 + (a1 - a0) * k / steps;
            path[k][0] = x;
            path[k][1] = g.sailY(x, a);
            path[k][2] = g.z(a);
        }
        m.sweep(path, radius, colors, 6, 1, 0, 0);
    }

    /** Tube along one side of the sheet, out to both edges of the pole. */
    private static void edge(MeshBuilder m, DjArchShape g, double x0, double x1, double angle, int color) {
        int n = 8;
        double px = x0, py = g.sailY(x0, angle), pz = g.z(angle);
        for (int i = 1; i <= n; i++) {
            double x = x0 + (x1 - x0) * i / n;
            double y = g.sailY(x, angle);
            double z = g.z(angle);
            m.tube(px, py, pz, x, y, z, 0.045, 6, color);
            px = x;
            py = y;
            pz = z;
        }
    }
}
