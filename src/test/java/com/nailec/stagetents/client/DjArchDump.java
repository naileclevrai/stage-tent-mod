package com.nailec.stagetents.client;

import com.nailec.stagetents.tent.DjArchShape;
import com.nailec.stagetents.tent.TentParams;
import com.nailec.stagetents.tent.TentType;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Writes the DJ arch quads so they can be drawn without starting the game. */
public final class DjArchDump {
    public static void main(String[] args) throws Exception {
        Path dir = Path.of(args[0]);
        Files.createDirectories(dir);
        TentParams stock = TentType.DJ_ARCH.defaults();
        stock.clamp();
        write(dir.resolve("dj_default.txt"), stock);
        TentParams small = TentType.DJ_ARCH.defaults();
        small.width = 5;
        small.length = 3;
        small.height = 4;
        small.clamp();
        write(dir.resolve("dj_small.txt"), small);
    }

    private static void write(Path file, TentParams params) throws Exception {
        DjArchShape shape = new DjArchShape(params, net.minecraft.core.Direction.EAST);
        MeshBuilder mesh = TentMeshes.build(shape, 0);
        Field layersF = MeshBuilder.class.getDeclaredField("layers");
        layersF.setAccessible(true);
        Object[] layers = (Object[]) layersF.get(mesh);
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(file))) {
            w.printf(Locale.ROOT, "# ax %.3f span %.3f H %.3f leg %.3f%n", shape.ax, shape.span, shape.H, shape.legHeight);
            for (int li = 0; li < layers.length; li++) {
                Object l = layers[li];
                Class<?> c = l.getClass();
                float[] data = (float[]) field(c, "data").get(l);
                int[] color = (int[]) field(c, "color").get(l);
                int count = field(c, "count").getInt(l);
                for (int i = 0; i < count; i++) {
                    int o = i * 8;
                    w.printf(Locale.ROOT, "%d %.4f %.4f %.4f %08X %.3f %.3f %.3f%n",
                            li, data[o], data[o + 1], data[o + 2], color[i], data[o + 5], data[o + 6], data[o + 7]);
                }
            }
        }
        System.out.println("wrote " + file + " quads " + mesh.quadCount());
    }

    private static Field field(Class<?> c, String name) throws Exception {
        Field f = c.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }
}
