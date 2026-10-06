package com.nailec.stagetents.tent;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Shape of a tent, shared by the renderer and the collision cells.
 *
 * <p>Local frame: origin at the centre of the master block, ground at y = 0, +x along the tent length towards the
 * front (entrance) end, +z across. The perimeter is walked counter-clockwise (seen from above, +z side first going
 * towards -x) by an arc-length abscissa {@code s}.
 */
public abstract class TentShape {
    public final TentParams params;
    public final Direction facing;
    /** Nominal eave height. */
    public final double Hw;
    /** Peak height. */
    public final double H;

    protected TentShape(TentParams params, Direction facing, double minRise) {
        this.params = params;
        this.facing = facing.getAxis().isHorizontal() ? facing : Direction.EAST;
        this.Hw = params.wallHeight;
        this.H = Math.max(params.height, Hw + minRise);
    }

    public static TentShape create(TentParams p, Direction facing) {
        return switch (p.type) {
            case BIG_TOP -> new BigTopShape(p, facing);
            case PAGODA -> new PagodaShape(p, facing);
            case FRAME -> new FrameShape(p, facing);
            case GAZEBO -> new GazeboShape(p, facing);
            case ARCH -> new ArchShape(p, facing);
            case DJ_ARCH -> new DjArchShape(p, facing);
            case STRETCH -> new StretchShape(p, facing);
            case TENSILE -> new TensileShape(p, facing);
            case OPUS_4200 -> new MobileStageShape(p, facing);
        };
    }

    // ------------------------------------------------------------------ perimeter

    public abstract double perimeterLength();

    /** Fills {@code out} with px, pz, outward nx, nz and the x of the ridge point the roof line starts from. */
    public abstract void perimeterPoint(double s, double[] out);

    public abstract double eaveHeight(double s);

    /** Abscissas of the side poles / legs, ascending, the first one at 0. Corners are always included. */
    public abstract double[] poleStations();

    /** Stripe panel index at abscissa {@code s}. */
    public abstract int stripeAt(double s);

    /** Abscissas of the stripe panel boundaries, ascending from 0 and ending with the perimeter length. */
    public abstract double[] panelBounds();

    // ------------------------------------------------------------------ volume

    /** Smooth roof height above local (x, z), NaN outside the footprint. */
    public abstract double roofHeight(double lx, double lz);

    /** Signed horizontal distance to the wall line: negative inside. */
    public abstract double perimeterDistance(double lx, double lz);

    /** Half extent of the footprint, without ropes. */
    public abstract double footprint();

    /** Horizontal reach from the centre, guy ropes included. */
    public abstract double extent();

    public abstract boolean isEntrance(double lx, double lz);

    public boolean hasWallAt(double lx, double lz) {
        return params.walls != WallMode.OPEN && !isEntrance(lx, lz);
    }

    /** Whether local (x, z) lies on the front side (the side an open front or counter applies to). */
    public boolean isFrontSide(double lx, double lz) {
        return false;
    }

    /** Whether local (x, z) lies on the back side (opposite the front). */
    public boolean isBackSide(double lx, double lz) {
        return false;
    }

    /** True where the wall stops at counter height. */
    public boolean isCounter(double lx, double lz) {
        return params.entrance.hasCounter() && isFrontSide(lx, lz);
    }

    /** Top of the wall at local (x, z), given the eave height there. */
    public double wallTop(double lx, double lz, double eave) {
        return isCounter(lx, lz) ? Math.min(eave, Entrance.COUNTER_HEIGHT) : eave;
    }

    /**
     * Canvas plane of the wall nearest to the cell centred on local (x, z): outward nx, nz and the plane's offset
     * from that centre along the normal.
     */
    public abstract double[] wallPlane(double lx, double lz);

    protected abstract void poleCells(CellSink sink);

    /** Pennant mounts: local x, base y, z. */
    public List<double[]> flagMounts() {
        return List.of();
    }

    /** Where real light sources go when the festoons are on: local x, y, z. */
    public List<double[]> lightPoints() {
        List<double[]> out = new ArrayList<>();
        double P = perimeterLength();
        int n = Math.max(4, (int) Math.round(P / 5));
        double[] per = new double[5];
        for (int i = 0; i < n; i++) {
            perimeterPoint(P * (i + 0.5) / n, per);
            out.add(new double[]{per[0] - per[2] * 1.2, Hw - 0.8, per[1] - per[3] * 1.2});
        }
        return out;
    }

