package com.nailec.stagetents.client;

import com.nailec.stagetents.tent.*;
import net.minecraft.core.Direction;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.Locale;

/** Dev tool: dumps tent meshes as quads (layer x y z argb nx ny nz per vertex) for offline previews. */
public final class MeshDump {
    public static void main(String[] args) throws Exception {
        String out = args[0];
        for (TentType type : TentType.values()) {
            for (String variant : new String[]{"default", "styled"}) {
                TentParams p = type.defaults();
                if (variant.equals("styled")) {
                    Presets.forType(type).get(Math.min(2, Presets.forType(type).size() - 1)).apply().accept(p);
                    p.lights = LightMode.PARTY;
                    switch (type) {
                        case PAGODA -> { p.length = 4; p.width = 6; p.joined = 1 << 3; p.walls = WallMode.WINDOWS; }
                        case BIG_TOP -> { p.length = 12; p.masts = 2; p.floor = FloorMode.CARPET; p.stage = true; p.stageDepth = 6; p.walls = WallMode.OPEN; p.floor = FloorMode.WOOD; }
                        case FRAME -> { p.floor = FloorMode.WOOD; p.floorHeight = 1; p.stage = true; p.stageHeight = 1; p.walls = WallMode.WINDOWS; p.rigging = true; }
                        case GAZEBO -> { p.length = 4; p.width = 4; p.wallHeight = 3; p.height = 5; Presets.forType(type).get(0).apply().accept(p); }
                        case STRETCH -> p.stretchPoles.add(new int[]{9, 4, 6});
                        default -> {}
                    }
                    if (type == TentType.STRETCH) p.stretchPoles.add(new int[]{-7, -5, 5});
                }
                p.clamp();
                TentShape shape = TentShape.create(p, Direction.EAST);
                int[] cells = new int[5];
                shape.forEachCell(new TentShape.CellSink() {
                    @Override
                    public void cell(int dx, int dy, int dz, TentPart part, int level, int shift) {
                        cells[part.ordinal()]++;
                    }

                    @Override
                    public void light(int dx, int dy, int dz, int level) {
                        cells[4]++;
                    }
                });
                System.out.printf(Locale.ROOT, "%s/%s cells roof=%d wall=%d pole=%d floor=%d lights=%d%n",
                        type.id, variant, cells[0], cells[1], cells[2], cells[3], cells[4]);
                MeshBuilder m = TentMeshes.build(shape, 0);
                try (PrintWriter w = new PrintWriter(out + "/" + type.id + "_" + variant + ".txt")) {
                    Field layersF = MeshBuilder.class.getDeclaredField("layers");
                    layersF.setAccessible(true);
                    Object[] layers = (Object[]) layersF.get(m);
                    for (int li = 0; li < layers.length; li++) {
                        Object l = layers[li];
                        Class<?> c = l.getClass();
                        float[] data = (float[]) field(c, "data").get(l);
                        int[] color = (int[]) field(c, "color").get(l);
                        int count = field(c, "count").getInt(l);
                        for (int i = 0; i < count; i++) {
                            int o = i * 8;
                            w.printf(Locale.ROOT, "%d %.3f %.3f %.3f %d %.3f %.3f %.3f%n", li == 1 ? 1 : 0, data[o], data[o + 1], data[o + 2], color[i], data[o + 5], data[o + 6], data[o + 7]);
                        }
                    }
                }
            }
        }
    }

    private static Field field(Class<?> c, String n) throws Exception {
        Field f = c.getDeclaredField(n);
        f.setAccessible(true);
        return f;
    }
}
