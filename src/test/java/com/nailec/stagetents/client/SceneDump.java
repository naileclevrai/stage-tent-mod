package com.nailec.stagetents.client;

import com.nailec.stagetents.tent.*;
import net.minecraft.core.Direction;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.Locale;
import java.util.function.Consumer;

/** Dev tool: dumps the showcase tents used for the README images (layer x y z argb nx ny nz per vertex). */
public final class SceneDump {
    public static void main(String[] args) throws Exception {
        String out = args[0];
        dump(out, "bigtop", TentType.BIG_TOP, p -> { p.width = 24; p.length = 16; p.masts = 2; p.height = 15; p.wallHeight = 5; });
        dump(out, "bigtop_round", TentType.BIG_TOP, p -> { p.width = 20; p.height = 13; });
        dump(out, "bigtop_concert", TentType.BIG_TOP, p -> {
            p.width = 24; p.length = 12; p.height = 14; p.wallHeight = 5;
            Presets.forType(TentType.BIG_TOP).get(3).apply().accept(p);
            p.lights = LightMode.WARM; p.floor = FloorMode.WOOD; p.stage = true; p.stageDepth = 7; p.stageHeight = 1;
        });
        dump(out, "frame", TentType.FRAME, p -> { p.width = 10; p.length = 20; p.lights = LightMode.WARM; });
        dump(out, "frame_party", TentType.FRAME, p -> {
            p.width = 12; p.length = 20; p.height = 7;
            Presets.forType(TentType.FRAME).get(3).apply().accept(p);
            p.floor = FloorMode.WOOD; p.floorHeight = 1; p.stage = true; p.stageDepth = 4; p.lights = LightMode.PARTY;
        });
        dump(out, "pagoda", TentType.PAGODA, p -> { p.width = 5; p.height = 7; p.walls = WallMode.WINDOWS; });
        dump(out, "pagoda_joined", TentType.PAGODA, p -> { p.width = 5; p.height = 7; p.joined = (1 << 0) | (1 << 2); });
        dump(out, "pagoda_end", TentType.PAGODA, p -> { p.width = 5; p.height = 7; p.joined = 1 << 0; });
        dump(out, "gazebo", TentType.GAZEBO, p -> { p.width = 3; p.length = 3; p.height = 4; p.colorA = Palette.RED; p.colorB = Palette.WHITE; p.stripes = true; });
        dump(out, "regie", TentType.GAZEBO, p -> { p.width = 4; p.length = 4; p.height = 5; p.wallHeight = 3; Presets.forType(TentType.GAZEBO).get(0).apply().accept(p); });
        dump(out, "stretch", TentType.STRETCH, p -> {
            p.height = 8; p.width = 5;
            p.stretchPoles.add(new int[]{9, 3, 6});
            p.stretchPoles.add(new int[]{-8, -4, 6});
            p.stretchPoles.add(new int[]{1, -9, 5});
        });
        dump(out, "arch", TentType.ARCH, p -> { p.width = 10; p.height = 7; });
        dump(out, "dj_arch", TentType.DJ_ARCH, p -> { });
        dump(out, "arena", TentType.TENSILE, p -> { });
        dump(out, "bar_mid", TentType.PAGODA, p -> { p.width = 5; p.height = 7; Presets.forType(TentType.PAGODA).get(1).apply().accept(p); p.joined = (1 << 0) | (1 << 2); });
        dump(out, "bar_end", TentType.PAGODA, p -> { p.width = 5; p.height = 7; Presets.forType(TentType.PAGODA).get(1).apply().accept(p); p.joined = 1 << 0; });
        dump(out, "bar_end2", TentType.PAGODA, p -> { p.width = 5; p.height = 7; Presets.forType(TentType.PAGODA).get(1).apply().accept(p); p.joined = 1 << 2; });
    }

    static void dump(String out, String name, TentType type, Consumer<TentParams> setup) throws Exception {
        TentParams p = type.defaults();
        setup.accept(p);
        p.clamp();
        MeshBuilder m = TentMeshes.build(TentShape.create(p, Direction.EAST), 0);
        Field layersF = MeshBuilder.class.getDeclaredField("layers");
        layersF.setAccessible(true);
        Object[] layers = (Object[]) layersF.get(m);
        try (PrintWriter w = new PrintWriter(out + "/" + name + ".txt")) {
            for (int li = 0; li < layers.length; li++) {
                Object l = layers[li];
                Class<?> c = l.getClass();
                float[] data = (float[]) field(c, "data").get(l);
                int[] color = (int[]) field(c, "color").get(l);
                boolean[] emissive = (boolean[]) field(c, "emissive").get(l);
                int count = field(c, "count").getInt(l);
                for (int i = 0; i < count; i++) {
                    int o = i * 8;
                    int kind = li == MeshBuilder.GLASS ? 1 : emissive[i] ? 2 : li == MeshBuilder.FLOOR ? 3 : 0;
                    w.printf(Locale.ROOT, "%d %.3f %.3f %.3f %d %.3f %.3f %.3f%n", kind, data[o], data[o + 1], data[o + 2], color[i], data[o + 5], data[o + 6], data[o + 7]);
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
