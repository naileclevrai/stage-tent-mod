package com.nailec.stagetents.tent;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * Pagoda / barnum: a square (or stretched) frame on legs with a tall, deeply concave tensioned roof rising to a
 * point (or a short ridge when stretched). The roof height follows the square "radius" max(|x| beyond the ridge, |z|),
 * which gives the four curved hips of a real pagoda.
 */
public final class PagodaShape extends RectShape {
    public final double hw, hl;

    public PagodaShape(TentParams params, Direction facing) {
        super(params, facing, 1.5, params.length / 2.0 + params.width / 2.0, params.width / 2.0);
        hw = params.width / 2.0;
        hl = params.length / 2.0;
    }

    /** Strongly concave: steep spire at the top, nearly flat skirt at the eave. */
    public static double profile(double t) {
        double u = 1 - t;
        return 0.8 * u * u + 0.2 * u;
    }

    @Override
    protected double ridgeX(double px) {
        return Mth.clamp(px, -params.length / 2.0, params.length / 2.0);
    }

    @Override
    public double roofHeight(double lx, double lz) {
        double d = Math.max(Math.max(0, Math.abs(lx) - hl), Math.abs(lz));
        if (d > hw || Math.abs(lx) > ax) return Double.NaN;
        return Hw + (H - Hw) * profile(d / hw);
    }

    @Override
    public List<double[]> flagMounts() {
        List<double[]> out = new ArrayList<>();
        out.add(new double[]{hl, H + 0.75, 0});
        if (hl > 0) out.add(new double[]{-hl, H + 0.75, 0});
        return out;
    }

    @Override
    public List<double[]> lightPoints() {
        List<double[]> out = super.lightPoints();
        out.add(new double[]{0, Math.max(Hw, H - 2.5), 0});
        return out;
    }
}
