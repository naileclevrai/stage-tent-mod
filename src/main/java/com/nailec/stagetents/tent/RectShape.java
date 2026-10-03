package com.nailec.stagetents.tent;

import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * Base for tents on a rectangular footprint of half sizes {@code ax} (along x) and {@code az} (across), held by
 * rigid eave beams on legs. The perimeter starts at the (+x, +z) corner.
 */
public abstract class RectShape extends TentShape {
    public final double ax, az, P;
    protected final double[] edgeStart = new double[5];
    protected final double[] stations;
    private final int[] panelsBefore = new int[5];
    private final int[] panelsPerEdge = new int[4];

    protected RectShape(TentParams params, Direction facing, double minRise, double ax, double az) {
        super(params, facing, minRise);
        this.ax = ax;
        this.az = az;
        double[] len = {2 * ax, 2 * az, 2 * ax, 2 * az};
        for (int e = 0; e < 4; e++) {
            edgeStart[e + 1] = edgeStart[e] + len[e];
            panelsPerEdge[e] = Math.max(1, (int) Math.round(len[e] / stripeWidth()));
            panelsBefore[e + 1] = panelsBefore[e] + panelsPerEdge[e];
        }
        P = edgeStart[4];
        List<Double> st = new ArrayList<>();
        for (int e = 0; e < 4; e++) {
            int n = legSegments(e, len[e]);
            for (int k = 0; k < n; k++) st.add(edgeStart[e] + len[e] * k / n);
        }
        stations = st.stream().mapToDouble(Double::doubleValue).toArray();
    }

    protected double stripeWidth() {
        return 1.6;
    }

    /** Number of leg-to-leg sections on an edge. */
    protected int legSegments(int edge, double len) {
        return Math.max(1, (int) Math.round(len / params.poleSpacing));
    }

    public double edgeLength(int e) {
        return edgeStart[e + 1] - edgeStart[e];
    }

    public int edgeAt(double s) {
        for (int e = 0; e < 3; e++) if (s < edgeStart[e + 1]) return e;
        return 3;
    }

    @Override
    public double perimeterLength() {
        return P;
    }

    @Override
    public void perimeterPoint(double s, double[] out) {
        s = s - Math.floor(s / P) * P;
        int e = edgeAt(s);
        double u = s - edgeStart[e];
        double px, pz, nx, nz;
        switch (e) {
            case 0 -> { px = ax - u; pz = az; nx = 0; nz = 1; }
            case 1 -> { px = -ax; pz = az - u; nx = -1; nz = 0; }
            case 2 -> { px = -ax + u; pz = -az; nx = 0; nz = -1; }
            default -> { px = ax; pz = -az + u; nx = 1; nz = 0; }
        }
        set(out, px, pz, nx, nz, ridgeX(px));
    }

    /** x of the ridge point the roof line from perimeter point {@code px} starts at. */
    protected abstract double ridgeX(double px);

    @Override
    public double eaveHeight(double s) {
        return Hw;
    }

    @Override
    public double[] poleStations() {
        return stations;
    }

    @Override
    public int stripeAt(double s) {
        s = s - Math.floor(s / P) * P;
        int e = edgeAt(s);
        int local = (int) Math.floor((s - edgeStart[e]) / edgeLength(e) * panelsPerEdge[e]);
        return panelsBefore[e] + Math.min(local, panelsPerEdge[e] - 1);
    }

    @Override
    public double[] panelBounds() {
        double[] out = new double[panelsBefore[4] + 1];
        int k = 0;
        for (int e = 0; e < 4; e++) {
            for (int i = 0; i < panelsPerEdge[e]; i++) out[k++] = edgeStart[e] + edgeLength(e) * i / panelsPerEdge[e];
        }
        out[k] = P;
        return out;
    }

    @Override
    public double perimeterDistance(double lx, double lz) {
        return Math.max(Math.abs(lx) - ax, Math.abs(lz) - az);
    }

