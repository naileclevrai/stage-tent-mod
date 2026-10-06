package com.nailec.stagetents.client;

import com.nailec.stagetents.tent.DjArchShape;
import com.nailec.stagetents.tent.TentParams;
import com.nailec.stagetents.tent.TentPart;
import com.nailec.stagetents.tent.TentShape;
import com.nailec.stagetents.tent.TentType;
import net.minecraft.core.Direction;

import java.util.HashMap;
import java.util.Map;

/** Standalone sanity checks for the compact DJ roof and its two post collisions. */
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
        for (int width : new int[]{4, 5, 8, 12}) for (int depth : new int[]{2, 3, 6})
            for (int height : new int[]{4, 5, 8}) for (Direction facing :
                    new Direction[]{Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH}) {
                TentParams p = TentType.DJ_ARCH.defaults();
                p.width = width; p.length = depth; p.height = height; p.clamp();
                DjArchShape shape = new DjArchShape(p, facing);
                check(shape.roofHeight(0, 0) > 2.5, "Roof must clear the booth");
                check(shape.y(0) > 1.4 && shape.y(Math.PI / 2) > shape.y(0) + 1.2
                                && shape.roofHeight(0, 0) > shape.y(0),
                        "Straight poles, then a curved sheet above them");
                check(Double.isNaN(shape.roofHeight(0, shape.span + 0.2)), "PVC extends beyond its frame");
                Map<String, Cell> dry = cells(shape);
                long posts = dry.values().stream().filter(c -> c.part == TentPart.POLE).count();
                check(posts == 2L * (int) Math.ceil(shape.legHeight), "Expected two narrow upright collisions");
                check(dry.values().stream().noneMatch(c -> c.part == TentPart.WALL || c.part == TentPart.FLOOR),
                        "Open DJ roof acquired a wall or floor");
                check(dry.values().stream().anyMatch(c -> c.part == TentPart.ROOF), "Missing PVC roof collision");
                p.guyRopes = true;
                p.clamp();
                check(!p.guyRopes, "DJ roof should never spawn side cables");
                check(cells(new DjArchShape(p, facing)).keySet().equals(dry.keySet()),
                        "Guy ropes should never change collision cells");
                cases++;
            }
        TentParams p = TentType.DJ_ARCH.defaults();
        DjArchShape shape = new DjArchShape(p, Direction.EAST);
        MeshBuilder mesh = TentMeshes.build(shape, 0);
        check(mesh.hasLayer(MeshBuilder.GLASS), "Transparent PVC layer missing");
        check(Math.abs(shape.sailY(shape.ax, Math.PI / 2) - shape.sailY(-shape.ax, Math.PI / 2)) < 0.05
                        && shape.sailY(0, Math.PI / 2) > shape.sailY(shape.ax, Math.PI / 2) + 0.4
                        && shape.sailCovers(Math.PI / 2)
                        && !shape.sailCovers(0.2)
                        && Double.isNaN(shape.roofHeight(0, shape.span * 0.95))
                        && shape.roofHeight(0, 0) > shape.legHeight + 1,
                "The sheet runs off both sides of the tube and stays open under the poles");
        int quads = mesh.quadCount();
        check(quads > 250 && quads < 8000, "Detail or mesh budget unexpectedly changed: " + quads + " quads");
        System.out.println("DJ arch checked: " + cases + " orientations and sizes, " + quads + " quads");
    }
}
