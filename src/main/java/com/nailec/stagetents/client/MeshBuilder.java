package com.nailec.stagetents.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nailec.stagetents.tent.TentParams;
import com.nailec.stagetents.tent.TentShape;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Arrays;

/**
 * Baked quads of a tent in block-entity space (origin at the master block corner), built in the tent's local frame.
 * Built once per settings change; light is refreshed a slice at a time.
 *
 * <p>Quads are sorted into layers so the renderer can skip what can't be seen: the outside of the canvas from inside
 * the tent, the inside of the canvas and the small details from far away. Canvas faces are emitted twice (outside
 * and inside) with opposite winding so they can be drawn with back-face culling and lit from both sides.
 *
 * <p>Layers are uploaded once to GPU vertex buffers and drawn from there with {@link TentShaders}, which also does the
 * wind; a layer is uploaded again only when its light actually changed. {@link #emit} is the CPU fallback.
 */
final class MeshBuilder implements AutoCloseable {
    /** Structure: poles, beams, plates, rolled walls, drapes. Always drawn. */
    static final int SOLID = 0;
    /** See-through window panels. */
    static final int GLASS = 1;
    /** Floor, drawn with the floor texture of the chosen floor mode. */
    static final int FLOOR = 2;
    /** Stage deck and skirt (black stage carpet). */
    static final int DECK = 3;
    /** Outside faces of the canvas envelope; never visible from inside the tent. */
    static final int OUTER = 4;
    /** Inside faces of the canvas envelope. */
    static final int INNER = 5;
    /** Thin parts (ropes, wires, bulbs, stakes) that vanish at a distance anyway. */
    static final int DETAIL = 6;
    static final int LAYERS = 7;

    /** Below this size (blocks) a tube or box counts as a detail. */
    private static final double DETAIL_SIZE = 0.05;
    private static final int STRIDE = 8; // x y z u v nx ny nz
    /** Per vertex: flex weight, the world direction it moves along (x y z) and its wave phase. */
    private static final int FLEX_STRIDE = 5;
    private static final float INV255 = 1F / 255F;

    final int version;
    final Direction facing;
    final TentShape g;
    final TentParams p;
    final long builtAt = System.nanoTime();
    long lightStamp;
    boolean lit;
    private final Layer[] layers = new Layer[LAYERS];
    // Incremental relight: a pass walks the layers a slice per frame.
    private int relightLayer, relightIndex;
    private boolean relightRunning;
    private Long2IntOpenHashMap relightCache;
    private final VertexBuffer[] gpu = new VertexBuffer[LAYERS];
    private final boolean[] gpuStale = new boolean[LAYERS];
    /** Shared staging buffer for uploads (render thread only); its native memory is reused, never reallocated per tent. */
    private static BufferBuilder staging;

    MeshBuilder(TentShape g, int version) {
        this.g = g;
        this.p = g.params;
        this.version = version;
        this.facing = g.facing;
        for (int i = 0; i < LAYERS; i++) layers[i] = new Layer();
    }

    private static final class Layer {
        float[] data = new float[STRIDE * 256];
        float[] flex;
        int[] color = new int[256];
        int[] light = new int[256];
        boolean[] emissive = new boolean[256];
        int count;

        void ensure() {
            if (count < color.length) return;
            int cap = count * 2;
            data = Arrays.copyOf(data, cap * STRIDE);
            color = Arrays.copyOf(color, cap);
            light = Arrays.copyOf(light, cap);
            emissive = Arrays.copyOf(emissive, cap);
            if (flex != null) flex = Arrays.copyOf(flex, cap * FLEX_STRIDE);
        }

        void trim() {
            data = Arrays.copyOf(data, count * STRIDE);
            color = Arrays.copyOf(color, count);
            light = Arrays.copyOf(light, count);
            emissive = Arrays.copyOf(emissive, count);
            if (flex != null) flex = Arrays.copyOf(flex, count * FLEX_STRIDE);
        }
    }

    /** Frees the spare capacity left by building. */
    MeshBuilder done() {
        for (Layer l : layers) l.trim();
        return this;
    }

    int quadCount() {
        int n = 0;
        for (Layer l : layers) n += l.count / 4;
        return n;
    }

    // ------------------------------------------------------------------ colours

    static int opaque(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }

