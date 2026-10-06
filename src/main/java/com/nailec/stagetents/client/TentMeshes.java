package com.nailec.stagetents.client;

import com.nailec.stagetents.tent.ArchShape;
import com.nailec.stagetents.tent.DjArchShape;
import com.nailec.stagetents.tent.BigTopShape;
import com.nailec.stagetents.tent.FloorMode;
import com.nailec.stagetents.tent.FrameShape;
import com.nailec.stagetents.tent.GazeboShape;
import com.nailec.stagetents.tent.RectShape;
import com.nailec.stagetents.tent.StretchShape;
import com.nailec.stagetents.tent.TensileShape;
import com.nailec.stagetents.tent.LightMode;
import com.nailec.stagetents.tent.PagodaShape;
import com.nailec.stagetents.tent.TentParams;
import com.nailec.stagetents.tent.TentShape;
import com.nailec.stagetents.tent.WallMode;
import net.minecraft.util.Mth;

import java.util.function.DoubleUnaryOperator;

/** Builds the mesh of each kind of tent out of shared parts (radial roof, walls, valance, ropes, festoons). */
final class TentMeshes {
    private static final int POLE = 0xFFB8BCC0;
    private static final int ALU = 0xFFC6CACE;
    private static final int RING = 0xFF4A4E52;
    private static final int ROPE = 0xFFC9B58C;
    private static final int EDGE_ROPE = 0xFFE6DFCC;
    private static final int WOOD = 0xFF8C6A46;
    private static final int SHELF = 0xFF3B2A1E;
    private static final int STRAP = 0xFF2E3A55;
    private static final int STAKE = 0xFF6B5A45;
    private static final int GLASS = 0x5AD8E4EC;
    private static final int[] PARTY = {0xFFFF4A3A, 0xFFFFD040, 0xFF4CFF6A, 0xFF4AA8FF, 0xFFFF62E0, 0xFFFF9A30};

    private TentMeshes() {}

    static MeshBuilder build(TentShape g, int version) {
        MeshBuilder m = new MeshBuilder(g, version);
        if (g instanceof BigTopShape b) bigTop(m, b);
        else if (g instanceof PagodaShape pg) pagoda(m, pg);
        else if (g instanceof FrameShape f) frame(m, f);
        else if (g instanceof GazeboShape gz) gazebo(m, gz);
        else if (g instanceof ArchShape a) arch(m, a);
        else if (g instanceof DjArchShape a) DjArchMeshes.build(m, a);
        else if (g instanceof StretchShape st) stretch(m, st);
        if (g.params.type.hasInterior()) {
            floor(m, g);
            stage(m, g);
        }
        if (g instanceof RectShape r) {
            gutters(m, r);
            if (m.p.sign) signBoard(m, r);
            if (m.p.poleCurtains) poleCurtains(m, r);
        }
        return m.done();
    }

    static int bulbColor(LightMode mode, int i) {
        return mode == LightMode.PARTY ? PARTY[Math.floorMod(i, PARTY.length)] : 0xFFFFD58A;
    }

    // ------------------------------------------------------------------ tent kinds

    private static void bigTop(MeshBuilder m, BigTopShape g) {
        radialRoof(m, g, g::ridgeHeight, BigTopShape::profile, BigTopShape.SEAM_SAG + 0.004 * g.R, 1.0);
        walls(m, g, 0.12);
        entranceFlaps(m, g);
        valance(m, g, 1.0);
        edgeRope(m, g);
        for (double[] q : g.quarterPoles()) {
            m.tube(q[0], 0, q[1], q[0], q[2], q[1], 0.06, 6, POLE);
            m.box(q[0], q[2] - 0.06, q[1], 0.1, q[2], RING, false);
        }
        for (double mx : g.mastX) {
            double top = g.ridgeHeight(mx);
            m.box(mx, m.p.showPlate ? 0.19 : 0, 0, 0.16, top + 1.4, POLE, false);
            m.box(mx, top - 0.12, 0, 0.42, top + 0.22, RING, false);
        }
        double[] per = new double[5];
        for (double s : g.poleStations()) {
            g.perimeterPoint(s, per);
            double px = per[0] - per[2] * 0.08, pz = per[1] - per[3] * 0.08;
            m.tube(px, 0, pz, px, g.Hw + 0.3, pz, 0.07, 6, POLE);
        }
        guyRopes(m, g, false);
        if (m.p.lights != LightMode.OFF) {
            eaveFestoon(m, g, 0.6);
            double[] st = g.poleStations();
            int seed = 0;
            for (int k = 0; k < st.length; k += 2) {
                g.perimeterPoint(st[k], per);
                double ex = per[0] - per[2] * 0.6, ez = per[1] - per[3] * 0.6;
                double mx = nearestMast(g, ex);
                m.festoon(mx, g.Hw + (g.ridgeHeight(mx) - g.Hw) * 0.55, 0, ex, g.Hw - 0.4, ez, 0.6, seed += 7);
            }
        }
    }

    private static double nearestMast(BigTopShape g, double x) {
        double best = g.mastX[0];
        for (double mx : g.mastX) if (Math.abs(mx - x) < Math.abs(best - x)) best = mx;
        return best;
    }

    private static void pagoda(MeshBuilder m, PagodaShape g) {
        radialRoof(m, g, x -> g.H, PagodaShape::profile, 0, 0.35);
        walls(m, g, 0.03);
        entranceFlaps(m, g);
        valance(m, g, 0.7);
        edgeRope(m, g);
        double[] per = new double[5];
        for (double s : g.poleStations()) {
            g.perimeterPoint(s, per);
            double ix = per[0] - Math.signum(per[0]) * 0.09, iz = per[1] - Math.signum(per[1]) * 0.09;
            m.box(ix, 0, iz, 0.075, g.Hw, ALU, false);
            m.box(ix, 0, iz, 0.16, 0.04, RING, false); // foot plate
        }
        eaveBeams(m, g, g.ax, g.az);
        for (double sx : g.hl > 0 ? new double[]{-g.hl, g.hl} : new double[]{0}) {
            m.tube(sx, g.H - 0.3, 0, sx, g.H + 0.65, 0, 0.04, 6, POLE);
            m.box(sx, g.H + 0.62, 0, 0.08, g.H + 0.78, ALU, false);
        }
        guyRopes(m, g, true);
        if (m.p.lights != LightMode.OFF) {
            eaveFestoon(m, g, 0.4);
            double cy = Math.max(g.Hw + 0.4, g.H - 1.6);
            int seed = 3;
            for (double[] c : new double[][]{{1, 1}, {-1, 1}, {-1, -1}, {1, -1}}) {
                m.festoon(Math.signum(c[0]) * g.hl, cy, 0, c[0] * (g.ax - 0.4), g.Hw - 0.35, c[1] * (g.az - 0.4), 0.35, seed += 5);
            }
        }
    }

