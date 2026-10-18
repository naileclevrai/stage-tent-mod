package com.nailec.stagetents.client;

import com.nailec.stagetents.tent.DjArchShape;
import com.nailec.stagetents.tent.TentParams;
import com.nailec.stagetents.tent.TentPart;
import com.nailec.stagetents.tent.TentShape;
import com.nailec.stagetents.tent.TentType;
import net.minecraft.core.Direction;

import java.util.HashMap;
import java.util.Map;

/** Standalone sanity checks for the two-foot DJ cover: leaf of two arches, saddle membrane, arch collisions. */
public final class DjArchCheck {
    private record Cell(int y, TentPart part) {}

    private static Map<String, Cell> cells(DjArchShape shape) {
        Map<String, Cell> out = new HashMap<>();
        shape.forEachCell(new TentShape.CellSink() {
            public void cell(int x, int y, int z, TentPart part, int level, int shift) {
                out.put(x + ":" + y + ":" + z, new Cell(y, part));
            }
            public void light(int x, int y, int z, int level) {}
        });
        return out;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        int cases = 0;
        for (int span : new int[]{6, 9, 14, 24}) for (int depth : new int[]{2, 5, 8})
            for (int height : new int[]{4, 7, 10}) for (Direction facing :
                    new Direction[]{Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH}) {
                TentParams p = TentType.DJ_ARCH.defaults();
                p.width = span; p.length = depth; p.height = height; p.clamp();
                DjArchShape shape = new DjArchShape(p, facing);
                double crown = shape.roofHeight(0, shape.zc);
                check(crown > 2.5, "Membrane must clear the booth at the crown");
                double[] front = shape.arch(0, 1), back = shape.arch(0, -1);
                check(Math.abs(front[0] + back[0]) < 1e-9 && Math.abs(front[1] - back[1]) < 1e-9,
                        "The two arches must mirror each other");
                check(crown < front[1] - 0.2, "The membrane must sag between the arches");
                double[] footA = shape.arch(-1, 1), footB = shape.arch(1, -1);
                check(Math.abs(footA[2]) < 1e-9 && Math.abs(footB[2] - span) < 1e-9 && Math.abs(footA[0]) < 1e-9,
                        "Both arches spring from the plate foot and land on the far foot");
                check(Double.isNaN(shape.roofHeight(0, -0.5)) && Double.isNaN(shape.roofHeight(0, span + 0.5)),
                        "PVC extends beyond the feet");
                check(Double.isNaN(shape.roofHeight(shape.lean + 0.2, shape.zc)), "PVC extends beyond the arches");
                Map<String, Cell> dry = cells(shape);
                check(dry.values().stream().anyMatch(c -> c.part == TentPart.POLE), "Missing arch collision");
                check(dry.values().stream().noneMatch(c -> c.part == TentPart.WALL || c.part == TentPart.FLOOR),
                        "Open DJ cover acquired a wall or floor");
                check(dry.values().stream().anyMatch(c -> c.part == TentPart.ROOF), "Missing PVC roof collision");
                check(!dry.containsKey("0:0:0"), "A cell landed on the plate foot");
                cases++;
            }
        TentParams p = TentType.DJ_ARCH.defaults();
        DjArchShape shape = new DjArchShape(p, Direction.EAST);
        MeshBuilder mesh = TentMeshes.build(shape, 0);
        check(mesh.hasLayer(MeshBuilder.GLASS), "Transparent PVC layer missing");
        int quads = mesh.quadCount();
        check(quads > 500 && quads < 20000, "Detail or mesh budget unexpectedly changed: " + quads + " quads");
        System.out.println("DJ cover checked: " + cases + " orientations and sizes, " + quads + " quads");
    }
}