    static int shade(int c, float mul) {
        int r = Math.min(255, (int) (((c >> 16) & 0xFF) * mul));
        int gg = Math.min(255, (int) (((c >> 8) & 0xFF) * mul));
        int b = Math.min(255, (int) ((c & 0xFF) * mul));
        return (c & 0xFF000000) | (r << 16) | (gg << 8) | b;
    }

    /** Inside colour for a canvas panel: the lining if one is set, otherwise the outside colour seen through. */
    int inner(int outer) {
        return p.lining >= 0 ? opaque(p.lining) : shade(outer, 0.9F);
    }

    /** Canvas colour of stripe panel {@code panel}; {@code invert} swaps the two colours. */
    int stripe(int panel, boolean invert) {
        int c = !p.stripes ? p.colorA : ((Math.floorMod(panel, 2) == 0) != invert ? p.colorA : p.colorB);
        return opaque(c);
    }

    // ------------------------------------------------------------------ primitives (local frame)

    /** Vertex: x y z u v nx ny nz. */
    static double[] v(double x, double y, double z, double u, double vv, double nx, double ny, double nz) {
        return new double[]{x, y, z, u, vv, nx, ny, nz};
    }

    /**
     * Vertex that moves in the wind: {@code flex} 0 (held by a pole or beam) to 1 (free), moving along its normal.
     * The direction is captured here so the inside copy of a two-sided face moves with the outside one.
     */
    static double[] vf(double x, double y, double z, double u, double vv, double nx, double ny, double nz, double flex) {
        return new double[]{x, y, z, u, vv, nx, ny, nz, flex, nx, ny, nz};
    }

    /**
     * A face of the canvas envelope, seen from both sides. On the SOLID layer the two sides go to OUTER and INNER
     * so the renderer can drop the side that can't be seen.
     */
    void twoSided(double[][] q, int outer, int inner, int layer) {
        int outLayer = layer == SOLID ? OUTER : layer, inLayer = layer == SOLID ? INNER : layer;
        quad(q, outer, outLayer, false);
        double[][] r = new double[4][];
        double fx = 0, fy = 0, fz = 0;
        for (int k = 0; k < 4; k++) {
            double[] vv = q[3 - k].clone();
            fx -= vv[5];
            fy -= vv[6];
            fz -= vv[7];
            // Shade the inside as daylight coming through the canvas: flip the horizontal part of the normal but
            // keep it pointing up, otherwise the vanilla entity lighting makes every ceiling dull grey.
            vv[5] = -vv[5];
            vv[6] = Math.abs(vv[6]);
            vv[7] = -vv[7];
            r[k] = vv;
        }
        quadFacing(r, inner, inLayer, false, fx, fy, fz);
    }

    void twoSided(double[][] q, int outer) {
        twoSided(q, outer, inner(outer), SOLID);
    }

    /** Both faces on the always-drawn SOLID layer (free-standing drapes, not part of the envelope). */
    void doubleSided(double[][] q, int col) {
        quad(q, col, SOLID, false);
        double[][] r = new double[4][];
        for (int k = 0; k < 4; k++) {
            double[] vv = q[3 - k].clone();
            vv[5] = -vv[5];
            vv[6] = -vv[6];
            vv[7] = -vv[7];
            r[k] = vv;
        }
        quad(r, col, SOLID, false);
    }

    /** Adds a quad facing its normals: the winding is flipped when it disagrees with them. */
    void quad(double[][] q, int col, int layer, boolean emissive) {
        double nx = 0, ny = 0, nz = 0;
        for (double[] vv : q) { nx += vv[5]; ny += vv[6]; nz += vv[7]; }
        quadFacing(q, col, layer, emissive, nx, ny, nz);
    }

    /** Adds a quad whose front side faces (nx, ny, nz), independently of the shading normals. */
    void quadFacing(double[][] q, int col, int layer, boolean emissive, double nx, double ny, double nz) {
        double ax = q[1][0] - q[0][0], ay = q[1][1] - q[0][1], az = q[1][2] - q[0][2];
        double bx = q[2][0] - q[0][0], by = q[2][1] - q[0][1], bz = q[2][2] - q[0][2];
        double cx = ay * bz - az * by, cy = az * bx - ax * bz, cz = ax * by - ay * bx;
        if (cx * cx + cy * cy + cz * cz < 1e-12) { // first three corners collinear (cone tip): use the other diagonal
            bx = q[3][0] - q[0][0]; by = q[3][1] - q[0][1]; bz = q[3][2] - q[0][2];
            ax = q[2][0] - q[0][0]; ay = q[2][1] - q[0][1]; az = q[2][2] - q[0][2];
            cx = ay * bz - az * by; cy = az * bx - ax * bz; cz = ax * by - ay * bx;
        }
        boolean flip = cx * nx + cy * ny + cz * nz < 0;
        Layer l = layers[layer];
        for (int k = 0; k < 4; k++) put(l, q[flip ? 3 - k : k], col, emissive);
    }