    // ------------------------------------------------------------------ frame helpers

    public double toWorldX(double lx, double lz) {
        return switch (facing) {
            case WEST -> -lx;
            case SOUTH -> -lz;
            case NORTH -> lz;
            default -> lx;
        };
    }

    public double toWorldZ(double lx, double lz) {
        return switch (facing) {
            case WEST -> -lz;
            case SOUTH -> lx;
            case NORTH -> -lx;
            default -> lz;
        };
    }

    public double toLocalX(double wx, double wz) {
        return switch (facing) {
            case WEST -> -wx;
            case SOUTH -> wz;
            case NORTH -> -wz;
            default -> wx;
        };
    }

    public double toLocalZ(double wx, double wz) {
        return switch (facing) {
            case WEST -> -wz;
            case SOUTH -> -wx;
            case NORTH -> wx;
            default -> wz;
        };
    }

    /** Highest point of the structure. */
    public double top() {
        return H;
    }

    public AABB renderBounds(double cx, double y, double cz) {
        double e = extent();
        return new AABB(cx - e, y - 1, cz - e, cx + e, y + top() + 4, cz + e);
    }

    /** True when the local point lies inside the footprint (used to find the tent a player stands in). */
    public boolean contains(double lx, double lz) {
        return perimeterDistance(lx, lz) <= 0.5;
    }

    // ------------------------------------------------------------------ cells

    public interface CellSink {
        /**
         * Roof: {@code level} is the canvas height inside the cell. Wall: {@code level} is the outward direction
         * (0..7, 45 degree steps from +x towards +z in world space) and {@code shift} the canvas plane offset from
         * the cell centre along it, in eighths.
         */
        void cell(int dx, int dy, int dz, TentPart part, int level, int shift);

        void light(int dx, int dy, int dz, int level);

        /** A rigging bar cell for hanging lights, running along world x ({@code alongX}) or z. */
        default void rig(int dx, int dy, int dz, boolean alongX) {}
    }

    // ------------------------------------------------------------------ floor and stage

    /** Top of the floor above the ground, 0 without a floor. */
    public double floorTop() {
        return params.type.hasInterior() && params.floor != FloorMode.NONE ? params.floorHeight * 0.25 : 0;
    }

    /** Local x of the back of the tent (the stage end). */
    public double backX() {
        return -footprint();
    }

    public double stageFrontX() {
        return backX() + params.stageDepth;
    }

    public boolean hasStage() {
        return params.stage && params.type.hasInterior();
    }

    public boolean isStage(double lx, double lz) {
        return hasStage() && lx < stageFrontX() && perimeterDistance(lx, lz) <= -0.3;
    }

    /** Rigging bar cells: local x, y, z and 0 when the bar runs along local x, 1 along local z. */
    public List<double[]> rigPoints() {
        return List.of();
    }