    private static void frame(MeshBuilder m, FrameShape g) {
        TentParams p = m.p;
        double rise = g.H - g.Hw;
        double slant = Math.hypot(g.az, rise);
        int nv = Math.max(3, (int) Math.ceil(g.az));
        int colsPerBay = Math.max(2, (int) Math.round(2 * g.ax / g.bays / 0.625));
        for (int side : new int[]{1, -1}) {
            for (int k = 0; k < g.bays; k++) {
                double bx0 = g.bayX(k), bx1 = g.bayX(k + 1), bl = bx1 - bx0;
                double sag = 0.05 * bl / 5;
                for (int i = 0; i < colsPerBay; i++) {
                    double x0 = bx0 + bl * i / colsPerBay, x1 = bx0 + bl * (i + 1) / colsPerBay;
                    int col = m.stripe((k * colsPerBay + i) / 2, false);
                    for (int j = 0; j < nv; j++) {
                        double v0 = j / (double) nv, v1 = (j + 1) / (double) nv;
                        double[][] q = {
                                panVert(g, side, x0, v0, bx0, bl, sag, slant), panVert(g, side, x1, v0, bx0, bl, sag, slant),
                                panVert(g, side, x1, v1, bx0, bl, sag, slant), panVert(g, side, x0, v1, bx0, bl, sag, slant)};
                        m.twoSided(q, col);
                    }
                }
            }
        }
        // Gable triangles above the eave.
        int gc = Math.max(4, 2 * (int) Math.ceil(g.az / 0.625));
        for (int end : new int[]{1, -1}) {
            double x = end * g.ax;
            for (int i = 0; i < gc; i++) {
                double z0 = -g.az + 2 * g.az * i / gc, z1 = -g.az + 2 * g.az * (i + 1) / gc;
                double t0 = g.Hw + rise * (1 - Math.abs(z0) / g.az), t1 = g.Hw + rise * (1 - Math.abs(z1) / g.az);
                int col = m.stripe(i / 2, false);
                verticalStrip(m, x, z0, x, z1, end, 0, g.Hw, g.Hw, t0, t1, col, MeshBuilder.SOLID);
            }
        }
        walls(m, g, 0.02);
        entranceFlaps(m, g);
        valance(m, g, 0.6);
        // Aluminium frame: legs (gable posts run up to the roof), eave beams, rafters, ridge.
        double[] per = new double[5];
        for (double s : g.poleStations()) {
            g.perimeterPoint(s, per);
            double ix = per[0] - Math.signum(per[0]) * 0.1, iz = per[1] - Math.signum(per[1]) * 0.1;
            double top = Math.abs(per[0]) >= g.ax - 1e-6 && Math.abs(per[1]) < g.az - 1e-6
                    ? g.roofHeight(ix, iz) - 0.12 : g.Hw;
            m.box(ix, 0, iz, 0.08, top, ALU, false);
            m.box(ix, 0, iz, 0.17, 0.04, RING, false);
        }
        eaveBeams(m, g, g.ax, g.az);
        for (int k = 0; k <= g.bays; k++) {
            double x = Mth.clamp(g.bayX(k), -g.ax + 0.1, g.ax - 0.1);
            m.tube(x, g.Hw - 0.08, g.az - 0.08, x, g.H - 0.14, 0, 0.07, 4, ALU);
            m.tube(x, g.Hw - 0.08, -g.az + 0.08, x, g.H - 0.14, 0, 0.07, 4, ALU);
        }
        m.tube(-g.ax + 0.1, g.H - 0.14, 0, g.ax - 0.1, g.H - 0.14, 0, 0.07, 4, ALU);
        guyRopes(m, g, false);
        if (p.lights != LightMode.OFF) {
            int seed = 0;
            for (int k = 0; k < g.bays; k++) {
                double x = (g.bayX(k) + g.bayX(k + 1)) / 2;
                m.festoon(x, g.Hw - 0.3, g.az - 0.4, x, g.H - 0.75, 0, 0.35, seed += 4);
                m.festoon(x, g.H - 0.75, 0, x, g.Hw - 0.3, -g.az + 0.4, 0.35, seed += 4);
            }
            m.festoon(-g.ax + 0.4, g.H - 0.6, 0, g.ax - 0.4, g.H - 0.6, 0, 0.25, 11);
        }
    }

    private static double[] panVert(FrameShape g, int side, double x, double v, double bx0, double bl, double sag, double slant) {
        double rise = g.H - g.Hw;
        double z = side * g.az * (1 - v);
        double y = g.Hw + rise * v - sag * Math.sin(Math.PI * (x - bx0) / bl) * Math.sin(Math.PI * v);
        // Normal of the pan plane, tilted a touch by the sag so the canvas reads as tensioned between rafters.
        double dydx = -sag * Math.PI / bl * Math.cos(Math.PI * (x - bx0) / bl) * Math.sin(Math.PI * v);
        double nx = -dydx, ny = 1, nz = side * rise / g.az;
        double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
        double flex = 0.6 * Math.sin(Math.PI * (x - bx0) / bl) * Math.sin(Math.PI * v);
        return MeshBuilder.vf(x, y, z, x / 2, v * slant / 2, nx / len, ny / len, nz / len, flex);
    }

    private static void gazebo(MeshBuilder m, GazeboShape g) {
        radialRoof(m, g, x -> g.H, GazeboShape::profile, 0, 0.3);
        walls(m, g, 0.02);
        entranceFlaps(m, g);
        valance(m, g, 0.45);
        double[] st = g.poleStations();
        double[] a = new double[5], b = new double[5];
        for (int k = 0; k < st.length; k++) {
            g.perimeterPoint(st[k], a);
            g.perimeterPoint(k + 1 < st.length ? st[k + 1] : g.perimeterLength(), b);
            double ax = a[0] - Math.signum(a[0]) * 0.06, az = a[1] - Math.signum(a[1]) * 0.06;
            double bx = b[0] - Math.signum(b[0]) * 0.06, bz = b[1] - Math.signum(b[1]) * 0.06;
            m.box(ax, 0, az, 0.05, g.Hw, ALU, false);
            m.box(ax, 0, az, 0.12, 0.03, RING, false);
            // Scissor truss between two legs.
            double top = g.Hw - 0.04, low = g.Hw - 0.55;
            m.tube(ax, top, az, bx, low, bz, 0.025, 4, ALU);
            m.tube(ax, low, az, bx, top, bz, 0.025, 4, ALU);
            m.tube(ax, top, az, bx, top, bz, 0.03, 4, ALU);
        }
        // Roof ribs from the corners up to the ridge.
        double y = g.H - 0.06;
        for (double[] c : new double[][]{{1, 1}, {-1, 1}, {-1, -1}, {1, -1}}) {
            m.tube(c[0] * (g.ax - 0.06), g.Hw - 0.04, c[1] * (g.az - 0.06), c[0] * g.ridge, y, 0, 0.03, 4, ALU);
        }
        if (g.ridge > 0) m.tube(-g.ridge, y, 0, g.ridge, y, 0, 0.035, 4, ALU);
        m.box(g.ridge, y - 0.05, 0, 0.07, y + 0.06, RING, false);
        guyRopes(m, g, true);
        if (m.p.lights != LightMode.OFF) eaveFestoon(m, g, 0.3);
    }

    private static void arch(MeshBuilder m, ArchShape g) {
        int n = 64;
        double arc = Math.PI * (g.span + g.H) / 2;
        int chambers = Math.max(5, (int) Math.round(arc / 1.3));
        double[][] path = new double[n + 1][];
        double[] radius = new double[n + 1];
        int[] colors = new int[n + 1];
        for (int i = 0; i <= n; i++) {
            double th = Math.PI * i / n;
            double[] c = g.centre(th);
            path[i] = new double[]{0, c[1], c[0]};
            double f = (double) i / n * chambers;
            // Each chamber puffs out between pinched seams.
            radius[i] = g.tube * (0.86 + 0.14 * Math.sin(Math.PI * (f - Math.floor(f))));
            colors[i] = m.stripe((int) Math.floor(Math.min(f, chambers - 1e-6)), false);
        }
        m.sweep(path, radius, colors, 16, 1, 0, 0);
        double zf = g.span - g.tube;
        for (double z : new double[]{zf, -zf}) m.box(0, 0, z, g.tube + 0.25, 0.35, 0xFF3A3C40, false);
        if (m.p.guyRopes) {
            for (double th : new double[]{Math.PI / 4, 3 * Math.PI / 4}) {
                double[] c = g.centre(th);
                for (int side : new int[]{1, -1}) {
                    double sx = side * (g.H * 0.6 + 1);
                    m.rope(side * g.tube * 0.8, c[1], c[0], sx, 0.12, c[0] * 1.1, 0.04 * g.H, 0.02, ROPE);
                    m.box(sx, -0.1, c[0] * 1.1, 0.05, 0.3, STAKE, false);
                }
            }
        }
    }

