package com.nailec.stagetents.tent;

import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * Reception / frame tent: aluminium legs every bay along both long sides, a two-pitch roof on rafters, and gable ends.
 * The ridge runs along x for the full length.
 */
public final class FrameShape extends RectShape {
    public final int bays;

    public FrameShape(TentParams params, Direction facing) {
        super(params, facing, 1, params.length / 2.0, params.width / 2.0);
        bays = Math.max(1, (int) Math.round(params.length / (double) params.poleSpacing));
    }

    @Override
    protected double stripeWidth() {
        return 1.25;
    }

    @Override
    protected int legSegments(int edge, double len) {
        if (edge == 0 || edge == 2) return Math.max(1, (int) Math.round(params.length / (double) params.poleSpacing));
        // Gable ends: always a centre post under the ridge.
        return Math.max(2, 2 * (int) Math.round(len / (2.0 * params.poleSpacing)));
    }

    @Override
    protected double ridgeX(double px) {
        return px;
    }

    /** x of rafter {@code k}, 0..bays. */
    public double bayX(int k) {
        return -ax + 2 * ax * k / bays;
    }

    @Override
    public double roofHeight(double lx, double lz) {
        if (Math.abs(lx) > ax || Math.abs(lz) > az) return Double.NaN;
        return Hw + (H - Hw) * (1 - Math.abs(lz) / az);
    }

    @Override
    protected double entranceHalfWidth() {
        return Math.min(az - 0.6, Math.max(1.5, az * 0.5));
    }

    /** Rigging bars along the ridge and halfway down each slope. */
    @Override
    public List<double[]> rigPoints() {
        List<double[]> out = new ArrayList<>();
        for (double z : new double[]{0, az * 0.5, -az * 0.5}) {
            double y = roofHeight(0, z) - 1.3;
            if (y < Hw - 0.5) continue;
            for (double x = -ax + 1; x <= ax - 1 + 1e-6; x += 1) out.add(new double[]{x, y, z, 0});
        }
        return out;
    }

    @Override
    public List<double[]> lightPoints() {
        List<double[]> out = super.lightPoints();
        for (int k = 0; k < bays; k++) out.add(new double[]{(bayX(k) + bayX(k + 1)) / 2, H - 1.2, 0});
        return out;
    }
}
