package com.nailec.stagetents.tent;

import net.minecraft.core.Direction;

import java.util.List;

/**
 * Inflatable arch spanning local z at x = 0, made of puffed chambers. It has no roof or walls; only its two feet
 * block movement.
 */
public final class ArchShape extends TentShape {
    public final double span, tube;

    public ArchShape(TentParams params, Direction facing) {
        super(params, facing, 0);
        span = params.width / 2.0;
        tube = 0.3 + 0.2 * params.wallHeight;
    }

    /** Centre line of the tube at angle {@code th} (0 = +z foot, PI = -z foot): local z, y. */
    public double[] centre(double th) {
        double rz = span - tube, ry = H - tube;
        return new double[]{rz * Math.cos(th), ry * Math.sin(th)};
    }

    @Override
    public double perimeterLength() {
        return 1;
    }

    @Override
    public void perimeterPoint(double s, double[] out) {
        set(out, 0, 0, 1, 0, 0);
    }

    @Override
    public double eaveHeight(double s) {
        return 0;
    }

    @Override
    public double[] poleStations() {
        return new double[0];
    }

    @Override
    public int stripeAt(double s) {
        return 0;
    }

    @Override
    public double[] panelBounds() {
        return new double[]{0, 1};
    }

    @Override
    public double roofHeight(double lx, double lz) {
        return Double.NaN;
    }

    @Override
    public double perimeterDistance(double lx, double lz) {
        return 99;
    }

    @Override
    public double[] wallPlane(double lx, double lz) {
        return new double[]{1, 0, 0};
    }

    @Override
    public double footprint() {
        return span + 1;
    }

    @Override
    public double extent() {
        return span + (params.guyRopes ? H * 0.6 + 1 : 1);
    }

    @Override
    public boolean isEntrance(double lx, double lz) {
        return false;
    }

    @Override
    public boolean contains(double lx, double lz) {
        return Math.abs(lx) < 1.5 && Math.abs(lz) < span;
    }

    @Override
    protected void poleCells(CellSink sink) {
        double z = span - tube;
        poleColumn(sink, 0, z, 0);
        poleColumn(sink, 0, -z, 0);
    }

    @Override
    public List<double[]> lightPoints() {
        return List.of();
    }
}
