package com.nailec.stagetents.tent;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

/**
 * Folding gazebo ("barnum pliant"): a light scissor frame on four legs with a low hipped canopy. Length is always at
 * least the width so the short ridge runs along x.
 */
public final class GazeboShape extends RectShape {
    /** Half length of the short ridge of a rectangular canopy. */
    public final double ridge;

    public GazeboShape(TentParams params, Direction facing) {
        super(params, facing, 0.8, Math.max(params.length, params.width) / 2.0, params.width / 2.0);
        ridge = ax - az;
    }

    /** Nearly straight slopes, just a touch of sag. */
    public static double profile(double t) {
        double u = 1 - t;
        return 0.15 * u * u + 0.85 * u;
    }

    @Override
    protected int legSegments(int edge, double len) {
        return len > 4.5 ? 2 : 1;
    }

    @Override
    protected double ridgeX(double px) {
        return Mth.clamp(px, -(ax - az), ax - az);
    }

    @Override
    public double roofHeight(double lx, double lz) {
        if (Math.abs(lx) > ax || Math.abs(lz) > az) return Double.NaN;
        double d = Math.max(Math.max(0, Math.abs(lx) - ridge), Math.abs(lz));
        return Hw + (H - Hw) * profile(d / az);
    }
}
