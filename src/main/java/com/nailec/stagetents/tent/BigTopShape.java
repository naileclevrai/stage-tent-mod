package com.nailec.stagetents.tent;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * Circus big top. The footprint is a stadium: a straight section of length {@code L} along the ridge capped by two
 * half circles of radius {@code R}. Every perimeter point is joined to its nearest ridge point by a radial line, and
 * the canvas height along that line follows a concave tension profile from the ridge down to the eave.
 */
public final class BigTopShape extends TentShape {
    /** Depth of the eave sag between two side poles. */
    public static final double SCALLOP = 0.22;
    /** Base sag of the canvas between two seams; the mesh adds 0.004 per block of radius. */
    public static final double SEAM_SAG = 0.05;

    public final double R, L, half, P;
    public final double[] mastX;
    public final int poleCount;
    public final int seams;
    public final double entranceHalf;

    public BigTopShape(TentParams params, Direction facing) {
        super(params, facing, 3);
        R = params.width / 2.0;
        L = params.length;
        half = L / 2.0;
        int m = params.effectiveMasts();
        mastX = new double[m];
        for (int i = 0; i < m; i++) {
            mastX[i] = m == 1 ? 0 : Math.round(-half + i * L / (m - 1));
        }
        P = 2 * L + 2 * Math.PI * R;
        poleCount = Math.max(6, (int) Math.round(P / params.poleSpacing));
        seams = Math.max(12, 2 * (int) Math.round(P / 3.5));
        entranceHalf = Mth.clamp(R * 0.4, 2.0, 6.0);
    }

    /** Concave tension profile: 1 at the ridge, 0 at the eave, never quite flat at the eave. */
    public static double profile(double t) {
        double u = 1 - t;
        return 0.55 * u * u + 0.45 * u;
    }

    /** Height of the canvas on the ridge line: peaks at each mast, saddles in between. */
    public double ridgeHeight(double x) {
        if (mastX.length == 1) return H;
        for (int i = 0; i < mastX.length - 1; i++) {
            double a = mastX[i], b = mastX[i + 1];
            if (x <= b || i == mastX.length - 2) {
                double gap = b - a;
                double u = Mth.clamp((x - a) / gap, 0, 1);
                double sag = Math.min(0.2 * gap, 0.3 * (H - Hw));
                return H - sag * Math.sin(Math.PI * u);
            }
        }
        return H;
    }

    @Override
    public double perimeterLength() {
        return P;
    }

    @Override
    public double eaveHeight(double s) {
        return Hw - SCALLOP * Math.sin(Math.PI * poleFrac(s));
    }

    /** Fraction of the way from one side pole to the next at abscissa {@code s}. */
    public double poleFrac(double s) {
        return frac(s * poleCount / P);
    }

    @Override
    public double[] poleStations() {
        double[] out = new double[poleCount];
        for (int k = 0; k < poleCount; k++) out[k] = P * k / poleCount;
        return out;
    }

    @Override
    public int stripeAt(double s) {
        return Math.floorMod((int) Math.floor(s * seams / P), seams);
    }

    @Override
    public double[] panelBounds() {
        double[] out = new double[seams + 1];
        for (int i = 0; i <= seams; i++) out[i] = P * i / seams;
        return out;
    }

    @Override
    public double roofHeight(double lx, double lz) {
        double qx = Mth.clamp(lx, -half, half);
        double d = Math.hypot(lx - qx, lz);
        if (d > R) return Double.NaN;
        return Hw + (ridgeHeight(qx) - Hw) * profile(d / R);
    }

    @Override
    public double perimeterDistance(double lx, double lz) {
        double qx = Mth.clamp(lx, -half, half);
        return Math.hypot(lx - qx, lz) - R;
    }

    @Override
    public double[] wallPlane(double lx, double lz) {
        double qx = Mth.clamp(lx, -half, half);
        double dx = lx - qx, d = Math.hypot(dx, lz);
        return d < 1e-6 ? new double[]{1, 0, R} : new double[]{dx / d, lz / d, R - d};
    }

    @Override
    public void perimeterPoint(double s, double[] out) {
        s = s - Math.floor(s / P) * P;
        double arc = Math.PI * R;
        if (s < L) {
            double x = half - s;
            set(out, x, R, 0, 1, x);
            return;
        }
        s -= L;
        if (s < arc) {
            double th = Math.PI / 2 + s / R, nx = Math.cos(th), nz = Math.sin(th);
            set(out, -half + R * nx, R * nz, nx, nz, -half);
            return;
        }
        s -= arc;
        if (s < L) {
            double x = -half + s;
            set(out, x, -R, 0, -1, x);
            return;
        }
        s -= L;
        double th = 1.5 * Math.PI + s / R, nx = Math.cos(th), nz = Math.sin(th);
        set(out, half + R * nx, R * nz, nx, nz, half);
    }

