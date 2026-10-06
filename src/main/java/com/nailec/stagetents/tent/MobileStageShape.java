package com.nailec.stagetents.tent;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import java.util.List;

/** Deployed trailer stage. Local +x is the audience side; the trailer runs across the facade along z. */
public final class MobileStageShape extends RectShape {
    public static final double SCALE = 2.0;
    public static final double DECK_Y = SCALE, HALF_DEPTH = 3.30 * SCALE, HALF_WIDTH = 4.10 * SCALE;
    private final double scale;

    public MobileStageShape(TentParams params, Direction facing) {
        this(params, facing, SCALE);
    }

    private MobileStageShape(TentParams params, Direction facing, double scale) {
        super(params, facing, 1, 3.30 * scale, 4.10 * scale);
        this.scale = scale;
    }

    @Override protected double ridgeX(double px) { return px; }
    @Override public double floorTop() { return scale; }
    @Override public double top() { return 5.64 * scale; }
    @Override public double extent() { return 6.3 * scale; }

    @Override public double roofHeight(double lx, double lz) {
        double x = lx / scale, z = lz / scale;
        if (Math.abs(x) > 3.50 || Math.abs(z) > 4.23) return Double.NaN;
        double sag = Math.sin(z * Math.PI / .82);
        return scale * (5.64 - Math.max(0, Math.abs(x) - 1.25) * .055 - .014 * sag * sag);
    }

    @Override public boolean isEntrance(double lx, double lz) {
        return lx > 2.9 * scale || (lx < -2.9 * scale && lz > 2.3 * scale && lz < 3.62 * scale);
    }

    @Override public boolean hasWallAt(double lx, double lz) { return !isEntrance(lx, lz); }

    @Override public void forEachCell(CellSink sink) {
        // Full floor cells keep the deck buildable at its two-block height.
        int radius = (int) Math.ceil(5.5 * scale);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double x = toLocalX(dx, dz), z = toLocalZ(dx, dz);
                if (Math.abs(x) <= 3 * scale && Math.abs(z) <= 4 * scale) {
                    for (int y = 0; y < (int) scale; y++) {
                        if (dx != 0 || dz != 0 || y != 0) sink.cell(dx, y, dz, TentPart.FLOOR, 15, 0);
                    }
                    double h = roofHeight(x, z);
                    int roofY = Mth.floor(h);
                    sink.cell(dx, roofY, dz, TentPart.ROOF, Mth.clamp((int) Math.round((h - roofY) * 16) - 1, 0, 15), 0);
                }
                // Thin curtains stop at the deck, leaving the trailer underside open.
                boolean side = Math.abs(z) == 4 * scale && x >= -3 * scale && x <= 2 * scale;
                boolean back = x == -3 * scale && Math.abs(z) <= 3 * scale
                        && !(z > 2.3 * scale && z < 3.62 * scale);
                if (side || back) {
                    double nx = side ? 0 : -1, nz = side ? Math.signum(z) : 0;
                    double offset = side ? .025 * scale : .17 * scale;
                    double wx = toWorldX(nx, nz), wz = toWorldZ(nx, nz);
                    int dir = Math.floorMod((int) Math.round(Math.atan2(wz, wx) / (Math.PI / 4)), 8);
                    int shift = Mth.clamp(Mth.floor((offset + .5) * 8), 0, 7);
                    for (int y = (int) scale; y < (int) Math.ceil(5.19 * scale); y++)
                        sink.cell(dx, y, dz, TentPart.WALL, dir, shift);
                }
            }
        }
        if (scale == 1) {
            sink.cell(cellX(-5, 3), 0, cellZ(-5, 3), TentPart.FLOOR, 5, 0);
            sink.cell(cellX(-4, 3), 0, cellZ(-4, 3), TentPart.FLOOR, 10, 0);
        } else {
            // Four .4-block rises: the player can ascend without jumping.
            int[] stairX = {-11, -10, -9, -8, -7};
            int[] topSixteenths = {6, 13, 19, 26, 26};
            for (int i = 0; i < stairX.length; i++) {
                for (int z = 5; z <= 7; z++) {
                    int x = stairX[i];
                    if (topSixteenths[i] > 16) sink.cell(cellX(x, z), 0, cellZ(x, z), TentPart.FLOOR, 15, 0);
                    int y = topSixteenths[i] > 16 ? 1 : 0;
                    sink.cell(cellX(x, z), y, cellZ(x, z), TentPart.FLOOR, (topSixteenths[i] - 1) % 16, 0);
                }
            }
        }
        poleCells(sink);
    }

    @Override public List<double[]> lightPoints() { return List.of(); }
    @Override public List<double[]> rigPoints() { return List.of(); }

    @Override protected void poleCells(CellSink sink) {
        // Facade trusses and the four telescopic lifting posts; no giant corner collision boxes.
        for (double z : new double[]{-3.94, 3.94}) poleColumn(sink, 3.10 * scale, z * scale, (int) scale);
        for (double x : new double[]{-1.16, 1.16})
            for (double z : new double[]{-3.70, 3.70}) poleColumn(sink, x * scale, z * scale, (int) scale);
    }

    /** Cells of the first detailed stage, used only to migrate existing worlds. */
    public static void previousCells(TentParams params, Direction facing, CellSink sink) {
        new MobileStageShape(params, facing, 1).forEachCell(sink);
    }

    /** Enumerates the first model's cells solely for removing them from an existing save. */
    public static void legacyCells(Direction facing, CellSink sink) {
        TentParams p = TentType.OPUS_4200.defaults();
        p.height = 7; p.wallHeight = 6; p.lights = LightMode.WARM; p.rigging = true;
        RectShape old = new RectShape(p, facing, 1, 3.5, 4.0) {
            @Override protected double ridgeX(double x) { return x; }
            @Override public double roofHeight(double x, double z) {
                return Math.abs(x) > ax || Math.abs(z) > az ? Double.NaN : 6 + (x + ax) / (2 * ax);
            }
            @Override public boolean isEntrance(double x, double z) {
                return isFrontSide(x, z) || (isBackSide(x, z) && z > az - 1.7 && z < az - .2);
            }
            @Override public boolean hasWallAt(double x, double z) { return !isEntrance(x, z); }
            @Override protected void poleCells(CellSink target) {
                for (double x : new double[]{-ax + .18, ax - .18})
                    for (double z : new double[]{-az + .18, az - .18}) poleColumn(target, x, z, 0);
            }
            @Override public List<double[]> lightPoints() {
                return List.of(new double[]{2.2, 5.5, -2.5}, new double[]{2.2, 5.5, -1.2},
                        new double[]{2.2, 5.5, 0}, new double[]{2.2, 5.5, 1.2}, new double[]{2.2, 5.5, 2.5});
            }
            @Override public List<double[]> rigPoints() {
                return java.util.stream.IntStream.rangeClosed(-3, 3)
                        .mapToObj(z -> new double[]{3, 5.35, z, 1}).toList();
            }
            @Override public void forEachCell(CellSink target) {
                super.forEachCell(target);
                target.cell(cellX(-4.95, 3), 0, cellZ(-4.95, 3), TentPart.FLOOR, 5, 0);
                target.cell(cellX(-4.05, 3), 0, cellZ(-4.05, 3), TentPart.FLOOR, 10, 0);
            }
        };
        old.forEachCell(sink);
    }
}