    @Override
    public double[] wallPlane(double lx, double lz) {
        double ex = Math.abs(lx) - ax, ez = Math.abs(lz) - az;
        double sx = lx >= 0 ? 1 : -1, sz = lz >= 0 ? 1 : -1;
        if (ex >= -0.5 && ez >= -0.5) {
            // Corner cell: a diagonal panel joining the ends of the two neighbouring side panels, so it closes the
            // corner without sticking out of it.
            double k = Math.sqrt(0.5);
            double mx = (sx * (Math.abs(lx) - 0.5) + sx * ax) / 2, mz = (sz * az + sz * (Math.abs(lz) - 0.5)) / 2;
            return new double[]{sx * k, sz * k, (mx - lx) * sx * k + (mz - lz) * sz * k};
        }
        return ex > ez ? new double[]{sx, 0, -ex} : new double[]{0, sz, -ez};
    }

    /** Half width of the front/back opening. */
    protected double entranceHalfWidth() {
        return az - 0.6;
    }

    /** Perimeter edge nearest to a local point: 0 = +z (left seen from the front), 1 = back, 2 = -z, 3 = front. */
    public int edgeOfPoint(double lx, double lz) {
        return Math.abs(lx) - ax > Math.abs(lz) - az ? (lx > 0 ? 3 : 1) : (lz > 0 ? 0 : 2);
    }

    public boolean isJoined(int edge) {
        return (params.joined & (1 << edge)) != 0;
    }

    public boolean isJoinedAt(double lx, double lz) {
        return params.joined != 0 && isJoined(edgeOfPoint(lx, lz));
    }

    @Override
    public boolean hasWallAt(double lx, double lz) {
        return super.hasWallAt(lx, lz) && !isJoinedAt(lx, lz);
    }

    @Override
    public double backX() {
        return -ax;
    }

    /** Default rigging: a ring of bars one block inside the eave. */
    @Override
    public List<double[]> rigPoints() {
        List<double[]> out = new ArrayList<>();
        double y = Math.max(1, Hw - 1);
        double ix = ax - 1, iz = az - 1;
        if (ix < 0.5 || iz < 0.5) return out;
        for (double x = -ix; x <= ix + 1e-6; x += 1) {
            out.add(new double[]{x, y, iz, 0});
            out.add(new double[]{x, y, -iz, 0});
        }
        for (double z = -iz + 1; z <= iz - 1 + 1e-6; z += 1) {
            out.add(new double[]{ix, y, z, 1});
            out.add(new double[]{-ix, y, z, 1});
        }
        return out;
    }

    @Override
    public boolean isFrontSide(double lx, double lz) {
        return lx > ax - 1 + 1e-3 && Math.abs(lx) - ax >= Math.abs(lz) - az - 1e-6;
    }

    @Override
    public boolean isBackSide(double lx, double lz) {
        return lx < -ax + 1 - 1e-3 && Math.abs(lx) - ax >= Math.abs(lz) - az - 1e-6;
    }

    @Override
    public boolean isEntrance(double lx, double lz) {
        if (params.entrance == Entrance.OPEN_FRONT) return isFrontSide(lx, lz);
        if (params.entrance == Entrance.COUNTER_BACK) return isBackSide(lx, lz);
        if (params.entrance == Entrance.NONE || params.entrance == Entrance.COUNTER || Math.abs(lz) >= entranceHalfWidth()) return false;
        return lx > ax - 1 + 1e-3 || (params.entrance == Entrance.BOTH && lx < -ax + 1 - 1e-3);
    }

    @Override
    public double footprint() {
        return Math.max(ax, az);
    }

    @Override
    public double extent() {
        return Math.hypot(ax, az) + (params.guyRopes ? Hw * 0.8 + 1.5 : 1);
    }

    @Override
    protected void poleCells(CellSink sink) {
        double[] per = new double[5];
        for (double s : stations) {
            perimeterPoint(s, per);
            double ix = per[0] - Math.signum(per[0]) * 0.15, iz = per[1] - Math.signum(per[1]) * 0.15;
            poleColumn(sink, ix, iz, 0);
        }
    }
}