    private static void stretch(MeshBuilder m, StretchShape g) {
        double P = g.perimeterLength();
        // Resolution grows with the membrane: arenas need more rows to keep the cones round.
        int ns = Mth.clamp((int) Math.ceil(P / 0.5), 48, 720);
        int nt = Mth.clamp((int) Math.round(g.footprint() * 0.45), 22, 56);
        double cx = g.centreX(), cz = g.centreZ();
        double[][] gx = new double[ns + 1][nt + 1], gy = new double[ns + 1][nt + 1], gz = new double[ns + 1][nt + 1];
        double[] per = new double[5];
        for (int i = 0; i <= ns; i++) {
            double s = P * i / ns;
            g.perimeterPoint(s, per);
            for (int j = 0; j <= nt; j++) {
                double t = j / (double) nt;
                double x = cx + (per[0] - cx) * t, z = cz + (per[1] - cz) * t;
                gx[i][j] = x;
                gz[i][j] = z;
                gy[i][j] = j == nt ? g.eaveHeight(s) : g.surfaceHeight(x, z);
            }
        }
        double[][][] nn = gridNormals(gx, gy, gz, ns, nt);
        for (int i = 0; i < ns; i++) {
            int col = m.stripe(g.stripeAt(P * (i + 0.5) / ns), false);
            for (int j = 0; j < nt; j++) {
                int[] ii = {i, i + 1, i + 1, i};
                int[] jj = {j, j, j + 1, j + 1};
                double[][] q = new double[4][];
                for (int k = 0; k < 4; k++) {
                    double[] v = nn[ii[k]][jj[k]];
                    double t = jj[k] / (double) nt;
                    q[k] = MeshBuilder.vf(gx[ii[k]][jj[k]], gy[ii[k]][jj[k]], gz[ii[k]][jj[k]],
                            gx[ii[k]][jj[k]] / 2, gz[ii[k]][jj[k]] / 2, v[0], v[1], v[2], 0.8 * Math.sin(Math.PI * t));
                }
                m.twoSided(q, col);
            }
        }
        edgeRope(m, g);
        if (g instanceof TensileShape t) {
            tensileStructure(m, t);
        } else {
            for (double[] mast : g.masts) {
                m.tube(mast[0], 0, mast[1], mast[0], mast[2] + 0.9, mast[1], 0.09, 8, WOOD);
                m.box(mast[0], mast[2] - 0.05, mast[1], 0.2, mast[2] + 0.08, RING, false);
            }
        }
        double reach = g.Hw * 0.8 + 0.8;
        for (int k = 0; k < g.anchorS.length; k++) {
            g.perimeterPoint(g.anchorS[k], per);
            double h = g.eaveHeight(g.anchorS[k]);
            double out = g.anchorPole[k] ? reach : 1.0;
            double sx = per[0] + per[2] * out, sz = per[1] + per[3] * out;
            if (g.anchorPole[k]) {
                m.tube(per[0], 0, per[1], per[0], h + 0.3, per[1], 0.06, 6, WOOD);
                m.rope(per[0], h + 0.22, per[1], sx, 0.25, sz, 0.05, 0.02, ROPE);
            } else {
                // Ground anchor: ratchet strap down to a stake.
                m.rope(per[0], h, per[1], sx, 0.25, sz, 0.02, 0.025, STRAP);
                m.box((per[0] + sx) / 2, (h + 0.25) / 2 - 0.05, (per[1] + sz) / 2, 0.05, (h + 0.25) / 2 + 0.05, RING, false);
            }
            m.tube(sx, -0.2, sz, sx + per[2] * 0.1, 0.3, sz + per[3] * 0.1, 0.04, 4, STAKE);
        }
        if (m.p.lights != LightMode.OFF) {
            int seed = 0;
            for (double[] mast : g.masts) {
                for (int k = 0; k < g.anchorS.length; k += 2) {
                    g.perimeterPoint(g.anchorS[k], per);
                    double h = g.eaveHeight(g.anchorS[k]);
                    // Follow the underside of the canvas: the membrane dips between supports, a straight wire would poke through.
                    double ex = per[0] - per[2] * 0.4, ez = per[1] - per[3] * 0.4, ey = h - 0.45, sy = mast[2] * 0.6;
                    int n = Math.max(3, (int) Math.ceil(Math.hypot(ex - mast[0], ez - mast[1]) / 0.8));
                    double[][] pts = new double[n + 1][];
                    for (int q = 0; q <= n; q++) {
                        double u = q / (double) n;
                        double x = mast[0] + (ex - mast[0]) * u, z = mast[1] + (ez - mast[1]) * u;
                        double y = sy + (ey - sy) * u - 0.5 * 4 * u * (1 - u);
                        if (q > 0 && q < n) y = Math.min(y, g.surfaceHeight(x, z) - 0.45);
                        pts[q] = new double[]{x, y, z};
                    }
                    m.festoonPath(pts, seed += 5);
                }
            }
        }
    }

    /**
     * Arena structure: square lattice masts with a crown ring holding the membrane, guy cables on the end masts,
     * and a grid of box trusses hung between the masts over the floor.
     */
    private static void tensileStructure(MeshBuilder m, TensileShape g) {
        double crown = 1.1;
        for (double[] mast : g.masts) {
            double top = mast[2];
            lattice(m, mast[0], 0, mast[1], mast[0], top + 1.6, mast[1], 0.32, true);
            // Crown: ring the membrane is laced to, struts up to the mast head, and a cap.
            int n = 16;
            double ry = top - 0.05;
            double[] px = new double[n + 1], pz = new double[n + 1];
            for (int k = 0; k <= n; k++) {
                double a = Math.PI * 2 * k / n;
                px[k] = mast[0] + crown * Math.cos(a);
                pz[k] = mast[1] + crown * Math.sin(a);
            }
            for (int k = 0; k < n; k++) m.tube(px[k], ry, pz[k], px[k + 1], ry, pz[k + 1], 0.07, 6, ALU);
            for (int k = 0; k < n; k += 4) m.tube(px[k], ry, pz[k], mast[0], top + 1.4, mast[1], 0.035, 4, ALU);
            m.box(mast[0], top + 1.55, mast[1], 0.28, top + 1.75, RING, false);
        }
        // Guy cables from the end masts out past the membrane.
        if (m.p.guyRopes) {
            double[] xs = g.mastXs();
            double reach = g.H * 0.55;
            for (double z : new double[]{g.lineZ(), -g.lineZ()}) {
                for (int end : new int[]{0, xs.length - 1}) {
                    double x = xs[end], dir = end == 0 ? -1 : 1;
                    double gx = dir * (g.params.length / 2.0 + reach * 0.6), gz = z * 1.6;
                    m.rope(x, g.H * 0.95, z, gx, 0.2, gz, 0.4, 0.04, ROPE);
                    m.box(gx, 0, gz, 0.4, 0.25, RING, false);
                }
            }
        }
        if (!m.p.rigging) return;
        double y = g.trussY();
        double[] xs = g.mastXs();
        double z = g.lineZ();
        for (double lz : new double[]{z, -z}) {
            for (int i = 0; i + 1 < xs.length; i++) lattice(m, xs[i] + 0.4, y, lz, xs[i + 1] - 0.4, y, lz, 0.26, false);
        }
        for (double x : xs) lattice(m, x, y, -z + 0.4, x, y, z - 0.4, 0.26, false);
    }

