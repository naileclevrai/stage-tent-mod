package com.nailec.stagetents.client;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Dev tool: dumps a control tower mesh as quads (layer x y z argb nx ny nz per vertex) for offline previews. */
public final class ControlTowerDump {
    public static void main(String[] args) throws Exception {
        Path dir = Path.of(args[0]);
        Files.createDirectories(dir);
        Class<?> mesh = Class.forName("com.nailec.stagetents.client.ControlTowerRenderer$Mesh");
        Method build = mesh.getDeclaredMethod("build", long.class, int.class, int.class, int.class, boolean.class,
                boolean.class, boolean.class, int.class);
        build.setAccessible(true);
        boolean roof = args.length < 2 || !args[1].equals("noroof");
        Object m = build.invoke(null, 0L, 3, 2, 3, roof, true, true, 0xFF3A4A6A);
        float[] xyz = (float[]) field(mesh, "xyz").get(m);
        int[] color = (int[]) field(mesh, "color").get(m);
        byte[] mat = (byte[]) field(mesh, "mat").get(m);
        int quads = field(mesh, "quads").getInt(m);
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(dir.resolve("tower.txt")))) {
            for (int q = 0; q < quads; q++) {
                int p = q * 12;
                double ax = xyz[p + 6] - xyz[p], ay = xyz[p + 7] - xyz[p + 1], az = xyz[p + 8] - xyz[p + 2];
                double bx = xyz[p + 9] - xyz[p + 3], by = xyz[p + 10] - xyz[p + 4], bz = xyz[p + 11] - xyz[p + 5];
                double nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
                double l = Math.max(1e-9, Math.sqrt(nx * nx + ny * ny + nz * nz));
                for (int v = 0; v < 4; v++) {
                    w.printf(Locale.ROOT, "%d %.4f %.4f %.4f %08X %.3f %.3f %.3f%n", mat[q] == 4 ? 2 : 0,
                            xyz[p + v * 3], xyz[p + v * 3 + 1], xyz[p + v * 3 + 2], color[q], nx / l, ny / l, nz / l);
                }
            }
        }
        System.out.println("tower quads " + quads);
        for (boolean skirt : new boolean[]{false, true}) {
            for (boolean gear : new boolean[]{false, true}) {
                Object v = build.invoke(null, 1L, 3, 2, 3, true, skirt, gear, 0xFF3A4A6A);
                System.out.println("sheets " + skirt + " gear " + gear + ": " + field(mesh, "quads").getInt(v) + " quads");
            }
        }
    }

    private static Field field(Class<?> c, String n) throws Exception {
        Field f = c.getDeclaredField(n);
        f.setAccessible(true);
        return f;
    }
}
