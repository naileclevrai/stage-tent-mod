package com.nailec.stagetents.client;

import com.nailec.stagetents.tent.*;
import net.minecraft.core.Direction;

import java.io.DataInputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/** Standalone checks for stage placement, model import, rotation and cleanup of the prototype. */
public final class MobileStageCheck {
    record Cell(int x, int y, int z, TentPart part, int level) {}

    static class Cells implements TentShape.CellSink {
        final Map<String, Cell> values = new HashMap<>();
        int lights, rigs;
        public void cell(int x, int y, int z, TentPart part, int level, int shift) {
            values.putIfAbsent(x + ":" + y + ":" + z, new Cell(x, y, z, part, level));
        }
        public void light(int x, int y, int z, int level) { lights++; }
        public void rig(int x, int y, int z, boolean alongX) { rigs++; }
    }

    static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static Object field(Object obj, String name) throws Exception {
        Field field = obj.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(obj);
    }

    public static void main(String[] args) throws Exception {
        int triangles;
        try (DataInputStream in = new DataInputStream(MobileStageCheck.class.getResourceAsStream(
                "/assets/stagetents/models/structure/opus_4200.mesh"))) {
            require(in.readInt() == 0x53544732, "Stage mesh header");
            triangles = in.readInt();
        }
        TentParams p = TentType.OPUS_4200.defaults();
        p.lights = LightMode.PARTY; p.rigging = true; p.height = 7; p.wallHeight = 6;
        p.clamp();
        require(p.lights == LightMode.OFF && !p.rigging, "Prototype lights must not survive a saved configuration");
        int cellCount = -1;
        for (Direction facing : new Direction[]{Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH}) {
            MobileStageShape shape = new MobileStageShape(p, facing);
            Cells cells = new Cells();
            shape.forEachCell(cells);
            require(cells.lights == 0 && cells.rigs == 0, "No automatically installed equipment");
            require(shape.floorTop() == 2 && !shape.hasStage(), "Two-block-high buildable deck");
            require(shape.isEntrance(-6, 6) && shape.isEntrance(6, 0), "Backstage and audience openings");
            require(shape.extent() >= 12.24 && shape.top() >= 11.28, "Render bounds contain the drawbar and roof");
            require(cellCount == -1 || cellCount == cells.values.size(), "Rotation changes the collision count");
            cellCount = cells.values.size();
            boolean[] steps = new boolean[4];
            boolean centreDeck = false;
            for (Cell cell : cells.values.values()) {
                double x = shape.toLocalX(cell.x, cell.z), z = shape.toLocalZ(cell.x, cell.z);
                require(!(cell.part == TentPart.WALL && cell.y == 0), "Curtains block the trailer underside");
                require(!(cell.part == TentPart.WALL && x == -6 && z == 6), "Blocked rear doorway");
                if (cell.part == TentPart.FLOOR && z == 6 && x <= -8 && x >= -11
                        && cell.y == (x <= -10 ? 0 : 1)) {
                    int i = (int) (x + 11);
                    steps[i] = cell.level == new int[]{5, 12, 2, 9}[i];
                }
                if (cell.part == TentPart.FLOOR && Math.abs(x) <= 6 && Math.abs(z) <= 8)
                    require(cell.level == 15, "Deck cells must accept blocks on top");
                if (cell.part == TentPart.FLOOR && x == 0 && z == 0 && cell.y == 1) centreDeck = true;
            }
            for (boolean step : steps) require(step, "Incomplete four-step rear staircase");
            require(centreDeck, "Missing centre deck collision at the raised height");
            Cells previous = new Cells();
            MobileStageShape.previousCells(p, facing, previous);
            require(previous.values.size() < cells.values.size() && previous.lights == 0,
                    "Previous detailed stage cells cannot be migrated");
            Cells old = new Cells();
            MobileStageShape.legacyCells(facing, old);
            require(old.lights == 5 && old.rigs == 7, "Incomplete cleanup of prototype equipment");
            require(old.values.values().stream().anyMatch(c -> c.y >= 6), "Prototype roof omitted from cleanup");
            MeshBuilder mesh = TentMeshes.build(shape, 0);
            require(mesh.quadCount() == triangles, "Imported mesh differs from the generated model");
            Object[] layers = (Object[]) field(mesh, "layers");
            for (int layer = 0; layer < layers.length; layer++) {
                int count = (int) field(layers[layer], "count");
                if (layer < MeshBuilder.MATERIAL) require(count == 0, "Generic slab or canvas added over the detailed model");
                boolean[] emissive = (boolean[]) field(layers[layer], "emissive");
                for (boolean value : emissive) require(!value, "Unexpected emissive fixture");
                float[] vertices = (float[]) field(layers[layer], "data");
                for (float value : vertices) require(Float.isFinite(value), "Invalid imported vertex");
            }
        }
        System.out.println("Mobile stage OK: " + triangles + " triangles, " + cellCount
                + " collision cells in all four orientations; no fixtures; prototype cleanup covered.");
    }
}