    @Override
    public boolean isEntrance(double lx, double lz) {
        if (params.entrance == Entrance.OPEN_FRONT) return isFrontSide(lx, lz);
        if (params.entrance == Entrance.COUNTER_BACK) return isBackSide(lx, lz);
        if (params.entrance == Entrance.NONE || params.entrance == Entrance.COUNTER || Math.abs(lz) >= entranceHalf) return false;
        return lx > half + 0.01 || (params.entrance == Entrance.BOTH && lx < -half - 0.01);
    }

    @Override
    public boolean isFrontSide(double lx, double lz) {
        return lx > half + 0.01;
    }

    @Override
    public boolean isBackSide(double lx, double lz) {
        return lx < -half - 0.01;
    }

    @Override
    public double footprint() {
        return half + R;
    }

    @Override
    public double extent() {
        return half + R + (params.guyRopes ? Hw * 0.8 + 1.5 : 1);
    }

    /** Radial position of the quarter poles, as a fraction of the way from the ridge to the eave. */
    public static final double QUARTER_T = 0.55;

    /**
     * Quarter poles: the ring of intermediate poles that push the canvas up between the king poles and the walls.
     * Only big enough tents get them. Returns local x, z, top height.
     */
    public List<double[]> quarterPoles() {
        List<double[]> out = new ArrayList<>();
        if (R < 7) return out;
        double[] per = new double[5];
        double[] st = poleStations();
        int step = Math.max(1, (int) Math.ceil(5.0 / (params.poleSpacing * QUARTER_T))); // about 5 blocks apart
        for (int k = 0; k < st.length; k += step) {
            perimeterPoint(st[k], per);
            double x = per[4] + (per[0] - per[4]) * QUARTER_T, z = per[1] * QUARTER_T;
            // Stay under the canvas where it sags between seams (see the roof ripple in the mesh).
            out.add(new double[]{x, z, roofHeight(x, z) - SEAM_SAG - 0.004 * R - 0.12});
        }
        return out;
    }

    @Override
    protected void poleCells(CellSink sink) {
        for (double mx : mastX) poleColumn(sink, mx, 0, 0);
        for (double[] q : quarterPoles()) poleColumn(sink, q[0], q[1], 0);
    }

    @Override
    public List<double[]> flagMounts() {
        List<double[]> out = new ArrayList<>();
        for (double mx : mastX) out.add(new double[]{mx, ridgeHeight(mx) + 1.4, 0});
        return out;
    }

    /** A rigging ring around each king pole, joined along the ridge between masts. */
    @Override
    public List<double[]> rigPoints() {
        List<double[]> out = new ArrayList<>();
        double y = Hw + (H - Hw) * 0.45;
        double r = Mth.clamp(R * 0.25, 2, 5);
        java.util.Set<Long> seen = new java.util.HashSet<>();
        for (double mx : mastX) {
            int n = (int) Math.ceil(2 * Math.PI * r * 2);
            for (int i = 0; i < n; i++) {
                double a = 2 * Math.PI * i / n;
                double x = mx + r * Math.cos(a), z = r * Math.sin(a);
                long key = ((long) Math.round(x) << 32) ^ (Math.round(z) & 0xFFFFFFFFL);
                if (seen.add(key)) out.add(new double[]{Math.round(x), y, Math.round(z), Math.abs(Math.cos(a)) > 0.7 ? 1 : 0});
            }
        }
        for (int i = 0; i + 1 < mastX.length; i++) {
            for (double x = mastX[i] + r + 1; x <= mastX[i + 1] - r - 1 + 1e-6; x += 1) out.add(new double[]{x, y, -r, 0});
            for (double x = mastX[i] + r + 1; x <= mastX[i + 1] - r - 1 + 1e-6; x += 1) out.add(new double[]{x, y, r, 0});
        }
        return out;
    }

    @Override
    public List<double[]> lightPoints() {
        List<double[]> out = super.lightPoints();
        for (double mx : mastX) out.add(new double[]{mx + 1, Hw + (H - Hw) * 0.35, 0});
        return out;
    }
}
