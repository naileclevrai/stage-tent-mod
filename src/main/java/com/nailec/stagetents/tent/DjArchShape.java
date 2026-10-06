package com.nailec.stagetents.tent;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/** Open DJ shelter: two plain poles and a curved clear sheet. */
public final class DjArchShape extends RectShape {
    public final double span;
    /** Height of the foot collision. The arch itself is the structure above the plates. */
    public final double legHeight;

    public DjArchShape(TentParams params, Direction facing) {
        super(params, facing, 0, params.length / 2.0 - 0.18, params.width / 2.0);
        span = az - 0.24;
        // Straight mono poles. The curved sheet starts at their tops.
        legHeight = Math.max(1.5, Math.min(H * 0.36, H - 1.8));
    }

    @Override
    protected double ridgeX(double px) {
        return px;
    }

    /** Angle 0 is the top of the +z leg, PI/2 the crown, PI the top of the -z leg. */
    public double z(double angle) {
        return span * Math.cos(angle);
    }

    public double y(double angle) {
        return legHeight + (H - legHeight) * Math.sin(angle);
    }

    /** The sail starts once the tube has risen this far. Below that, the arch stays open. */
    public static final double SAIL_SIN = 0.40;

    /**
     * Sheet on both sides of the tube. Highest on the tube, easing down toward each hem.
     * It only exists on the upper part of the arch, so the skin stays a sail.
     */
    public double sailY(double x, double angle) {
        double ux = ax <= 0.01 ? 0 : Math.max(-1, Math.min(1, x / ax));
        double along = Math.cos(ux * Math.PI * 0.5);
        double s = Math.sin(angle);
        return y(angle) - (H - legHeight) * 0.20 * s * (1 - along);
    }

    public boolean sailCovers(double angle) {
        return Math.sin(angle) >= SAIL_SIN;
    }

    public double canopyY(double x, double angle) {
        return sailY(x, angle);
    }

    @Override
    public double roofHeight(double lx, double lz) {
        if (Math.abs(lx) > ax || Math.abs(lz) > span) return Double.NaN;
        double angle = Math.acos(Math.max(-1, Math.min(1, lz / span)));
        if (!sailCovers(angle)) return Double.NaN;
        return canopyY(lx, angle);
    }

    @Override
    public boolean hasWallAt(double lx, double lz) {
        return false;
    }

    @Override
    public double eaveHeight(double s) {
        return legHeight;
    }

    @Override
    protected void poleCells(CellSink sink) {
        // Two plain poles under the middle of the tube, one on each side.
        for (int sz : new int[]{-1, 1}) {
            double wx = toWorldX(0, sz * span), wz = toWorldZ(0, sz * span);
            int dx = Mth.floor(wx + 0.5), dz = Mth.floor(wz + 0.5);
            int fx = Mth.clamp(Mth.floor((wx - dx + 0.5) * 16), 0, 15);
            int fz = Mth.clamp(Mth.floor((wz - dz + 0.5) * 8), 0, 7);
            for (int y = 0; y < (int) Math.ceil(legHeight); y++)
                sink.cell(dx, y, dz, TentPart.POLE, fx, fz);
        }
    }

    @Override
    public List<double[]> rigPoints() {
        List<double[]> out = new ArrayList<>();
        for (int i = -1; i <= 1; i++) {
            double z = i * span * 0.42;
            double y = y(Math.acos(z / span)) - 0.8;
            for (int x = (int) Math.ceil(-ax + 0.6); x <= ax - 0.6; x++) {
                out.add(new double[]{x, y, z, 0});
            }
        }
        return out;
    }

    @Override
    public List<double[]> lightPoints() {
        return List.of();
    }

    @Override
    public double extent() {
        return Math.hypot(ax, az) + 1;
    }
}
