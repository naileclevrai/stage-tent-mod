package com.nailec.stagetents.tent;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * Stretch (Bedouin) tent: one continuous membrane pushed up by masts and pulled down at the edge alternately by short
 * edge poles and ground anchors, with the edge curving in between them.
 *
 * <p>The footprint is the convex hull of the masts grown by the overhang ({@code width}). The canvas height is solved
 * as a tensioned membrane (Laplace equation with a little sag) with the masts and the edge held fixed, which gives
 * the saddles and peaks of real stretch fabric.
 */
public final class StretchShape extends TentShape {
    private static final double GRID = 0.4;
    private static final double CAP = 0.4;

    /** Masts in local coordinates: x, z, height. The plate itself is the first mast. */
    public final List<double[]> masts = new ArrayList<>();
    /** Edge anchors: abscissa and whether it is a pole (true) or a ground anchor (false). */
    public final double[] anchorS;
    public final boolean[] anchorPole;
    private final double P;
    private final int nb;
    private final double[] bx, bz, bnx, bnz, bh;
    private final double cx, cz;
    // Membrane height field.
    private final double gx0, gz0;
    private final int gw, gh;
    private final double[] field;
    private final boolean[] inside;
    private final double ext;

    public StretchShape(TentParams params, Direction facing) {
        super(params, facing, 1);
        masts.add(new double[]{0, 0, Math.max(params.height, Hw + 1)});
        for (int[] sp : params.stretchPoles) {
            double lx = toLocalX(sp[0], sp[1]), lz = toLocalZ(sp[0], sp[1]);
            if (Math.hypot(lx, lz) < 1) continue;
            masts.add(new double[]{lx, lz, Math.max(sp[2], Hw + 1)});
        }

        // Rounded convex hull of the masts, grown by the overhang.
        double margin = params.width;
        List<double[]> hull = convexHull(masts);
        List<double[]> ring = new ArrayList<>(); // x, z, nx, nz
        int nh = hull.size();
        for (int i = 0; i < nh; i++) {
            double[] prev = hull.get((i + nh - 1) % nh), cur = hull.get(i), next = hull.get((i + 1) % nh);
            double a0, a1;
            if (nh == 1) {
                a0 = 0;
                a1 = 2 * Math.PI;
            } else {
                a0 = Math.atan2(-(cur[0] - prev[0]), cur[1] - prev[1]); // outward normal of the incoming edge
                a1 = Math.atan2(-(next[0] - cur[0]), next[1] - cur[1]);
                if (nh == 2 && i == 0) a0 = a1 - Math.PI;
                while (a1 < a0) a1 += 2 * Math.PI;
                if (nh == 2 && i == 1) a1 = a0 + Math.PI;
            }
            int steps = Math.max(1, (int) Math.ceil((a1 - a0) * margin / 0.25));
            for (int k = 0; k <= steps; k++) {
                double a = a0 + (a1 - a0) * k / steps;
                ring.add(new double[]{cur[0] + Math.cos(a) * margin, cur[1] + Math.sin(a) * margin, Math.cos(a), Math.sin(a)});
            }
        }
        // Make the loop counter-clockwise like the other tents (+z side towards -x).
        if (signedArea(ring) < 0) java.util.Collections.reverse(ring);
        double[] cum = new double[ring.size() + 1];
        for (int i = 0; i < ring.size(); i++) {
            double[] a = ring.get(i), b = ring.get((i + 1) % ring.size());
            cum[i + 1] = cum[i] + Math.hypot(b[0] - a[0], b[1] - a[1]);
        }
        double p0 = cum[ring.size()];

        int k = Math.max(4, 2 * (int) Math.round(p0 / (2.0 * params.poleSpacing)));
        anchorS = new double[k];
        anchorPole = new boolean[k];
        for (int i = 0; i < k; i++) {
            anchorS[i] = p0 * i / k;
            anchorPole[i] = i % 2 == 0;
        }

        // Scalloped edge: pulled in between anchors, height interpolated between pole tops and ground anchors.
        nb = Math.max(32, (int) Math.ceil(p0 / 0.3));
        P = p0;
        bx = new double[nb];
        bz = new double[nb];
        bnx = new double[nb];
        bnz = new double[nb];
        bh = new double[nb];
        double seg = p0 / k;
        int ri = 0;
        double sumX = 0, sumZ = 0;
        for (int i = 0; i < nb; i++) {
            double s = p0 * i / nb;
            while (ri < ring.size() - 1 && cum[ri + 1] < s) ri++;
            double[] a = ring.get(ri), b = ring.get((ri + 1) % ring.size());
            double f = (s - cum[ri]) / Math.max(1e-9, cum[ri + 1] - cum[ri]);
            double x = a[0] + (b[0] - a[0]) * f, z = a[1] + (b[1] - a[1]) * f;
            double nx = a[2] + (b[2] - a[2]) * f, nz = a[3] + (b[3] - a[3]) * f, nl = Math.hypot(nx, nz);
            nx /= nl;
            nz /= nl;
            int ai = Math.min(k - 1, (int) Math.floor(s / seg));
            double u = s / seg - ai;
            double pull = 0.13 * seg * Math.sin(Math.PI * u);
            bx[i] = x - nx * pull;
            bz[i] = z - nz * pull;
            bnx[i] = nx;
            bnz[i] = nz;
            double h0 = anchorHeight(ai), h1 = anchorHeight((ai + 1) % k);
            double sm = u * u * (3 - 2 * u);
            bh[i] = h0 + (h1 - h0) * sm - 0.04 * seg * Math.sin(Math.PI * u);
            sumX += bx[i];
            sumZ += bz[i];
        }
        cx = sumX / nb;
        cz = sumZ / nb;

        // Membrane solve.
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (int i = 0; i < nb; i++) {
            minX = Math.min(minX, bx[i]);
            maxX = Math.max(maxX, bx[i]);
            minZ = Math.min(minZ, bz[i]);
            maxZ = Math.max(maxZ, bz[i]);
        }
        gx0 = minX - 2 * GRID;
        gz0 = minZ - 2 * GRID;
        gw = (int) Math.ceil((maxX - minX) / GRID) + 5;
        gh = (int) Math.ceil((maxZ - minZ) / GRID) + 5;
        field = new double[gw * gh];
        inside = new boolean[gw * gh];
        boolean[] fixed = new boolean[gw * gh];
        double meanEdge = 0;
        for (double h : bh) meanEdge += h;
        meanEdge /= nb;
        for (int j = 0; j < gh; j++) {
            for (int i = 0; i < gw; i++) {
                int id = j * gw + i;
                double x = gx0 + i * GRID, z = gz0 + j * GRID;
                inside[id] = pointInPolygon(x, z);
                if (!inside[id]) {
                    field[id] = edgeHeightNear(x, z);
                    fixed[id] = true;
                } else {
                    field[id] = meanEdge;
                }
                for (double[] m : masts) {
                    if (Math.hypot(x - m[0], z - m[1]) <= CAP) {
                        field[id] = m[2];
                        fixed[id] = true;
                    }
                }
            }
        }
        double sag = 0.002; // slight droop of the fabric between supports
        // Successive over-relaxation, stopped once no cell moves by more than a millimetre.
        for (int it = 0; it < 600; it++) {
            double maxDelta = 0;
            for (int j = 1; j < gh - 1; j++) {
                for (int i = 1; i < gw - 1; i++) {
                    int id = j * gw + i;
                    if (fixed[id]) continue;
                    double avg = (field[id - 1] + field[id + 1] + field[id - gw] + field[id + gw]) * 0.25 - sag;
                    double delta = 1.85 * (avg - field[id]);
                    field[id] += delta;
                    maxDelta = Math.max(maxDelta, Math.abs(delta));
                }
            }
            if (maxDelta < 1e-3) break;
        }
        double far = 0;
        for (int i = 0; i < nb; i++) far = Math.max(far, Math.hypot(bx[i], bz[i]));
        ext = far;
    }

