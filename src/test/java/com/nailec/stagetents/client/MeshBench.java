package com.nailec.stagetents.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nailec.stagetents.tent.*;
import net.minecraft.core.Direction;

import java.lang.reflect.Field;
import java.util.Locale;

/** Dev tool: mesh build time, quad counts and per-frame emit time for typical and worst-case tents. */
public final class MeshBench {
    /** Consumes vertices like a buffer would, without a GPU. */
    static final class Sink implements VertexConsumer {
        long n;
        double acc;

        public VertexConsumer vertex(double x, double y, double z) { acc += x + y + z; return this; }
        public VertexConsumer color(int r, int g, int b, int a) { acc += r; return this; }
        public VertexConsumer uv(float u, float v) { acc += u; return this; }
        public VertexConsumer overlayCoords(int u, int v) { return this; }
        public VertexConsumer uv2(int u, int v) { acc += u; return this; }
        public VertexConsumer normal(float x, float y, float z) { acc += x; return this; }
        public void endVertex() { n++; }
        public void defaultColor(int r, int g, int b, int a) {}
        public void unsetDefaultColor() {}
    }

    public static void main(String[] args) throws Exception {
        bench("big top 20 (default)", TentType.BIG_TOP.defaults());
        TentParams big = TentType.BIG_TOP.defaults();
        big.width = 40; big.length = 40; big.masts = 3; big.height = 20; big.wallHeight = 6;
        big.lights = LightMode.WARM; big.walls = WallMode.WINDOWS; big.floor = FloorMode.WOOD; big.stage = true;
        bench("big top 40x80 lights+windows", big);
        TentParams huge = TentType.BIG_TOP.defaults();
        huge.width = 64; huge.length = 64; huge.masts = 4; huge.height = 30; huge.wallHeight = 8;
        huge.lights = LightMode.PARTY; huge.walls = WallMode.WINDOWS;
        bench("big top 64x128 worst case", huge);
        TentParams frame = TentType.FRAME.defaults();
        frame.width = 20; frame.length = 40; frame.lights = LightMode.WARM;
        bench("frame 20x40 lights", frame);
        TentParams st = TentType.STRETCH.defaults();
        st.stretchPoles.add(new int[]{10, 6, 7}); st.stretchPoles.add(new int[]{-9, -5, 6}); st.stretchPoles.add(new int[]{4, -10, 5});
        st.lights = LightMode.WARM;
        bench("stretch 4 masts lights", st);
    }

    static void bench(String name, TentParams p) throws Exception {
        p.clamp();
        long t0 = System.nanoTime();
        TentShape shape = null;
        for (int i = 0; i < 3; i++) shape = TentShape.create(p, Direction.EAST);
        long t1 = System.nanoTime();
        MeshBuilder m = null;
        for (int i = 0; i < 3; i++) m = TentMeshes.build(shape, 0);
        long t2 = System.nanoTime();

        Field layersF = MeshBuilder.class.getDeclaredField("layers");
        layersF.setAccessible(true);
        Object[] layers = (Object[]) layersF.get(m);
        StringBuilder counts = new StringBuilder();
        long total = 0;
        for (Object l : layers) {
            Field c = l.getClass().getDeclaredField("count");
            c.setAccessible(true);
            int verts = c.getInt(l);
            total += verts;
            counts.append(verts / 4).append(' ');
        }
        PoseStack ps = new PoseStack();
        Sink sink = new Sink();
        double[] ms = new double[3];
        String[] modes = {"near", "inside", "far"};
        for (int mode = 0; mode < 3; mode++) {
            for (int w = 0; w < 50; w++) emitMode(m, ps, sink, w, mode);
            long a0 = System.nanoTime();
            int frames = 200;
            for (int f = 0; f < frames; f++) emitMode(m, ps, sink, f, mode);
            ms[mode] = (System.nanoTime() - a0) / 1e6 / frames;
        }
        System.out.printf(Locale.ROOT, "%-30s shape %6.1f ms  mesh %6.1f ms  quads %6d [%s]  emit near %.3f / inside %.3f / far %.3f ms%n",
                name, (t1 - t0) / 3e6, (t2 - t1) / 3e6, total / 4, counts.toString().trim(), ms[0], ms[1], ms[2]);
    }

    /** Mirrors the renderer: near = everything with wind, inside = no outer faces, far = no inner/detail/wind. */
    static void emitMode(MeshBuilder m, PoseStack ps, Sink sink, int frame, int mode) {
        float wind = mode == 2 ? 0 : 0.05F;
        for (int layer = 0; layer < MeshBuilder.LAYERS; layer++) {
            if (mode == 1 && layer == MeshBuilder.OUTER) continue;
            if (mode == 2 && (layer == MeshBuilder.INNER || layer == MeshBuilder.DETAIL)) continue;
            m.emit(layer, ps.last(), sink, 0, frame, wind);
        }
    }

}
