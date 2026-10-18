package com.nailec.stagetents.tent;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DJ cover in the Saddlespan style. Each foot is a square plinth with a straight tube rising from it. At the top of
 * the two legs, two identical arches split off, one leaning out to the front and one to the back, so that seen from
 * above they draw a leaf high over the booth. A clear PVC membrane is tensioned between them and sags into a saddle.
 *
 * <p>The plate is the first foot, at the local origin; the second foot stands {@code width} blocks away along local
 * +z. Arch parameter {@code u} runs from -1 (plate leg) to 1 (far leg); {@code w(u)} is 0 on the legs and 1 at
 * the crown. The leg height is the tent's wall height. Membrane parameter {@code t} runs from 0 on the front arch (+x) to 1 on the back arch (-x).
 */
public final class DjArchShape extends RectShape {
    /** Height of the square plinth under each leg. */
    public static final double PLINTH = 0.45;
    /** Gap between a tube's centre line and the hem of the sheet, bridged by the lacing straps. */
    public static final double GAP = 0.30;

    /** Distance between the two feet. */
    public final double span;
    /** Local z of the midpoint between the feet. */
    public final double zc;
    public final double half, lean, sag;
    /** Top of the straight legs, where the two arches split. */
    public final double leg;

    public DjArchShape(TentParams params, Direction facing) {
        super(params, facing, 2.0, Math.max(1.0, params.length / 2.0) + 0.3, params.width / 2.0);
        span = params.width;
        half = span / 2.0;
        zc = half;
        lean = Math.max(1.0, params.length / 2.0);
        leg = Math.max(1.5, Hw);
        sag = Math.min(0.24 * 2 * lean, 0.35 * (H - leg));
    }

    @Override
    protected double ridgeX(double px) {
        return px;
    }

    /**
     * Arch profile, 0 on the legs and 1 at the crown. The (1 + 0.6 u^2) factor flattens the crown into a round arch
     * while the ends stay sharp, so the plan keeps its leaf points and the arches spring steeply off the legs.
     */
    public double w(double u) {
        double u2 = u * u;
        return (1 - u2) * (1 + 0.6 * u2);
    }

    public double z(double u) {
        return zc + half * u;
    }

    /** Centre line of an arch: side +1 is the front arch, -1 the back arch. */
    public double[] arch(double u, int side) {
        double w = w(u);
        return new double[]{side * lean * w, leg + (H - leg) * w, z(u)};
    }

    /** Membrane point between the arches, sagging in the middle. */
    public double[] membrane(double u, double t) {
        double w = w(u);
        return new double[]{lean * w * (1 - 2 * t), leg + (H - leg) * w - sag * w * 4 * t * (1 - t), z(u)};
    }

    /** Share of the saddle left open on each side, between the tube and the hem. 0.5 where the sheet ends. */
    public double inset(double u) {
        double width = 2 * lean * w(u);
        return width <= 2 * GAP ? 0.5 : GAP / width;
    }

    /** The PVC sheet itself: {@code s} 0 on the front hem, 1 on the back hem, on the saddle inside the gaps. */
    public double[] sheet(double u, double s) {
        double e = inset(u);
        return membrane(u, e + (1 - 2 * e) * s);
    }

    @Override
    public double roofHeight(double lx, double lz) {
        double u = (lz - zc) / half;
        if (Math.abs(u) >= 1) return Double.NaN;
        double xf = lean * w(u) * (1 - 2 * inset(u));
        if (xf < 0.05 || Math.abs(lx) > xf) return Double.NaN;
        return sheet(u, (xf - lx) / (2 * xf))[1];
    }

    @Override
    public double perimeterDistance(double lx, double lz) {
        return Math.max(Math.abs(lx) - ax, Math.abs(lz - zc) - az);
    }

    @Override
    public boolean hasWallAt(double lx, double lz) {
        return false;
    }

    @Override
    public double eaveHeight(double s) {
        return leg;
    }

    @Override
    public double footprint() {
        return span + 1;
    }

    @Override
    public double extent() {
        return Math.hypot(ax, span) + 1;
    }

    @Override
    protected void poleCells(CellSink sink) {
        // The two legs, from just above the plinth to the split.
        Set<Long> seen = new HashSet<>();
        for (double zf : new double[]{0, span}) {
            double wx = toWorldX(0, zf), wz = toWorldZ(0, zf);
            int dx = Mth.floor(wx + 0.5), dz = Mth.floor(wz + 0.5);
            int fx = Mth.clamp(Mth.floor((wx - dx + 0.5) * 16), 0, 15);
            int fz = Mth.clamp(Mth.floor((wz - dz + 0.5) * 8), 0, 7);
            for (int y = 1; y < Math.ceil(leg); y++) {
                seen.add((((long) dx & 0xFFFFF) << 40) | (((long) y & 0xFFFFF) << 20) | ((long) dz & 0xFFFFF));
                sink.cell(dx, y, dz, TentPart.POLE, fx, fz);
            }
        }
        // Both arches, sampled every few decimetres.
        int steps = Mth.clamp((int) Math.round((span + 2 * H) / 0.3), 24, 240);
        for (int i = 0; i <= steps; i++) {
            double u = -1 + 2.0 * i / steps;
            for (int side : new int[]{1, -1}) {
                double[] p = arch(u, side);
                double wx = toWorldX(p[0], p[2]), wz = toWorldZ(p[0], p[2]);
                int dx = Mth.floor(wx + 0.5), dz = Mth.floor(wz + 0.5), dy = Mth.floor(p[1]);
                if (!seen.add((((long) dx & 0xFFFFF) << 40) | (((long) dy & 0xFFFFF) << 20) | ((long) dz & 0xFFFFF))) continue;
                int fx = Mth.clamp(Mth.floor((wx - dx + 0.5) * 16), 0, 15);
                int fz = Mth.clamp(Mth.floor((wz - dz + 0.5) * 8), 0, 7);
                sink.cell(dx, dy, dz, TentPart.POLE, fx, fz);
            }
        }
    }

    @Override
    public List<double[]> rigPoints() {
        // Short bars along x under the membrane, at the crown and halfway to each foot.
        List<double[]> out = new ArrayList<>();
        for (double u : new double[]{-0.45, 0, 0.45}) {
            double low = Double.MAX_VALUE;
            for (int k = 0; k <= 8; k++) low = Math.min(low, membrane(u, k / 8.0)[1]);
            double y = low - 0.8;
            if (y < 2.2) continue;
            double xf = lean * w(u);
            for (int x = (int) Math.ceil(-xf + 0.6); x <= xf - 0.6; x++) {
                out.add(new double[]{x, y, z(u), 0});
            }
        }
        return out;
    }

    @Override
    public List<double[]> lightPoints() {
        return List.of();
    }
}