    /** Axis-aligned (in the local frame) box centred on (cx, cz), half width {@code h}. */
    void box(double cx, double y0, double cz, double h, double y1, int col, boolean emissive) {
        box(cx - h, y0, cz - h, cx + h, y1, cz + h, col, emissive);
    }

    void box(double x0, double y0, double z0, double x1, double y1, double z1, int col, boolean emissive) {
        double w = x1 - x0, d = z1 - z0, ht = y1 - y0;
        // Bulbs and stakes are details; thin but tall parts (legs) are structure and stay visible from afar.
        int layer = emissive || (Math.min(w, d) < 2 * DETAIL_SIZE + 0.01 && ht < 0.6) ? DETAIL : SOLID;
        quad(new double[][]{v(x0, y1, z0, 0, 0, 0, 1, 0), v(x0, y1, z1, 0, d, 0, 1, 0), v(x1, y1, z1, w, d, 0, 1, 0), v(x1, y1, z0, w, 0, 0, 1, 0)}, col, layer, emissive);
        quad(new double[][]{v(x0, y0, z0, 0, 0, 0, -1, 0), v(x1, y0, z0, w, 0, 0, -1, 0), v(x1, y0, z1, w, d, 0, -1, 0), v(x0, y0, z1, 0, d, 0, -1, 0)}, col, layer, emissive);
        quad(new double[][]{v(x0, y0, z0, 0, ht, 0, 0, -1), v(x0, y1, z0, 0, 0, 0, 0, -1), v(x1, y1, z0, w, 0, 0, 0, -1), v(x1, y0, z0, w, ht, 0, 0, -1)}, col, layer, emissive);
        quad(new double[][]{v(x0, y0, z1, 0, ht, 0, 0, 1), v(x1, y0, z1, w, ht, 0, 0, 1), v(x1, y1, z1, w, 0, 0, 0, 1), v(x0, y1, z1, 0, 0, 0, 0, 1)}, col, layer, emissive);
        quad(new double[][]{v(x0, y0, z0, 0, ht, -1, 0, 0), v(x0, y0, z1, d, ht, -1, 0, 0), v(x0, y1, z1, d, 0, -1, 0, 0), v(x0, y1, z0, 0, 0, -1, 0, 0)}, col, layer, emissive);
        quad(new double[][]{v(x1, y0, z0, 0, ht, 1, 0, 0), v(x1, y1, z0, 0, 0, 1, 0, 0), v(x1, y1, z1, d, 0, 1, 0, 0), v(x1, y0, z1, d, ht, 1, 0, 0)}, col, layer, emissive);
    }