    /**
     * Square lattice truss from a to b: four chords, rungs and zig-zag diagonals on every face. {@code vertical}
     * builds a mast with a base plate.
     */
    private static void lattice(MeshBuilder m, double ax, double ay, double az, double bx, double by, double bz, double half, boolean vertical) {
        double dx = bx - ax, dy = by - ay, dz = bz - az, len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-6) return;
        dx /= len; dy /= len; dz /= len;
        // Two directions across the truss.
        double ux, uy, uz, wx, wy, wz;
        if (Math.abs(dy) > 0.9) {
            ux = 1; uy = 0; uz = 0;
            wx = 0; wy = 0; wz = 1;
        } else {
            ux = -dz; uy = 0; uz = dx;
            double l = Math.hypot(ux, uz);
            ux /= l; uz /= l;
            wx = 0; wy = 1; wz = 0;
        }
        double[][] corners = {{1, 1}, {-1, 1}, {-1, -1}, {1, -1}};
        double chord = Math.max(0.035, half * 0.16), lace = chord * 0.45;
        for (double[] c : corners) {
            double ox = (ux * c[0] + wx * c[1]) * half, oy = (uy * c[0] + wy * c[1]) * half, oz = (uz * c[0] + wz * c[1]) * half;
            m.tube(ax + ox, ay + oy, az + oz, bx + ox, by + oy, bz + oz, chord, 6, ALU);
        }
        int bays = Math.max(1, (int) Math.round(len / (half * 2.2)));
        for (int b = 0; b <= bays; b++) {
            double t0 = len * b / bays;
            for (int f = 0; f < 4; f++) {
                double[] c0 = corners[f], c1 = corners[(f + 1) % 4];
                double[] p0 = corner(ax, ay, az, dx, dy, dz, ux, uy, uz, wx, wy, wz, half, c0, t0);
                double[] p1 = corner(ax, ay, az, dx, dy, dz, ux, uy, uz, wx, wy, wz, half, c1, t0);
                m.tube(p0[0], p0[1], p0[2], p1[0], p1[1], p1[2], lace, 4, ALU);
                if (b < bays) {
                    double t1 = len * (b + 1) / bays;
                    double[] q = corner(ax, ay, az, dx, dy, dz, ux, uy, uz, wx, wy, wz, half, (b + f) % 2 == 0 ? c1 : c0, t1);
                    double[] p = (b + f) % 2 == 0 ? p0 : p1;
                    m.tube(p[0], p[1], p[2], q[0], q[1], q[2], lace, 4, ALU);
                }
            }
        }
        if (vertical) m.box(ax, ay, az, half * 2.2, ay + 0.06, RING, false);
    }

    private static double[] corner(double ax, double ay, double az, double dx, double dy, double dz,
                                   double ux, double uy, double uz, double wx, double wy, double wz,
                                   double half, double[] c, double t) {
        return new double[]{
                ax + dx * t + (ux * c[0] + wx * c[1]) * half,
                ay + dy * t + (uy * c[0] + wy * c[1]) * half,
                az + dz * t + (uz * c[0] + wz * c[1]) * half};
    }

    /** Floor laid inside the walls, with a skirt when raised. */
    private static void floor(MeshBuilder m, TentShape g) {
        FloorMode mode = m.p.floor;
        if (mode == FloorMode.NONE) return;
        double ft = Math.max(0.015, g.floorTop());
        int col = MeshBuilder.opaque(mode.tint);
        double[][] ring = insetRing(g, 0.12, 0.5);
        double cx = 0, cz = 0;
        for (double[] p : ring) { cx += p[0]; cz += p[1]; }
        cx /= ring.length;
        cz /= ring.length;
        for (int i = 0; i < ring.length; i++) {
            double[] a = ring[i], b = ring[(i + 1) % ring.length];
            m.quad(new double[][]{
                    MeshBuilder.v(cx, ft, cz, cx / 2, cz / 2, 0, 1, 0),
                    MeshBuilder.v(a[0], ft, a[1], a[0] / 2, a[1] / 2, 0, 1, 0),
                    MeshBuilder.v(b[0], ft, b[1], b[0] / 2, b[1] / 2, 0, 1, 0),
                    MeshBuilder.v(b[0], ft, b[1], b[0] / 2, b[1] / 2, 0, 1, 0)}, col, MeshBuilder.FLOOR, false);
            if (ft > 0.05) skirt(m, a, b, 0, ft, MeshBuilder.shade(col, 0.7F), MeshBuilder.FLOOR);
        }
    }

    /** Perimeter pulled in by {@code inset}, sampled every {@code step} (panel boundaries kept so corners stay sharp). */
    private static double[][] insetRing(TentShape g, double inset, double step) {
        java.util.List<double[]> out = new java.util.ArrayList<>();
        double[] pb = g.panelBounds();
        double[] per = new double[5];
        for (int k = 0; k + 1 < pb.length; k++) {
            int n = Math.max(1, (int) Math.ceil((pb[k + 1] - pb[k]) / step));
            for (int i = 0; i < n; i++) {
                double s = pb[k] + (pb[k + 1] - pb[k]) * i / n;
                double[] nrm = stationNormal(g, s);
                g.perimeterPoint(s, per);
                boolean corner = Math.abs(nrm[0]) > 0.3 && Math.abs(nrm[1]) > 0.3;
                double d = corner ? inset * Math.sqrt(2) : inset;
                out.add(new double[]{per[0] - nrm[0] * d, per[1] - nrm[1] * d});
            }
        }
        return out.toArray(new double[0][]);
    }

    private static void skirt(MeshBuilder m, double[] a, double[] b, double y0, double y1, int col, int layer) {
        double dx = b[0] - a[0], dz = b[1] - a[1], l = Math.hypot(dx, dz);
        if (l < 1e-6) return;
        double nx = dz / l, nz = -dx / l;
        m.quad(new double[][]{
                MeshBuilder.v(a[0], y0, a[1], 0, y0, nx, 0, nz),
                MeshBuilder.v(b[0], y0, b[1], l, y0, nx, 0, nz),
                MeshBuilder.v(b[0], y1, b[1], l, y1, nx, 0, nz),
                MeshBuilder.v(a[0], y1, a[1], 0, y1, nx, 0, nz)}, col, layer, false);
    }

    /** Stage platform at the back of the tent, with its front face, side skirts and a pleated black drape. */
    private static void stage(MeshBuilder m, TentShape g) {
        if (!g.hasStage()) return;
        double xs = g.stageFrontX(), sh = m.p.stageHeight, ft = g.floorTop();
        double[][] ring = insetRing(g, 0.3, 0.25);
        int n = ring.length;
        int start = -1;
        for (int i = 0; i < n; i++) if (ring[i][0] >= xs && ring[(i + 1) % n][0] < xs) start = (i + 1) % n;
        if (start < 0) return;
        java.util.List<double[]> poly = new java.util.ArrayList<>();
        double[] enter = crossing(ring[(start + n - 1) % n], ring[start], xs);
        poly.add(enter);
        int i = start;
        double minX = Double.MAX_VALUE;
        while (ring[i][0] < xs && poly.size() <= n) {
            poly.add(ring[i]);
            minX = Math.min(minX, ring[i][0]);
            i = (i + 1) % n;
        }
        double[] exit = crossing(ring[(i + n - 1) % n], ring[i], xs);
        poly.add(exit);
        int deck = MeshBuilder.opaque(0x1A1A1C), skirtCol = MeshBuilder.opaque(0x111113);
        double cx = 0, cz = 0;
        for (double[] p : poly) { cx += p[0]; cz += p[1]; }
        cx /= poly.size();
        cz /= poly.size();
        // Fan around the centre, including the closing triangle along the front edge.
        for (int k = 0; k < poly.size(); k++) {
            double[] a = poly.get(k), b = poly.get((k + 1) % poly.size());
            m.quad(new double[][]{
                    MeshBuilder.v(cx, sh, cz, cx / 2, cz / 2, 0, 1, 0),
                    MeshBuilder.v(b[0], sh, b[1], b[0] / 2, b[1] / 2, 0, 1, 0),
                    MeshBuilder.v(a[0], sh, a[1], a[0] / 2, a[1] / 2, 0, 1, 0),
                    MeshBuilder.v(a[0], sh, a[1], a[0] / 2, a[1] / 2, 0, 1, 0)}, deck, MeshBuilder.DECK, false);
            if (k + 1 < poly.size()) {
                skirt(m, a, b, ft, sh, skirtCol, MeshBuilder.DECK);
                skirt(m, b, a, ft, sh, skirtCol, MeshBuilder.DECK);
            }
        }
        // Front face of the stage (the closing edge), both faces.
        skirt(m, exit, enter, ft, sh, skirtCol, MeshBuilder.DECK);
        skirt(m, enter, exit, ft, sh, skirtCol, MeshBuilder.DECK);
        m.box(xs - 0.04, sh - 0.02, Math.min(enter[1], exit[1]), xs + 0.02, sh + 0.02, Math.max(enter[1], exit[1]), 0xFF2A2A2C, false);
        if (!m.p.curtain) return;
        // Pleated drape across the back, hanging from just under the eave. On a rounded end it moves forward until
        // it covers most of the stage width.
        double front = Math.abs(exit[1] - enter[1]);
        double xc = minX + 0.35;
        double[] span = chordAt(ring, xc);
        while (xc < xs - 1 && (span == null || span[1] - span[0] < 0.8 * front)) {
            xc += 0.1;
            span = chordAt(ring, xc);
        }
        if (span == null) return;
        double z0 = span[0], z1 = span[1];
        double roof = g.roofHeight(xc, (z0 + z1) / 2);
        double top = Math.min(g.Hw - 0.15, Double.isNaN(roof) ? g.Hw : roof - 0.3);
        top = Math.max(top, sh + 1.5);
        int cols = Math.max(4, (int) Math.ceil((z1 - z0) / 0.2));
        int black = MeshBuilder.opaque(0x0E0E10);
        int rows = Math.max(1, (int) Math.ceil(top - sh));
        for (int k = 0; k < cols; k++) {
            double za0 = z0 + (z1 - z0) * k / cols, za1 = z0 + (z1 - z0) * (k + 1) / cols;
            double x0 = xc + 0.07 * Math.sin(za0 * Math.PI / 0.4), x1 = xc + 0.07 * Math.sin(za1 * Math.PI / 0.4);
            double nz0 = -Math.cos(za0 * Math.PI / 0.4) * 0.5, nz1 = -Math.cos(za1 * Math.PI / 0.4) * 0.5;
            for (int r = 0; r < rows; r++) {
                double y0 = sh + (top - sh) * r / rows, y1 = sh + (top - sh) * (r + 1) / rows;
                double f0 = 0.25 * (1 - (double) r / rows), f1 = 0.25 * (1 - (double) (r + 1) / rows);
                m.doubleSided(new double[][]{
                        MeshBuilder.vf(x0, y0, za0, za0, y0, 1, 0, nz0, f0),
                        MeshBuilder.vf(x1, y0, za1, za1, y0, 1, 0, nz1, f0),
                        MeshBuilder.vf(x1, y1, za1, za1, y1, 1, 0, nz1, f1),
                        MeshBuilder.vf(x0, y1, za0, za0, y1, 1, 0, nz0, f1)}, black);
            }
        }
        m.tube(xc - 0.05, top + 0.02, z0, xc - 0.05, top + 0.02, z1, 0.03, 4, ALU);
    }

    /** z extent {min, max} of the ring at local x, or null. */
    private static double[] chordAt(double[][] ring, double x) {
        double lo = Double.MAX_VALUE, hi = -Double.MAX_VALUE;
        int n = ring.length;
        for (int k = 0; k < n; k++) {
            double[] a = ring[k], b = ring[(k + 1) % n];
            if ((a[0] - x) * (b[0] - x) <= 0 && a[0] != b[0]) {
                double z = crossing(a, b, x)[1];
                lo = Math.min(lo, z);
                hi = Math.max(hi, z);
            }
        }
        return lo < hi ? new double[]{lo, hi} : null;
    }

    private static double[] crossing(double[] a, double[] b, double x) {
        double t = (x - a[0]) / (b[0] - a[0]);
        return new double[]{x, a[1] + (b[1] - a[1]) * t};
    }

    /** Sign board over the front, on two brackets; the lettering is drawn by the renderer. */
    private static void signBoard(MeshBuilder m, RectShape g) {
        double[] b = g.signBoard();
        int col = MeshBuilder.opaque(m.p.signColor), trim = 0xFF1A1A1C;
        m.box(b[0] - 0.1, b[1], b[3], b[0], b[2], b[4], col, false);
        m.box(b[0] - 0.09, b[2] - 0.04, b[3], b[0] + 0.01, b[2] + 0.01, b[4], trim, false);
        m.box(b[0] - 0.09, b[1] - 0.01, b[3], b[0] + 0.01, b[1] + 0.04, b[4], trim, false);
        for (double z : new double[]{b[3] + 0.3, b[4] - 0.3}) {
            m.box(g.ax - 0.05, b[1] + 0.2, z - 0.04, b[0] - 0.1, b[1] + 0.3, z + 0.04, ALU, false);
        }
    }

    /** Gathered curtains hanging in front of every front pole, tied at mid height. */
    private static void poleCurtains(MeshBuilder m, RectShape g) {
        double[] per = new double[5];
        int col = MeshBuilder.opaque(m.p.colorA);
        double top = m.p.sign ? g.signBoard()[1] : g.Hw - 0.05;
        for (double s : g.poleStations()) {
            g.perimeterPoint(s, per);
            if (!(per[0] > g.ax - 1e-6)) continue;
            // Corner curtains move in a little so they stay on the front.
            double z = Mth.clamp(per[1], -g.az + 0.25, g.az - 0.25);
            drape(m, g.ax + 0.16, z, top, col);
        }
    }

    /**
     * A curtain gathered at a tie: wide and pleated at the top, pinched at the tie, flaring out again to the floor.
     * Built in the local frame facing +x, centred on z.
     */
    private static void drape(MeshBuilder m, double x, double z, double top, int col) {
        double tie = Math.max(0.9, top * 0.48);
        double[] ys = {top, tie, 0.02};
        double[] half = {0.42, 0.07, 0.24};
        double[] depth = {0.07, 0.03, 0.09};
        int folds = 6;
        for (int r = 0; r < 2; r++) {
            for (int k = 0; k < folds; k++) {
                double[][] q = new double[4][];
                int[][] corners = {{r, k}, {r, k + 1}, {r + 1, k + 1}, {r + 1, k}};
                for (int c = 0; c < 4; c++) {
                    int row = corners[c][0], f = corners[c][1];
                    double u = f / (double) folds;
                    double zz = z + (u * 2 - 1) * half[row];
                    double xx = x + ((f % 2 == 0) ? depth[row] : -depth[row]) * 0.5;
                    double flex = row == 0 ? 0 : row == 1 ? 0.15 : 0.6;
                    q[c] = MeshBuilder.vf(xx, ys[row], zz, zz / 2, ys[row] / 2, 1, 0, (f % 2 == 0) ? 0.3 : -0.3, flex);
                }
                m.doubleSided(q, col);
            }
        }
        m.box(x, tie - 0.05, z, 0.09, tie + 0.05, ROPE, false);
    }

    /** Gutters along edges joined to a neighbouring tent. */
    private static void gutters(MeshBuilder m, RectShape g) {
        if (m.p.joined == 0) return;
        double y = g.Hw + 0.02, ax = g.ax, az = g.az;
        double[][] edges = {{ax, az, -ax, az}, {-ax, az, -ax, -az}, {-ax, -az, ax, -az}, {ax, -az, ax, az}};
        for (int e = 0; e < 4; e++) {
            if (!g.isJoined(e)) continue;
            double[] c = edges[e];
            m.tube(c[0], y, c[1], c[2], y, c[3], 0.11, 4, ALU);
        }
    }

    // ------------------------------------------------------------------ shared parts

    /**
     * Membrane drawn as radial lines from the ridge to each perimeter sample, with a slight sag between seams.
     * Samples follow the stripe panels so colours and corners line up.
     */
    private static void radialRoof(MeshBuilder m, TentShape g, DoubleUnaryOperator ridge, DoubleUnaryOperator profile, double ripple, double flex) {
        double[] pb = g.panelBounds();
        // Three columns per panel give the seam sag its curve; big tents are seen from further and get two.
        int panels = pb.length - 1, sub = g.footprint() > 20 ? 2 : 3, ns = panels * sub;
        double[] S = new double[ns + 1];
        for (int k = 0; k < panels; k++) {
            for (int j = 0; j < sub; j++) S[k * sub + j] = pb[k] + (pb[k + 1] - pb[k]) * j / sub;
        }
        S[ns] = pb[panels];
        int nt = Mth.clamp((int) Math.round(g.footprint() * 0.55) + 4, 8, 20);
        double[][] gx = new double[ns + 1][nt + 1], gy = new double[ns + 1][nt + 1], gz = new double[ns + 1][nt + 1];
        double[][] gv = new double[ns + 1][nt + 1], gf = new double[ns + 1][nt + 1];
        double[] per = new double[5];
        for (int i = 0; i <= ns; i++) {
            double s = S[i];
            g.perimeterPoint(s, per);
            double he = g.eaveHeight(s), hr = ridge.applyAsDouble(per[4]);
            double slant = Math.hypot(Math.hypot(per[0] - per[4], per[1]), hr - he);
            double sf = Math.sin(Math.PI * (i % sub) / (double) sub);
            for (int j = 0; j <= nt; j++) {
                double t = j / (double) nt;
                gx[i][j] = per[4] + (per[0] - per[4]) * t;
                gz[i][j] = per[1] * t;
                gy[i][j] = he + (hr - he) * profile.applyAsDouble(t) - ripple * sf * Math.sin(Math.PI * t);
                gv[i][j] = t * slant / 2;
                gf[i][j] = flex * Math.sin(Math.PI * t) * (0.35 + 0.65 * sf);
            }
        }
        double[][][] n = gridNormals(gx, gy, gz, ns, nt);
        for (int i = 0; i < ns; i++) {
            int col = m.stripe(i / sub, false);
            double u0 = S[i] / 2, u1 = S[i + 1] / 2;
            for (int j = 0; j < nt; j++) {
                int[] ii = {i, i + 1, i + 1, i};
                int[] jj = {j, j, j + 1, j + 1};
                double[] us = {u0, u1, u1, u0};
                double[][] q = new double[4][];
                for (int k = 0; k < 4; k++) {
                    double[] nn = n[ii[k]][jj[k]];
                    q[k] = MeshBuilder.vf(gx[ii[k]][jj[k]], gy[ii[k]][jj[k]], gz[ii[k]][jj[k]], us[k], gv[ii[k]][jj[k]], nn[0], nn[1], nn[2], gf[ii[k]][jj[k]]);
                }
                m.twoSided(q, col);
            }
        }
    }

    private static double[][][] gridNormals(double[][] x, double[][] y, double[][] z, int ns, int nt) {
        double[][][] out = new double[ns + 1][nt + 1][];
        for (int i = 0; i <= ns; i++) {
            int ia = i == 0 ? ns - 1 : i - 1, ib = i == ns ? 1 : i + 1; // the perimeter is a closed loop
            for (int j = 0; j <= nt; j++) {
                int jr = Math.max(j, 1); // the ridge row can collapse to a point; borrow the next row's tangent
                double sx = x[ib][jr] - x[ia][jr], sy = y[ib][jr] - y[ia][jr], sz = z[ib][jr] - z[ia][jr];
                int ja = Math.max(0, j - 1), jb = Math.min(nt, j + 1);
                double tx = x[i][jb] - x[i][ja], ty = y[i][jb] - y[i][ja], tz = z[i][jb] - z[i][ja];
                double nx = sy * tz - sz * ty, ny = sz * tx - sx * tz, nz = sx * ty - sy * tx;
                double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (len < 1e-9) {
                    out[i][j] = new double[]{0, 1, 0};
                    continue;
                }
                if (ny < 0) len = -len;
                out[i][j] = new double[]{nx / len, ny / len, nz / len};
            }
        }
        return out;
    }

    /** Side walls between consecutive poles, optionally with arched windows; rolled up under the eave when open. */
    private static void walls(MeshBuilder m, TentShape g, double bowAmp) {
        boolean open = m.p.walls == WallMode.OPEN;
        double[] st = g.poleStations();
        double P = g.perimeterLength();
        double[] a = new double[5], b = new double[5], mid = new double[5];
        for (int k = 0; k < st.length; k++) {
            double s0 = st[k], s1 = k + 1 < st.length ? st[k + 1] : P, len = s1 - s0;
            boolean windowed = m.p.walls == WallMode.WINDOWS && len >= 1.5;
            int pieces = open ? Math.max(1, (int) Math.ceil(len)) : windowed ? Math.max(8, (int) Math.ceil(len / 0.4))
                    : Math.max(4, (int) Math.ceil(len / 0.8));
            for (int i = 0; i < pieces; i++) {
                double sa = s0 + len * i / pieces, sb = s0 + len * (i + 1) / pieces;
                g.perimeterPoint(sa, a);
                g.perimeterPoint(sb, b);
                g.perimeterPoint((sa + sb) / 2, mid);
                double cx = b[0] - a[0], cz = b[1] - a[1], cl = Math.hypot(cx, cz);
                if (cl < 1e-6) continue;
                double nx = cz / cl, nz = -cx / cl;
                if (open) {
                    double y = (g.eaveHeight(sa) + g.eaveHeight(sb)) / 2 - 0.2;
                    m.tube(a[0] - nx * 0.06, y, a[1] - nz * 0.06, b[0] - nx * 0.06, y, b[1] - nz * 0.06, 0.12, 6, MeshBuilder.opaque(m.p.colorA));
                    continue;
                }
                if (!g.hasWallAt(mid[0], mid[1])) continue;
                int col = m.stripe(g.stripeAt((sa + sb) / 2), false);
                double ua = i / (double) pieces, ub = (i + 1) / (double) pieces;
                double ta = g.wallTop(mid[0], mid[1], g.eaveHeight(sa)), tb = g.wallTop(mid[0], mid[1], g.eaveHeight(sb));
                if (g.isCounter(mid[0], mid[1])) {
                    shelf(m, a, b, nx, nz, ta);
                    wallColumn(m, g, a, b, nx, nz, sa, sb, ua, ub, 0, 0, 0, ta, tb, ta, tb, col, MeshBuilder.SOLID);
                    continue;
                }
                double margin = 0.14;
                if (windowed && ua >= margin - 1e-6 && ub <= 1 - margin + 1e-6) {
                    double yb = Math.min(0.9, 0.3 * Math.min(ta, tb));
                    double wa = archTop(ua, margin, yb, ta), wb = archTop(ub, margin, yb, tb);
                    wallColumn(m, g, a, b, nx, nz, sa, sb, ua, ub, bowAmp, 0, 0, yb, yb, ta, tb, col, MeshBuilder.SOLID);
                    wallColumn(m, g, a, b, nx, nz, sa, sb, ua, ub, bowAmp, yb, yb, wa, wb, ta, tb, GLASS, MeshBuilder.GLASS);
                    wallColumn(m, g, a, b, nx, nz, sa, sb, ua, ub, bowAmp, wa, wb, ta, tb, ta, tb, col, MeshBuilder.SOLID);
                } else {
                    wallColumn(m, g, a, b, nx, nz, sa, sb, ua, ub, bowAmp, 0, 0, ta, tb, ta, tb, col, MeshBuilder.SOLID);
                }
            }
        }
    }

    /** Counter board on top of a low wall: 0.1 out, 0.4 in, 6 cm thick. */
    private static void shelf(MeshBuilder m, double[] a, double[] b, double nx, double nz, double y) {
        double out = 0.1, in = 0.4, t = 0.06;
        double[] ao = {a[0] + nx * out, a[1] + nz * out}, bo = {b[0] + nx * out, b[1] + nz * out};
        double[] ai = {a[0] - nx * in, a[1] - nz * in}, bi = {b[0] - nx * in, b[1] - nz * in};
        double y1 = y + t;
        m.quad(new double[][]{MeshBuilder.v(ao[0], y1, ao[1], 0, 0, 0, 1, 0), MeshBuilder.v(bo[0], y1, bo[1], 1, 0, 0, 1, 0),
                MeshBuilder.v(bi[0], y1, bi[1], 1, 0.25, 0, 1, 0), MeshBuilder.v(ai[0], y1, ai[1], 0, 0.25, 0, 1, 0)}, SHELF, MeshBuilder.SOLID, false);
        m.quad(new double[][]{MeshBuilder.v(ao[0], y, ao[1], 0, 0, 0, -1, 0), MeshBuilder.v(bo[0], y, bo[1], 1, 0, 0, -1, 0),
                MeshBuilder.v(bi[0], y, bi[1], 1, 0.25, 0, -1, 0), MeshBuilder.v(ai[0], y, ai[1], 0, 0.25, 0, -1, 0)}, SHELF, MeshBuilder.SOLID, false);
        m.quad(new double[][]{MeshBuilder.v(ao[0], y, ao[1], 0, 0, nx, 0, nz), MeshBuilder.v(bo[0], y, bo[1], 1, 0, nx, 0, nz),
                MeshBuilder.v(bo[0], y1, bo[1], 1, 0.03, nx, 0, nz), MeshBuilder.v(ao[0], y1, ao[1], 0, 0.03, nx, 0, nz)}, SHELF, MeshBuilder.SOLID, false);
        m.quad(new double[][]{MeshBuilder.v(ai[0], y, ai[1], 0, 0, -nx, 0, -nz), MeshBuilder.v(bi[0], y, bi[1], 1, 0, -nx, 0, -nz),
                MeshBuilder.v(bi[0], y1, bi[1], 1, 0.03, -nx, 0, -nz), MeshBuilder.v(ai[0], y1, ai[1], 0, 0.03, -nx, 0, -nz)}, SHELF, MeshBuilder.SOLID, false);
    }

    /** Top of an arched window at section fraction {@code u}. */
    private static double archTop(double u, double margin, double yb, double top) {
        double w = (u - margin) / (1 - 2 * margin);
        return yb + (top - yb - 0.45) * (0.72 + 0.28 * Math.sin(Math.PI * Mth.clamp(w, 0, 1)));
    }

    /** One vertical slice of wall from y0 to y1 (per end), split in rows for smooth lighting and bowed inwards. */
    private static void wallColumn(MeshBuilder m, TentShape g, double[] a, double[] b, double nx, double nz,
                                   double sa, double sb, double ua, double ub, double bowAmp,
                                   double ya0, double yb0, double ya1, double yb1, double ta, double tb, int col, int layer) {
        int rows = Math.max(1, (int) Math.ceil(Math.max(ya1 - ya0, yb1 - yb0)));
        for (int r = 0; r < rows; r++) {
            double f0 = r / (double) rows, f1 = (r + 1) / (double) rows;
            double[][] q = {
                    wallVert(a, nx, nz, sa, ua, ya0 + (ya1 - ya0) * f0, ta, bowAmp),
                    wallVert(b, nx, nz, sb, ub, yb0 + (yb1 - yb0) * f0, tb, bowAmp),
                    wallVert(b, nx, nz, sb, ub, yb0 + (yb1 - yb0) * f1, tb, bowAmp),
                    wallVert(a, nx, nz, sa, ua, ya0 + (ya1 - ya0) * f1, ta, bowAmp)};
            if (layer == MeshBuilder.GLASS) m.twoSided(q, col, col, layer);
            else m.twoSided(q, col, m.inner(col), layer);
        }
    }

    private static double[] wallVert(double[] per, double nx, double nz, double s, double u, double y, double top, double bowAmp) {
        double shape = Math.sin(Math.PI * u) * Math.sin(Math.PI * Mth.clamp(y / top, 0, 1));
        double bow = bowAmp * shape;
        return MeshBuilder.vf(per[0] - nx * bow, y, per[1] - nz * bow, s / 2, y / 2, nx, 0, nz, 0.8 * shape);
    }

    /** Flat vertical strip (gables): from (x0, z0) to (x1, z1), facing +x * {@code facingX}. */
    private static void verticalStrip(MeshBuilder m, double x0, double z0, double x1, double z1, int facingX,
                                      double nz, double ya0, double yb0, double ya1, double yb1, int col, int layer) {
        int rows = Math.max(1, (int) Math.ceil(Math.max(ya1 - ya0, yb1 - yb0)));
        for (int r = 0; r < rows; r++) {
            double f0 = r / (double) rows, f1 = (r + 1) / (double) rows;
            double a0 = ya0 + (ya1 - ya0) * f0, a1 = ya0 + (ya1 - ya0) * f1, b0 = yb0 + (yb1 - yb0) * f0, b1 = yb0 + (yb1 - yb0) * f1;
            double[][] q = {
                    MeshBuilder.v(x0, a0, z0, z0 / 2, a0 / 2, facingX, 0, nz),
                    MeshBuilder.v(x1, b0, z1, z1 / 2, b0 / 2, facingX, 0, nz),
                    MeshBuilder.v(x1, b1, z1, z1 / 2, b1 / 2, facingX, 0, nz),
                    MeshBuilder.v(x0, a1, z0, z0 / 2, a1 / 2, facingX, 0, nz)};
            m.twoSided(q, col, m.inner(col), layer);
        }
    }

    /** Scalloped flap hanging just outside the eave, in the opposite stripe colour. */
    private static void valance(MeshBuilder m, TentShape g, double depth) {
        if (!m.p.valance) return;
        double[] pb = g.panelBounds();
        double[] a = new double[5], b = new double[5];
        int sub = 6;
        double o = 0.06;
        double[] mid = new double[5];
        for (int k = 0; k + 1 < pb.length; k++) {
            g.perimeterPoint((pb[k] + pb[k + 1]) / 2, mid);
            if (g instanceof RectShape r && r.isJoinedAt(mid[0], mid[1])) continue;
            int col = m.stripe(k, true);
            for (int i = 0; i < sub; i++) {
                double s0 = pb[k] + (pb[k + 1] - pb[k]) * i / sub, s1 = pb[k] + (pb[k + 1] - pb[k]) * (i + 1) / sub;
                g.perimeterPoint(s0, a);
                g.perimeterPoint(s1, b);
                double cx = b[0] - a[0], cz = b[1] - a[1], cl = Math.hypot(cx, cz);
                if (cl < 1e-6) continue;
                double nx = cz / cl, nz = -cx / cl;
                double d0 = depth * (0.42 + 0.28 * Math.sin(Math.PI * i / sub));
                double d1 = depth * (0.42 + 0.28 * Math.sin(Math.PI * (i + 1) / sub));
                double t0 = g.eaveHeight(s0) + 0.02, t1 = g.eaveHeight(s1) + 0.02;
                double[][] q = {
                        MeshBuilder.vf(a[0] + nx * o, t0 - d0, a[1] + nz * o, s0 / 2, d0 / 2, nx, 0, nz, 1.6),
                        MeshBuilder.vf(b[0] + nx * o, t1 - d1, b[1] + nz * o, s1 / 2, d1 / 2, nx, 0, nz, 1.6),
                        MeshBuilder.vf(b[0] + nx * o, t1, b[1] + nz * o, s1 / 2, 0, nx, 0, nz, 0.1),
                        MeshBuilder.vf(a[0] + nx * o, t0, a[1] + nz * o, s0 / 2, 0, nx, 0, nz, 0.1)};
                m.twoSided(q, col);
            }
        }
    }

    /** Outward direction at a pole station, averaged across corners. */
    private static double[] stationNormal(TentShape g, double s) {
        double[] a = new double[5], b = new double[5];
        g.perimeterPoint(s - 1e-3, a);
        g.perimeterPoint(s + 1e-3, b);
        double nx = a[2] + b[2], nz = a[3] + b[3], l = Math.hypot(nx, nz);
        return new double[]{nx / l, nz / l};
    }

    private static void guyRopes(MeshBuilder m, TentShape g, boolean cornersOnly) {
        if (!m.p.guyRopes) return;
        double[] per = new double[5];
        for (double s : g.poleStations()) {
            double[] n = stationNormal(g, s);
            boolean corner = Math.abs(n[0]) > 0.3 && Math.abs(n[1]) > 0.3;
            if (cornersOnly && !corner) continue;
            g.perimeterPoint(s, per);
            if (g instanceof RectShape r && r.isJoinedAt(per[0] + n[0] * 0.01, per[1] + n[1] * 0.01)) continue;
            double reach = g.Hw * 0.8 + 0.6;
            double sx = per[0] + n[0] * reach, sz = per[1] + n[1] * reach;
            // Stakes lean away from the tent, the rope sags a little under its own weight.
            double tx = sx + n[0] * 0.12, tz = sz + n[1] * 0.12, ty = 0.3;
            m.tube(sx, -0.2, sz, tx, ty, tz, 0.04, 4, STAKE);
            double top = g.eaveHeight(s) + 0.22;
            double len = Math.hypot(reach, top - ty);
            m.rope(per[0], top, per[1], tx, ty - 0.03, tz, 0.02 * len, 0.02, ROPE);
        }
    }

    /** Bolt rope running along the eave edge of the canvas. */
    private static void edgeRope(MeshBuilder m, TentShape g) {
        double P = g.perimeterLength();
        int n = Math.max(16, (int) Math.ceil(P / 0.7));
        double[] a = new double[5], b = new double[5];
        for (int i = 0; i < n; i++) {
            double s0 = P * i / n, s1 = P * (i + 1) / n;
            g.perimeterPoint(s0, a);
            g.perimeterPoint(s1, b);
            m.tube(a[0] + a[2] * 0.07, g.eaveHeight(s0) + 0.01, a[1] + a[3] * 0.07,
                    b[0] + b[2] * 0.07, g.eaveHeight(s1) + 0.01, b[1] + b[3] * 0.07, 0.03, 3, EDGE_ROPE);
        }
    }

    /** Door curtains tied back on each side of every opening in closed walls. */
    private static void entranceFlaps(MeshBuilder m, TentShape g) {
        if (m.p.walls == WallMode.OPEN || m.p.entrance.opensWholeSide()) return;
        double P = g.perimeterLength(), step = 0.25;
        int n = (int) Math.ceil(P / step);
        double[] per = new double[5], pa = new double[5], pb = new double[5];
        g.perimeterPoint(-step / 2, per);
        boolean prev = g.hasWallAt(per[0], per[1]);
        for (int i = 0; i < n; i++) {
            double sm = P * (i + 0.5) / n;
            g.perimeterPoint(sm, per);
            boolean cur = g.hasWallAt(per[0], per[1]);
            // Only openings that are real entrances get curtains (not joined sides).
            boolean entranceSide = false;
            if (cur != prev) {
                g.perimeterPoint(sm - P / n, pa);
                entranceSide = cur ? g.isEntrance(pa[0], pa[1]) : g.isEntrance(per[0], per[1]);
            }
            if (cur != prev && entranceSide) {
                double st = P * i / n;
                g.perimeterPoint(st - 0.01, pa);
                g.perimeterPoint(st + 0.01, pb);
                double tx = pb[0] - pa[0], tz = pb[1] - pa[1], tl = Math.hypot(tx, tz);
                // dir points along the perimeter into the walled side
                double dx = (cur ? tx : -tx) / tl, dz = (cur ? tz : -tz) / tl;
                g.perimeterPoint(st, per);
                int col = m.stripe(g.stripeAt(st + (cur ? 0.3 : -0.3)), false);
                flap(m, per[0], per[1], per[2], per[3], dx, dz, g.eaveHeight(st), col);
            }
            prev = cur;
        }
    }

    private static void flap(MeshBuilder m, double bx, double bz, double nx, double nz, double dx, double dz, double top, int col) {
        // Curtain width follows the wall height so small tents get small curtains.
        double w = Mth.clamp(top * 0.22, 0.35, 1.0);
        dx *= w;
        dz *= w;
        double o = 0.08, tie = Math.min(1.15, top * 0.4);
        double[] t = {bx + dx * 0.25 + nx * (o + 0.12), tie, bz + dz * 0.25 + nz * (o + 0.12)};
        double[][] topEdge = {
                {bx + nx * o, top - 0.02, bz + nz * o},
                {bx + dx * 0.5 + nx * (o + 0.05), top - 0.02, bz + dz * 0.5 + nz * (o + 0.05)},
                {bx + dx * 1.0 + nx * o, top - 0.02, bz + dz * 1.0 + nz * o}};
        for (int k = 0; k < 2; k++) {
            double[] a = topEdge[k], b = topEdge[k + 1];
            m.twoSided(new double[][]{
                    MeshBuilder.v(a[0], a[1], a[2], k * 0.25, 0, nx, 0, nz),
                    MeshBuilder.v(b[0], b[1], b[2], (k + 1) * 0.25, 0, nx, 0, nz),
                    MeshBuilder.v(t[0], t[1], t[2], 0.2, (top - tie) / 2, nx, 0, nz),
                    MeshBuilder.v(t[0], t[1], t[2], 0.2, (top - tie) / 2, nx, 0, nz)}, col);
        }
        m.twoSided(new double[][]{
                MeshBuilder.v(t[0], t[1], t[2], 0.2, 0, nx, 0, nz),
                MeshBuilder.v(t[0], t[1], t[2], 0.2, 0, nx, 0, nz),
                MeshBuilder.vf(bx + dx * 0.55 + nx * (o + 0.14), 0.02, bz + dz * 0.55 + nz * (o + 0.14), 0.3, tie / 2, nx, 0, nz, 0.9),
                MeshBuilder.vf(bx + dx * 0.1 + nx * (o + 0.1), 0.02, bz + dz * 0.1 + nz * (o + 0.1), 0.05, tie / 2, nx, 0, nz, 0.6)}, col);
        m.box(t[0], tie - 0.06, t[2], 0.06, tie + 0.06, ROPE, false);
    }

    /** Aluminium beams along the eave of a rectangular frame. */
    private static void eaveBeams(MeshBuilder m, TentShape g, double ax, double az) {
        double y = g.Hw - 0.07, i = 0.09;
        m.tube(ax - i, y, az - i, -ax + i, y, az - i, 0.065, 4, ALU);
        m.tube(-ax + i, y, az - i, -ax + i, y, -az + i, 0.065, 4, ALU);
        m.tube(-ax + i, y, -az + i, ax - i, y, -az + i, 0.065, 4, ALU);
        m.tube(ax - i, y, -az + i, ax - i, y, az - i, 0.065, 4, ALU);
    }

    /** Festoon loop just inside the eave, from pole to pole. */
    private static void eaveFestoon(MeshBuilder m, TentShape g, double inset) {
        double[] st = g.poleStations();
        double P = g.perimeterLength();
        double[] a = new double[5], b = new double[5];
        for (int k = 0; k < st.length; k++) {
            double s0 = st[k], s1 = k + 1 < st.length ? st[k + 1] : P;
            double[] n0 = stationNormal(g, s0), n1 = stationNormal(g, s1);
            g.perimeterPoint(s0, a);
            g.perimeterPoint(s1, b);
            m.festoon(a[0] - n0[0] * inset, g.Hw - 0.3, a[1] - n0[1] * inset,
                    b[0] - n1[0] * inset, g.Hw - 0.3, b[1] - n1[1] * inset, 0.3, k * 5);
        }
    }
}
