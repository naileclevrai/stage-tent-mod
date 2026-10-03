package com.nailec.stagetents.tent;

import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * Tensile arena: a very large membrane on two lines of lattice masts, its edge lifted into arches between ground
 * anchors so the sides open up. Width is the full span, length the full length; masts per line comes from
 * {@code masts}, the arch height from {@code wallHeight}.
 */
public final class TensileShape extends StretchShape {
    /** Height of the truss grid, kept clear of the membrane where it dips between the masts. */
    private final double trussY;

    public TensileShape(TentParams params, Direction facing) {
        super(params, facing);
        double y = Math.max(Hw + 2, H * 0.55);
        double[] xs = mastXs();
        double z = lineZ();
        for (int k = 0; k <= 40; k++) {
            double t = k / 40.0;
            for (double lz : new double[]{z, -z}) y = Math.min(y, surfaceHeight(xs[0] + (xs[xs.length - 1] - xs[0]) * t, lz) - 1.5);
            for (double x : xs) y = Math.min(y, surfaceHeight(x, -z + 2 * z * t) - 1.5);
        }
        trussY = Math.max(Hw * 0.6, y);
    }

    /** Distance of each mast line from the centre line. */
    public double lineZ() {
        return params.width * 0.21;
    }

    public int perLine() {
        return Math.max(2, Math.min(TentParams.MAX_MASTS, params.masts));
    }

    /** x of the masts of one line, evenly spread, leaving the overhang at both ends. */
    public double[] mastXs() {
        int n = perLine();
        double half = params.length / 2.0 - margin();
        double[] xs = new double[n];
        for (int i = 0; i < n; i++) xs[i] = n == 1 ? 0 : -half + 2 * half * i / (n - 1);
        return xs;
    }

    /** Height of the truss grid hung between the masts. */
    public double trussY() {
        return trussY;
    }

    @Override
    protected void buildMasts(List<double[]> out) {
        double[] xs = mastXs();
        for (double z : new double[]{lineZ(), -lineZ()}) {
            for (int i = 0; i < xs.length; i++) {
                // End masts a little lower, which gives the long membrane its rhythm.
                boolean end = i == 0 || i == xs.length - 1;
                out.add(new double[]{xs[i], z, end ? H * 0.9 : H});
            }
        }
    }

    @Override
    protected double margin() {
        return params.width / 2.0 - lineZ();
    }

    @Override
    protected boolean arches() {
        return true;
    }

    @Override
    protected double cap() {
        return 1.1;
    }

    @Override
    public List<double[]> flagMounts() {
        return List.of();
    }

    /** Rigging pipes under the trusses: along both mast lines and across between facing masts. */
    @Override
    public List<double[]> rigPoints() {
        List<double[]> out = new ArrayList<>();
        double y = trussY() - 0.9;
        double[] xs = mastXs();
        double z = lineZ();
        for (double lz : new double[]{z, -z}) {
            for (double x = xs[0] + 1.5; x <= xs[xs.length - 1] - 1.5 + 1e-6; x += 1) out.add(new double[]{x, y, lz, 0});
        }
        for (double x : xs) {
            for (double lz = -z + 1.5; lz <= z - 1.5 + 1e-6; lz += 1) out.add(new double[]{x, y, lz, 1});
        }
        return out;
    }

    @Override
    public List<double[]> lightPoints() {
        List<double[]> out = new ArrayList<>();
        for (double[] m : masts) out.add(new double[]{m[0] + 1.5, trussY() - 1.5, m[1]});
        return out;
    }
}