    /** Prism of {@code sides} faces of radius {@code r} along the segment a -> b (ropes, rafters, rolled walls). */
    void tube(double ax, double ay, double az, double bx, double by, double bz, double r, int sides, int col) {
        double dx = bx - ax, dy = by - ay, dz = bz - az;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-6) return;
        int layer = r < DETAIL_SIZE ? DETAIL : SOLID;
        dx /= len; dy /= len; dz /= len;
        double rx = 0, ry = 1, rz = 0;
        if (Math.abs(dy) > 0.9) { rx = 1; ry = 0; }
        double ux = dy * rz - dz * ry, uy = dz * rx - dx * rz, uz = dx * ry - dy * rx;
        double ul = Math.sqrt(ux * ux + uy * uy + uz * uz);
        ux /= ul; uy /= ul; uz /= ul;
        double wx = dy * uz - dz * uy, wy = dz * ux - dx * uz, wz = dx * uy - dy * ux;
        double phase = sides == 4 ? Math.PI / 4 : 0; // square tubes sit flat
        for (int k = 0; k < sides; k++) {
            double a0 = phase + 2 * Math.PI * k / sides, a1 = phase + 2 * Math.PI * (k + 1) / sides, am = (a0 + a1) / 2;
            double c0 = Math.cos(a0), s0 = Math.sin(a0), c1 = Math.cos(a1), s1 = Math.sin(a1);
            double cm = Math.cos(am), sm = Math.sin(am);
            double nx = ux * cm + wx * sm, ny = uy * cm + wy * sm, nz = uz * cm + wz * sm;
            double ox0 = (ux * c0 + wx * s0) * r, oy0 = (uy * c0 + wy * s0) * r, oz0 = (uz * c0 + wz * s0) * r;
            double ox1 = (ux * c1 + wx * s1) * r, oy1 = (uy * c1 + wy * s1) * r, oz1 = (uz * c1 + wz * s1) * r;
            double uv = 2 * Math.PI * r / sides;
            quad(new double[][]{
                    v(ax + ox0, ay + oy0, az + oz0, 0, 0, nx, ny, nz),
                    v(bx + ox0, by + oy0, bz + oz0, len / 2, 0, nx, ny, nz),
                    v(bx + ox1, by + oy1, bz + oz1, len / 2, uv, nx, ny, nz),
                    v(ax + ox1, ay + oy1, az + oz1, 0, uv, nx, ny, nz)}, col, layer, false);
        }
    }

    /**
     * Tube swept along a polyline with a radius and colour per ring (inflatable arch). {@code side} is a fixed
     * direction perpendicular to the whole path.
     */
    void sweep(double[][] path, double[] radius, int[] colors, int sides, double sx, double sy, double sz) {
        int n = path.length;
        double[][][] rings = new double[n][sides][];
        for (int i = 0; i < n; i++) {
            double[] a = path[Math.max(0, i - 1)], b = path[Math.min(n - 1, i + 1)];
            double tx = b[0] - a[0], ty = b[1] - a[1], tz = b[2] - a[2], tl = Math.sqrt(tx * tx + ty * ty + tz * tz);
            tx /= tl; ty /= tl; tz /= tl;
            double wx = ty * sz - tz * sy, wy = tz * sx - tx * sz, wz = tx * sy - ty * sx; // t x side
            for (int k = 0; k < sides; k++) {
                double ang = 2 * Math.PI * k / sides, c = Math.cos(ang), sn = Math.sin(ang);
                double nx = sx * c + wx * sn, ny = sy * c + wy * sn, nz = sz * c + wz * sn;
                rings[i][k] = new double[]{path[i][0] + nx * radius[i], path[i][1] + ny * radius[i], path[i][2] + nz * radius[i], nx, ny, nz};
            }
        }
        for (int i = 0; i + 1 < n; i++) {
            for (int k = 0; k < sides; k++) {
                int k1 = (k + 1) % sides;
                double[] a = rings[i][k], b = rings[i + 1][k], c = rings[i + 1][k1], d = rings[i][k1];
                double u0 = k / (double) sides, u1 = (k + 1) / (double) sides;
                quad(new double[][]{
                        v(a[0], a[1], a[2], u0, i * 0.25, a[3], a[4], a[5]),
                        v(b[0], b[1], b[2], u0, (i + 1) * 0.25, b[3], b[4], b[5]),
                        v(c[0], c[1], c[2], u1, (i + 1) * 0.25, c[3], c[4], c[5]),
                        v(d[0], d[1], d[2], u1, i * 0.25, d[3], d[4], d[5])}, colors[i], SOLID, false);
            }
        }
    }

    /** Rope from a to b sagging by {@code sag} at mid span. */
    void rope(double ax, double ay, double az, double bx, double by, double bz, double sag, double r, int col) {
        // Short ropes don't need many segments to read as a curve.
        double len = Math.sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay) + (bz - az) * (bz - az));
        int n = Mth.clamp((int) Math.ceil(len / 1.5), 2, 6);
        double px = ax, py = ay, pz = az;
        for (int i = 1; i <= n; i++) {
            double u = i / (double) n;
            double x = ax + (bx - ax) * u, y = ay + (by - ay) * u - sag * 4 * u * (1 - u), z = az + (bz - az) * u;
            tube(px, py, pz, x, y, z, r, 3, col);
            px = x; py = y; pz = z;
        }
    }

    /** Wire through the given points (local x y z), with a bulb hanging at every inner point. */
    void festoonPath(double[][] pts, int seed) {
        for (int i = 1; i < pts.length; i++) {
            double[] a = pts[i - 1], b = pts[i];
            tube(a[0], a[1], a[2], b[0], b[1], b[2], 0.018, 3, 0xFF2A2622);
            if (i < pts.length - 1) bulb(b[0], b[1] - 0.16, b[2], TentMeshes.bulbColor(p.lights, seed + i));
        }
    }

    /** Glowing bulb under a wire: four sides and a bottom (the top is against the wire). */
    private void bulb(double x, double y0, double z, int col) {
        double h = 0.055, y1 = y0 + 0.11, x0 = x - h, x1 = x + h, z0 = z - h, z1 = z + h;
        quad(new double[][]{v(x0, y0, z0, 0, 0, 0, -1, 0), v(x1, y0, z0, 1, 0, 0, -1, 0), v(x1, y0, z1, 1, 1, 0, -1, 0), v(x0, y0, z1, 0, 1, 0, -1, 0)}, col, DETAIL, true);
        quad(new double[][]{v(x0, y0, z0, 0, 1, 0, 0, -1), v(x0, y1, z0, 0, 0, 0, 0, -1), v(x1, y1, z0, 1, 0, 0, 0, -1), v(x1, y0, z0, 1, 1, 0, 0, -1)}, col, DETAIL, true);
        quad(new double[][]{v(x0, y0, z1, 0, 1, 0, 0, 1), v(x1, y0, z1, 1, 1, 0, 0, 1), v(x1, y1, z1, 1, 0, 0, 0, 1), v(x0, y1, z1, 0, 0, 0, 0, 1)}, col, DETAIL, true);
        quad(new double[][]{v(x0, y0, z0, 0, 1, -1, 0, 0), v(x0, y0, z1, 1, 1, -1, 0, 0), v(x0, y1, z1, 1, 0, -1, 0, 0), v(x0, y1, z0, 0, 0, -1, 0, 0)}, col, DETAIL, true);
        quad(new double[][]{v(x1, y0, z0, 0, 1, 1, 0, 0), v(x1, y1, z0, 0, 0, 1, 0, 0), v(x1, y1, z1, 1, 0, 1, 0, 0), v(x1, y0, z1, 1, 1, 1, 0, 0)}, col, DETAIL, true);
    }

    /** Hanging wire with bulbs between a and b, sagging by {@code sag} at mid span. */
    void festoon(double ax, double ay, double az, double bx, double by, double bz, double sag, int seed) {
        double len = Math.sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay) + (bz - az) * (bz - az));
        int n = Math.max(2, (int) Math.ceil(len / 0.8));
        double[][] pts = new double[n + 1][];
        for (int i = 0; i <= n; i++) {
            double u = i / (double) n;
            pts[i] = new double[]{ax + (bx - ax) * u, ay + (by - ay) * u - sag * 4 * u * (1 - u), az + (bz - az) * u};
        }
        festoonPath(pts, seed);
    }

    private void put(Layer l, double[] vv, int col, boolean emissive) {
        l.ensure();
        int o = l.count * STRIDE;
        float x = (float) (0.5 + g.toWorldX(vv[0], vv[2])), z = (float) (0.5 + g.toWorldZ(vv[0], vv[2]));
        l.data[o] = x;
        l.data[o + 1] = (float) vv[1];
        l.data[o + 2] = z;
        l.data[o + 3] = (float) vv[3];
        l.data[o + 4] = (float) vv[4];
        l.data[o + 5] = (float) g.toWorldX(vv[5], vv[7]);
        l.data[o + 6] = (float) vv[6];
        l.data[o + 7] = (float) g.toWorldZ(vv[5], vv[7]);
        if (vv.length >= 12 && vv[8] > 0) {
            if (l.flex == null) l.flex = new float[l.color.length * FLEX_STRIDE];
            int f = l.count * FLEX_STRIDE;
            l.flex[f] = (float) vv[8];
            l.flex[f + 1] = (float) g.toWorldX(vv[9], vv[11]);
            l.flex[f + 2] = (float) vv[10];
            l.flex[f + 3] = (float) g.toWorldZ(vv[9], vv[11]);
            l.flex[f + 4] = x * 0.35F + z * 0.22F; // the wave travels across the tent
        } else if (l.flex != null) {
            l.flex[l.count * FLEX_STRIDE] = 0;
        }
        l.color[l.count] = col;
        l.emissive[l.count] = emissive;
        l.light[l.count] = emissive ? LightTexture.FULL_BRIGHT : 0;
        l.count++;
    }

    // ------------------------------------------------------------------ light

    /** Lights every vertex now (first display, so the tent never shows up black). */
    void relightAll(Level level, BlockPos origin, long stamp) {
        relightRunning = true;
        relightLayer = 0;
        relightIndex = 0;
        relightCache = new Long2IntOpenHashMap();
        relightStep(level, origin, Integer.MAX_VALUE, stamp);
    }

    /**
     * Continues the current relight pass for at most {@code budget} vertices; starts a new pass when the last one
     * is older than {@code interval} ticks. Spreads the light lookups of big tents over several frames.
     */
    void relightTick(Level level, BlockPos origin, long time, int interval, int budget) {
        if (!relightRunning) {
            if (time - lightStamp < interval && time >= lightStamp) return;
            relightRunning = true;
            relightLayer = 0;
            relightIndex = 0;
            relightCache = new Long2IntOpenHashMap();
        }
        relightStep(level, origin, budget, time);
    }

    private void relightStep(Level level, BlockPos origin, int budget, long stamp) {
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        int ox = origin.getX(), oy = origin.getY(), oz = origin.getZ();
        while (relightLayer < LAYERS && budget > 0) {
            Layer l = layers[relightLayer];
            while (relightIndex < l.count && budget > 0) {
                int i = relightIndex++;
                budget--;
                if (l.emissive[i]) continue;
                int o = i * STRIDE;
                int x = ox + Mth.floor(l.data[o] + l.data[o + 5] * 0.4F);
                int y = oy + Mth.floor(l.data[o + 1] + l.data[o + 6] * 0.4F);
                int z = oz + Mth.floor(l.data[o + 2] + l.data[o + 7] * 0.4F);
                long key = BlockPos.asLong(x, y, z);
                int lv = relightCache.getOrDefault(key, -1);
                if (lv == -1) {
                    lv = LevelRenderer.getLightColor(level, mp.set(x, y, z));
                    relightCache.put(key, lv);
                }
                if (l.light[i] != lv) {
                    l.light[i] = lv;
                    gpuStale[relightLayer] = true;
                }
            }
            if (relightIndex >= l.count) {
                relightLayer++;
                relightIndex = 0;
            }
        }
        if (relightLayer >= LAYERS) {
            relightRunning = false;
            relightCache = null;
            lightStamp = stamp;
            lit = true;
        }
    }

    // ------------------------------------------------------------------ draw

    boolean hasLayer(int layer) {
        return layers[layer].count > 0;
    }

    // ------------------------------------------------------------------ GPU path

    /**
     * Draws a layer from GPU memory with the tent shader ({@code modelView} already holds the block entity pose);
     * uploads it first when it is new or its light changed.
     */
    void draw(int layer, RenderType type, org.joml.Matrix4f modelView, ShaderInstance shader) {
        if (layers[layer].count == 0) return;
        VertexBuffer vb = gpu[layer];
        if (vb == null || gpuStale[layer]) vb = upload(layer);
        type.setupRenderState();
        vb.bind();
        vb.drawWithShader(modelView, RenderSystem.getProjectionMatrix(), shader);
        VertexBuffer.unbind();
        type.clearRenderState();
    }

    private VertexBuffer upload(int layer) {
        Layer l = layers[layer];
        if (staging == null) staging = new BufferBuilder(1 << 18);
        BufferBuilder bb = staging;
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
        float[] data = l.data, flex = l.flex;
        int[] color = l.color, light = l.light;
        for (int i = 0; i < l.count; i++) {
            int o = i * STRIDE, c = color[i];
            int packed = 0;
            if (flex != null) {
                int f = i * FLEX_STRIDE;
                if (flex[f] > 0) {
                    // Overlay slot reused for the flex: weight in u, octahedral direction in v (see tent.vsh).
                    packed = Math.max(1, Math.round(flex[f] * 1000)) | octEncode(flex[f + 1], flex[f + 2], flex[f + 3]) << 16;
                }
            }
            bb.vertex(data[o], data[o + 1], data[o + 2],
                    ((c >> 16) & 0xFF) * INV255, ((c >> 8) & 0xFF) * INV255, (c & 0xFF) * INV255, (c >>> 24) * INV255,
                    data[o + 3], data[o + 4], packed, light[i], data[o + 5], data[o + 6], data[o + 7]);
        }
        VertexBuffer vb = gpu[layer];
        if (vb == null) vb = gpu[layer] = new VertexBuffer(VertexBuffer.Usage.STATIC);
        vb.bind();
        vb.upload(bb.end());
        VertexBuffer.unbind();
        gpuStale[layer] = false;
        return vb;
    }

    /** Unit vector to 7 + 7 bits, octahedral with y up (decoded in tent.vsh). */
    static int octEncode(float x, float y, float z) {
        float s = Math.abs(x) + Math.abs(y) + Math.abs(z);
        if (s < 1e-6F) return 63 * 128 + 63;
        float px = x / s, pz = z / s;
        if (y < 0) {
            float ox = (1 - Math.abs(pz)) * (px >= 0 ? 1 : -1), oz = (1 - Math.abs(px)) * (pz >= 0 ? 1 : -1);
            px = ox;
            pz = oz;
        }
        int a = Mth.clamp(Math.round((px * 0.5F + 0.5F) * 127), 0, 127), b = Mth.clamp(Math.round((pz * 0.5F + 0.5F) * 127), 0, 127);
        return a * 128 + b;
    }

    /** Frees the GPU buffers (on the render thread). */
    @Override
    public void close() {
        VertexBuffer[] buffers = gpu.clone();
        java.util.Arrays.fill(gpu, null);
        Runnable free = () -> {
            for (VertexBuffer vb : buffers) if (vb != null) vb.close();
        };
        if (RenderSystem.isOnRenderThread()) free.run();
        else RenderSystem.recordRenderCall(free::run);
    }

    /**
     * Draws a layer. {@code wind} is the flutter amplitude in blocks (0 = still), {@code time} in ticks; free parts
     * of the canvas ripple with a wave travelling across the tent.
     *
     * <p>Hot path: the pose is applied by hand and every vertex goes out in one bulk call, which buffer builders
     * write without per-element bookkeeping.
     */
    void emit(int layer, PoseStack.Pose pose, VertexConsumer vc, int overlay, float time, float wind) {
        Layer l = layers[layer];
        int count = l.count;
        if (count == 0) return;
        Matrix4f m = pose.pose();
        Matrix3f nm = pose.normal();
        float m00 = m.m00(), m01 = m.m01(), m02 = m.m02(), m10 = m.m10(), m11 = m.m11(), m12 = m.m12();
        float m20 = m.m20(), m21 = m.m21(), m22 = m.m22(), m30 = m.m30(), m31 = m.m31(), m32 = m.m32();
        float n00 = nm.m00(), n01 = nm.m01(), n02 = nm.m02(), n10 = nm.m10(), n11 = nm.m11(), n12 = nm.m12();
        float n20 = nm.m20(), n21 = nm.m21(), n22 = nm.m22();
        float[] data = l.data, flex = l.flex;
        int[] color = l.color, light = l.light;
        boolean animate = wind > 0 && flex != null;
        float speed = 0.12F + wind * 2.5F, ts = time * speed;
        for (int i = 0; i < count; i++) {
            int o = i * STRIDE;
            float x = data[o], y = data[o + 1], z = data[o + 2];
            if (animate) {
                int f = i * FLEX_STRIDE;
                float w = flex[f];
                if (w > 0) {
                    float phase = ts - flex[f + 4];
                    float d = wind * w * (Mth.sin(phase) + 0.45F * Mth.sin(phase * 2.3F + z * 0.5F));
                    x += flex[f + 1] * d;
                    y += flex[f + 2] * d;
                    z += flex[f + 3] * d;
                }
            }
            float nx = data[o + 5], ny = data[o + 6], nz = data[o + 7];
            int c = color[i];
            vc.vertex(m00 * x + m10 * y + m20 * z + m30, m01 * x + m11 * y + m21 * z + m31, m02 * x + m12 * y + m22 * z + m32,
                    ((c >> 16) & 0xFF) * INV255, ((c >> 8) & 0xFF) * INV255, (c & 0xFF) * INV255, (c >>> 24) * INV255,
                    data[o + 3], data[o + 4], overlay, light[i],
                    n00 * nx + n10 * ny + n20 * nz, n01 * nx + n11 * ny + n21 * nz, n02 * nx + n12 * ny + n22 * nz);
        }
    }
}