    /**
     * Collision cells and light sources relative to the master block. Roof cells carry the canvas height inside the
     * cell: {@code level} 0..15 means the layer tops out at (level + 1) / 16.
     */
    public void forEachCell(CellSink sink) {
        int ext = (int) Math.ceil(footprint() + 1);
        for (int dx = -ext; dx <= ext; dx++) {
            for (int dz = -ext; dz <= ext; dz++) {
                double lx = toLocalX(dx, dz), lz = toLocalZ(dx, dz);
                double dist = perimeterDistance(lx, lz);
                double h = roofHeight(lx, lz);
                int roofY = Integer.MIN_VALUE;
                // Keep the roof layer inside the eave so it doesn't stick out past the canvas.
                if (!Double.isNaN(h) && dist <= -0.2) {
                    int y = Mth.floor(h);
                    int top = Mth.clamp((int) Math.round((h - y) * 16), 1, 16);
                    roofY = y;
                    sink.cell(dx, y, dz, TentPart.ROOF, top - 1, 0);
                }
                // Walls: a thin panel placed on the canvas line inside every cell the line crosses.
                if (dist >= -0.5 && dist < 0.5 && hasWallAt(lx, lz)) {
                    double[] n = wallPlane(lx, lz);
                    double wx = toWorldX(n[0], n[1]), wz = toWorldZ(n[0], n[1]);
                    int dir = Math.floorMod((int) Math.round(Math.atan2(wz, wx) / (Math.PI / 4)), 8);
                    int shift = Mth.clamp(Mth.floor((n[2] + 0.5) * 8), 0, 7);
                    double eave = Double.isNaN(h) ? Hw : Math.min(h, Hw + 0.5);
                    int topY = isCounter(lx, lz) ? 0 : roofY != Integer.MIN_VALUE ? roofY - 1 : Mth.ceil(eave) - 1;
                    for (int y = 0; y <= topY; y++) {
                        if (dx == 0 && dz == 0 && y == 0) continue;
                        sink.cell(dx, y, dz, TentPart.WALL, dir, shift);
                    }
                }
            }
        }
        floorCells(sink, ext);
        poleCells(sink);
        if (params.rigging) {
            boolean xAlongX = facing.getAxis() == Direction.Axis.X;
            for (double[] rp : rigPoints()) {
                boolean localX = rp[3] == 0;
                sink.rig(cellX(rp[0], rp[2]), Mth.floor(rp[1]), cellZ(rp[0], rp[2]), localX == xAlongX);
            }
        }
        if (params.lights != LightMode.OFF) {
            int level = params.lights == LightMode.WARM ? 14 : 13;
            for (double[] lp : lightPoints()) {
                sink.light(cellX(lp[0], lp[2]), Mth.floor(lp[1]), cellZ(lp[0], lp[2]), level);
            }
        }
    }

    /** Floor layer and stage platform cells. */
    private void floorCells(CellSink sink, int ext) {
        if (!params.type.hasInterior()) return;
        double ft = floorTop();
        boolean stage = hasStage();
        if (ft <= 0 && !stage) return;
        int floorLevel = Mth.clamp((int) Math.round(ft * 16) - 1, 0, 15);
        for (int dx = -ext; dx <= ext; dx++) {
            for (int dz = -ext; dz <= ext; dz++) {
                double lx = toLocalX(dx, dz), lz = toLocalZ(dx, dz);
                if (perimeterDistance(lx, lz) > -0.2) continue;
                if (stage && isStage(lx, lz)) {
                    for (int y = 0; y < params.stageHeight; y++) {
                        if (dx == 0 && dz == 0 && y == 0) continue;
                        sink.cell(dx, y, dz, TentPart.FLOOR, 15, 0);
                    }
                } else if (ft > 0 && !(dx == 0 && dz == 0)) {
                    for (int y = 0; y < Mth.ceil(ft) - (ft % 1 == 0 ? 0 : 1); y++) sink.cell(dx, y, dz, TentPart.FLOOR, 15, 0);
                    if (ft % 1 != 0) sink.cell(dx, Mth.floor(ft), dz, TentPart.FLOOR, floorLevel % 16, 0);
                }
            }
        }
    }

    protected int cellX(double lx, double lz) {
        return Mth.floor(toWorldX(lx, lz) + 0.5);
    }

    protected int cellZ(double lx, double lz) {
        return Mth.floor(toWorldZ(lx, lz) + 0.5);
    }

    /**
     * Pole collision from {@code y0} up to just under the roof above it, at the pole's exact position inside the
     * cell (x in sixteenths as {@code level}, z in eighths as {@code shift}).
     */
    protected void poleColumn(CellSink sink, double lx, double lz, int y0) {
        double wx = toWorldX(lx, lz), wz = toWorldZ(lx, lz);
        int dx = Mth.floor(wx + 0.5), dz = Mth.floor(wz + 0.5);
        int fx = Mth.clamp(Mth.floor((wx - dx + 0.5) * 16), 0, 15);
        int fz = Mth.clamp(Mth.floor((wz - dz + 0.5) * 8), 0, 7);
        double h = roofHeight(lx, lz);
        int top = Double.isNaN(h) ? Mth.ceil(Hw) - 1 : Mth.floor(h) - 1;
        for (int y = Math.max(y0, (dx == 0 && dz == 0) ? 1 : 0); y <= top; y++) {
            sink.cell(dx, y, dz, TentPart.POLE, fx, fz);
        }
    }

    protected static double frac(double v) {
        return v - Math.floor(v);
    }

    protected static void set(double[] o, double a, double b, double c, double d, double e) {
        o[0] = a;
        o[1] = b;
        o[2] = c;
        o[3] = d;
        o[4] = e;
    }
}