    private double anchorHeight(int i) {
        return anchorPole[i] ? Hw : 0.35;
    }

    private static List<double[]> convexHull(List<double[]> pts) {
        List<double[]> p = new ArrayList<>(pts);
        p.sort((a, b) -> a[0] != b[0] ? Double.compare(a[0], b[0]) : Double.compare(a[1], b[1]));
        if (p.size() < 3) return p;
        List<double[]> h = new ArrayList<>();
        for (int pass = 0; pass < 2; pass++) {
            int start = h.size();
            for (double[] q : p) {
                while (h.size() >= start + 2 && cross(h.get(h.size() - 2), h.get(h.size() - 1), q) <= 0) h.remove(h.size() - 1);
                h.add(q);
            }
            h.remove(h.size() - 1);
            java.util.Collections.reverse(p);
        }
        return h;
    }

    private static double cross(double[] o, double[] a, double[] b) {
        return (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0]);
    }

    private static double signedArea(List<double[]> ring) {
        double a = 0;
        for (int i = 0; i < ring.size(); i++) {
            double[] p = ring.get(i), q = ring.get((i + 1) % ring.size());
            a += p[0] * q[1] - q[0] * p[1];
        }
        return a;
    }

    private boolean pointInPolygon(double x, double z) {
        boolean in = false;
        for (int i = 0, j = nb - 1; i < nb; j = i++) {
            if ((bz[i] > z) != (bz[j] > z) && x < (bx[j] - bx[i]) * (z - bz[i]) / (bz[j] - bz[i]) + bx[i]) in = !in;
        }
        return in;
    }

    private int nearestEdge(double x, double z) {
        int best = 0;
        double bd = Double.MAX_VALUE;
        for (int i = 0; i < nb; i++) {
            double d = (bx[i] - x) * (bx[i] - x) + (bz[i] - z) * (bz[i] - z);
            if (d < bd) {
                bd = d;
                best = i;
            }
        }
        return best;
    }

    private double edgeHeightNear(double x, double z) {
        return bh[nearestEdge(x, z)];
    }

    /** Centre the canvas is meshed from (radial lines run from here to the edge). */
    public double centreX() {
        return cx;
    }

    public double centreZ() {
        return cz;
    }

    private double sampleField(double lx, double lz) {
        double fx = (lx - gx0) / GRID, fz = (lz - gz0) / GRID;
        int i = Mth.clamp(Mth.floor(fx), 0, gw - 2), j = Mth.clamp(Mth.floor(fz), 0, gh - 2);
        double u = Mth.clamp(fx - i, 0, 1), v = Mth.clamp(fz - j, 0, 1);
        double a = field[j * gw + i], b = field[j * gw + i + 1], c = field[(j + 1) * gw + i], d = field[(j + 1) * gw + i + 1];
        return (a * (1 - u) + b * u) * (1 - v) + (c * (1 - u) + d * u) * v;
    }

    /** Membrane height anywhere inside, including right at the edge. */
    public double surfaceHeight(double lx, double lz) {
        return sampleField(lx, lz);
    }

    // ------------------------------------------------------------------ TentShape

    @Override
    public double perimeterLength() {
        return P;
    }

    @Override
    public void perimeterPoint(double s, double[] out) {
        s = s - Math.floor(s / P) * P;
        double f = s / P * nb;
        int i = Mth.floor(f) % nb, j = (i + 1) % nb;
        double u = f - Math.floor(f);
        set(out, bx[i] + (bx[j] - bx[i]) * u, bz[i] + (bz[j] - bz[i]) * u, bnx[i], bnz[i], cx);
    }

    @Override
    public double eaveHeight(double s) {
        s = s - Math.floor(s / P) * P;
        double f = s / P * nb;
        int i = Mth.floor(f) % nb, j = (i + 1) % nb;
        double u = f - Math.floor(f);
        return bh[i] + (bh[j] - bh[i]) * u;
    }

    @Override
    public double[] poleStations() {
        return anchorS;
    }

    @Override
    public int stripeAt(double s) {
        return (int) Math.floor(s / P * anchorS.length);
    }

    @Override
    public double[] panelBounds() {
        int n = anchorS.length * 2;
        double[] out = new double[n + 1];
        for (int i = 0; i <= n; i++) out[i] = P * i / n;
        return out;
    }

    @Override
    public double roofHeight(double lx, double lz) {
        return pointInPolygon(lx, lz) ? sampleField(lx, lz) : Double.NaN;
    }

    @Override
    public double perimeterDistance(double lx, double lz) {
        int i = nearestEdge(lx, lz);
        double d = Math.hypot(bx[i] - lx, bz[i] - lz);
        return pointInPolygon(lx, lz) ? -d : d;
    }

    @Override
    public double[] wallPlane(double lx, double lz) {
        int i = nearestEdge(lx, lz);
        return new double[]{bnx[i], bnz[i], -perimeterDistance(lx, lz)};
    }

    @Override
    public double footprint() {
        return ext;
    }

    @Override
    public double top() {
        double t = H;
        for (double[] m : masts) t = Math.max(t, m[2]);
        return t;
    }

    @Override
    public double extent() {
        return ext + Hw * 0.8 + 1.5;
    }

    @Override
    public boolean isEntrance(double lx, double lz) {
        return true;
    }

    @Override
    public boolean hasWallAt(double lx, double lz) {
        return false;
    }

    @Override
    protected void poleCells(CellSink sink) {
        for (double[] m : masts) poleColumn(sink, m[0], m[1], 0);
        double[] per = new double[5];
        for (int i = 0; i < anchorS.length; i++) {
            if (!anchorPole[i]) continue;
            perimeterPoint(anchorS[i], per);
            poleColumn(sink, per[0], per[1], 0);
        }
    }

    @Override
    public List<double[]> lightPoints() {
        List<double[]> out = new ArrayList<>();
        for (double[] m : masts) out.add(new double[]{m[0] + 0.8, Math.max(1, m[2] * 0.45), m[1]});
        return out;
    }

    @Override
    public java.util.List<double[]> flagMounts() {
        List<double[]> out = new ArrayList<>();
        for (double[] m : masts) out.add(new double[]{m[0], m[2] + 0.9, m[1]});
        return out;
    }
}
