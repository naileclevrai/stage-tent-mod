package com.nailec.stagetents.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Arrays;

/**
 * Small mesh kit for the machines: coloured quads on a plain white sprite, printed quads cut from the decal sheet,
 * and lamps lit at full brightness. Faces are shaded from their normal the way blocks are, so a box reads as a box
 * without lighting. A builder carries a transform stack for parts built in their own frame (wheels, loads, arms).
 */
public final class MeshKit {
    public static final ResourceLocation PLAIN = new ResourceLocation("stagetents", "block/props/white");
    public static final ResourceLocation DECALS = new ResourceLocation("stagetents", "block/props/machine_decals");
    /** Size of the decal sheet in pixels. Rects below are in sheet pixels. */
    public static final int SHEET = 512;

    public static final int PAINT = 0;
    public static final int GLOW = 1;
    public static final int DECAL = 2;
    public static final int DECAL_GLOW = 3;
    /** Box growth step, see {@link Builder#nudge}. */
    static final double GROW_STEP = 0.0015;
    /** How far a printed panel floats off its face: above the largest box growth. */
    static final double DECAL_LIFT = 0.009;

    private MeshKit() {}

    /** Rectangle of the decal sheet, in pixels. */
    public record Rect(int x, int y, int w, int h) {
        float u0() { return (x + 0.5f) / SHEET; }
        float u1() { return (x + w - 0.5f) / SHEET; }
        float v0() { return (y + 0.5f) / SHEET; }
        float v1() { return (y + h - 0.5f) / SHEET; }
    }

    public static int shade(int rgb, float f) {
        int r = Math.min(255, Math.max(0, (int) (((rgb >> 16) & 255) * f)));
        int g = Math.min(255, Math.max(0, (int) (((rgb >> 8) & 255) * f)));
        int b = Math.min(255, Math.max(0, (int) ((rgb & 255) * f)));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public static int mix(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    public static final class Builder {
        private float[] xyz = new float[12 * 512];
        private float[] uv = new float[8 * 512];
        private byte[] mat = new byte[512];
        private int[] color = new int[512];
        private int quads;
        /** Affine transform rows: x' = m0 x + m1 y + m2 z + m3, and so on. */
        private double[] m = {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0};
        private final double[][] stack = new double[16][];
        private int depth;
        /** Turn the automatic face shading off for parts that carry their own (lamps, decals on glass). */
        private boolean flat;
        /**
         * Boxes and bars touching face to face would share a plane and flicker. Each one is grown by its own few
         * millimetres, so two touching faces never land on the same depth.
         */
        private int grow;

        private double nudge() {
            return GROW_STEP * (1 + (grow++ % 4));
        }

        public int quads() {
            return quads;
        }

        public Builder push() {
            stack[depth++] = m.clone();
            return this;
        }

        public Builder pop() {
            m = stack[--depth];
            return this;
        }

        public Builder translate(double x, double y, double z) {
            m[3] += m[0] * x + m[1] * y + m[2] * z;
            m[7] += m[4] * x + m[5] * y + m[6] * z;
            m[11] += m[8] * x + m[9] * y + m[10] * z;
            return this;
        }

        /** Turn about +y, counter-clockwise seen from above, in degrees. */
        public Builder rotY(double deg) {
            double c = Math.cos(Math.toRadians(deg)), s = Math.sin(Math.toRadians(deg));
            return mul(c, 0, s, 0, 1, 0, -s, 0, c);
        }

        /** Turn about +x: positive lifts +z towards +y. */
        public Builder rotX(double deg) {
            double c = Math.cos(Math.toRadians(deg)), s = Math.sin(Math.toRadians(deg));
            return mul(1, 0, 0, 0, c, -s, 0, s, c);
        }

        public Builder rotZ(double deg) {
            double c = Math.cos(Math.toRadians(deg)), s = Math.sin(Math.toRadians(deg));
            return mul(c, -s, 0, s, c, 0, 0, 0, 1);
        }

        public Builder scale(double sx, double sy, double sz) {
            return mul(sx, 0, 0, 0, sy, 0, 0, 0, sz);
        }

        private Builder mul(double a, double b, double c, double d, double e, double f, double g, double h, double i) {
            double[] o = m;
            m = new double[]{
                    o[0] * a + o[1] * d + o[2] * g, o[0] * b + o[1] * e + o[2] * h, o[0] * c + o[1] * f + o[2] * i, o[3],
                    o[4] * a + o[5] * d + o[6] * g, o[4] * b + o[5] * e + o[6] * h, o[4] * c + o[5] * f + o[6] * i, o[7],
                    o[8] * a + o[9] * d + o[10] * g, o[8] * b + o[9] * e + o[10] * h, o[8] * c + o[9] * f + o[10] * i, o[11]};
            return this;
        }

        public Builder flat(boolean on) {
            flat = on;
            return this;
        }

        private void grow() {
            if (quads < color.length) return;
            int n = color.length * 2;
            xyz = Arrays.copyOf(xyz, 12 * n);
            uv = Arrays.copyOf(uv, 8 * n);
            mat = Arrays.copyOf(mat, n);
            color = Arrays.copyOf(color, n);
        }

        /** Quad, counter-clockwise seen from its front. The picture is upright with p0 bottom left and p3 top left. */
        public void quad(double x0, double y0, double z0, double x1, double y1, double z1,
                         double x2, double y2, double z2, double x3, double y3, double z3, int col, int material) {
            quadUv(x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, col, material, 0, 1, 1, 0);
        }

        public void quadUv(double x0, double y0, double z0, double x1, double y1, double z1,
                           double x2, double y2, double z2, double x3, double y3, double z3, int col, int material,
                           float u0, float v1, float u1, float v0) {
            grow();
            int q = quads++;
            int p = q * 12;
            put(p, x0, y0, z0);
            put(p + 3, x1, y1, z1);
            put(p + 6, x2, y2, z2);
            put(p + 9, x3, y3, z3);
            int t = q * 8;
            uv[t] = u0; uv[t + 1] = v1;
            uv[t + 2] = u1; uv[t + 3] = v1;
            uv[t + 4] = u1; uv[t + 5] = v0;
            uv[t + 6] = u0; uv[t + 7] = v0;
            mat[q] = (byte) material;
            if (flat || material == GLOW || material == DECAL_GLOW) {
                color[q] = col | 0xFF000000;
            } else {
                double ax = xyz[p + 6] - xyz[p], ay = xyz[p + 7] - xyz[p + 1], az = xyz[p + 8] - xyz[p + 2];
                double bx = xyz[p + 9] - xyz[p + 3], by = xyz[p + 10] - xyz[p + 4], bz = xyz[p + 11] - xyz[p + 5];
                double nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
                double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
                float f = 1f;
                if (len > 1.0e-12) {
                    nx /= len;
                    ny /= len;
                    nz /= len;
                    f = (float) (nx * nx * 0.66 + ny * ny * (ny > 0 ? 1.0 : 0.52) + nz * nz * 0.82);
                }
                color[q] = shade(col, f);
            }
        }

        private void put(int i, double x, double y, double z) {
            xyz[i] = (float) (m[0] * x + m[1] * y + m[2] * z + m[3]);
            xyz[i + 1] = (float) (m[4] * x + m[5] * y + m[6] * z + m[7]);
            xyz[i + 2] = (float) (m[8] * x + m[9] * y + m[10] * z + m[11]);
        }

        /** Axis-aligned box in the current frame. */
        public void box(double x0, double y0, double z0, double x1, double y1, double z1, int col) {
            box(x0, y0, z0, x1, y1, z1, col, PAINT);
        }

        public void box(double x0, double y0, double z0, double x1, double y1, double z1, int col, int material) {
            double e = nudge();
            x0 -= e;
            y0 -= e;
            z0 -= e;
            x1 += e;
            y1 += e;
            z1 += e;
            rawBox(x0, y0, z0, x1, y1, z1, col, material);
        }

        private void rawBox(double x0, double y0, double z0, double x1, double y1, double z1, int col, int material) {
            quad(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, col, material);
            quad(x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, col, material);
            quad(x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, col, material);
            quad(x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, col, material);
            quad(x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, col, material);
            quad(x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, col, material);
        }

        /** Box with its top edges chamfered by {@code c}: hoods, counterweights, pads. */
        public void chamferBox(double x0, double y0, double z0, double x1, double y1, double z1, double c, int col) {
            double e = nudge();
            x0 -= e;
            y0 -= e;
            z0 -= e;
            x1 += e;
            y1 += e;
            z1 += e;
            double yc = y1 - c;
            rawBox(x0, y0, z0, x1, yc, z1, col, PAINT);
            // Top and the four slopes.
            quad(x0 + c, y1, z1 - c, x1 - c, y1, z1 - c, x1 - c, y1, z0 + c, x0 + c, y1, z0 + c, col, PAINT);
            quad(x0, yc, z1, x1, yc, z1, x1 - c, y1, z1 - c, x0 + c, y1, z1 - c, col, PAINT);
            quad(x1, yc, z0, x0, yc, z0, x0 + c, y1, z0 + c, x1 - c, y1, z0 + c, col, PAINT);
            quad(x0, yc, z0, x0, yc, z1, x0 + c, y1, z1 - c, x0 + c, y1, z0 + c, col, PAINT);
            quad(x1, yc, z1, x1, yc, z0, x1 - c, y1, z0 + c, x1 - c, y1, z1 - c, col, PAINT);
        }

        /** Square bar from p to q. {@code up} picks how it is turned; w is across, h along up. */
        public void beam(double px, double py, double pz, double qx, double qy, double qz,
                         double ux, double uy, double uz, double w, double h, int col) {
            double dx = qx - px, dy = qy - py, dz = qz - pz;
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1.0e-5) return;
            double grown = nudge();
            w += grown * 2;
            h += grown * 2;
            dx /= len;
            dy /= len;
            dz /= len;
            double sx = dy * uz - dz * uy, sy = dz * ux - dx * uz, sz = dx * uy - dy * ux;
            double sl = Math.sqrt(sx * sx + sy * sy + sz * sz);
            if (sl < 1.0e-6) {
                sx = 1;
                sy = 0;
                sz = 0;
                sl = 1;
            }
            sx = sx / sl * w * 0.5;
            sy = sy / sl * w * 0.5;
            sz = sz / sl * w * 0.5;
            double vx = (sy * dz - sz * dy), vy = (sz * dx - sx * dz), vz = (sx * dy - sy * dx);
            double vl = Math.sqrt(vx * vx + vy * vy + vz * vz);
            vx = vx / vl * h * 0.5;
            vy = vy / vl * h * 0.5;
            vz = vz / vl * h * 0.5;
            double[][] c = new double[8][];
            int i = 0;
            for (int e = 0; e < 2; e++) {
                double bx = e == 0 ? px : qx, by = e == 0 ? py : qy, bz = e == 0 ? pz : qz;
                c[i++] = new double[]{bx - sx - vx, by - sy - vy, bz - sz - vz};
                c[i++] = new double[]{bx + sx - vx, by + sy - vy, bz + sz - vz};
                c[i++] = new double[]{bx + sx + vx, by + sy + vy, bz + sz + vz};
                c[i++] = new double[]{bx - sx + vx, by - sy + vy, bz - sz + vz};
            }
            int[][] faces = {{0, 1, 5, 4}, {1, 2, 6, 5}, {2, 3, 7, 6}, {3, 0, 4, 7}, {3, 2, 1, 0}, {4, 5, 6, 7}};
            for (int[] f : faces) {
                quad(c[f[0]][0], c[f[0]][1], c[f[0]][2], c[f[1]][0], c[f[1]][1], c[f[1]][2],
                        c[f[2]][0], c[f[2]][1], c[f[2]][2], c[f[3]][0], c[f[3]][1], c[f[3]][2], col, PAINT);
            }
        }

        /** Round bar from p to q with n sides, open ends. */
        public void tube(double px, double py, double pz, double qx, double qy, double qz, double r, int n, int col) {
            cyl(px, py, pz, qx, qy, qz, r, r, n, col, false, PAINT);
        }

        /** Round bar from p to q with n sides, both ends closed. */
        public void rod(double px, double py, double pz, double qx, double qy, double qz, double r, int n, int col) {
            cyl(px, py, pz, qx, qy, qz, r, r, n, col, true, PAINT);
        }

        /** Cylinder or cone from p (radius r0) to q (radius r1). */
        public void cyl(double px, double py, double pz, double qx, double qy, double qz, double r0, double r1, int n,
                        int col, boolean caps, int material) {
            double dx = qx - px, dy = qy - py, dz = qz - pz;
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1.0e-5) return;
            dx /= len;
            dy /= len;
            dz /= len;
            double ax = Math.abs(dy) < 0.9 ? 0 : 1, ay = Math.abs(dy) < 0.9 ? 1 : 0;
            double sx = dy * 0 - dz * ay, sy = dz * ax - dx * 0, sz = dx * ay - dy * ax;
            double sl = Math.sqrt(sx * sx + sy * sy + sz * sz);
            sx /= sl;
            sy /= sl;
            sz /= sl;
            double ux = dy * sz - dz * sy, uy = dz * sx - dx * sz, uz = dx * sy - dy * sx;
            double[] cx = new double[n + 1], cy = new double[n + 1], cz = new double[n + 1];
            for (int i = 0; i <= n; i++) {
                double a = i * Math.PI * 2 / n;
                double c = Math.cos(a), s = Math.sin(a);
                cx[i] = sx * c + ux * s;
                cy[i] = sy * c + uy * s;
                cz[i] = sz * c + uz * s;
            }
            for (int i = 0; i < n; i++) {
                int j = i + 1;
                quad(px + cx[j] * r0, py + cy[j] * r0, pz + cz[j] * r0, px + cx[i] * r0, py + cy[i] * r0, pz + cz[i] * r0,
                        qx + cx[i] * r1, qy + cy[i] * r1, qz + cz[i] * r1, qx + cx[j] * r1, qy + cy[j] * r1, qz + cz[j] * r1,
                        col, material);
            }
            if (!caps) return;
            for (int i = 0; i < n; i++) {
                int j = i + 1;
                quad(px, py, pz, px + cx[i] * r0, py + cy[i] * r0, pz + cz[i] * r0,
                        px + cx[j] * r0, py + cy[j] * r0, pz + cz[j] * r0, px, py, pz, col, material);
                quad(qx, qy, qz, qx + cx[j] * r1, qy + cy[j] * r1, qz + cz[j] * r1,
                        qx + cx[i] * r1, qy + cy[i] * r1, qz + cz[i] * r1, qx, qy, qz, col, material);
            }
        }

        /** Disc facing +dir at p, as a fan of quads. A printed disc maps the rect onto its square. */
        public void disc(double px, double py, double pz, double dx, double dy, double dz, double r, int n, int col,
                         int material, Rect rect) {
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            dx /= len;
            dy /= len;
            dz /= len;
            double ax = Math.abs(dy) < 0.9 ? 0 : 1, ay = Math.abs(dy) < 0.9 ? 1 : 0;
            double sx = -dz * ay, sy = dz * ax, sz = dx * ay - dy * ax;
            double sl = Math.sqrt(sx * sx + sy * sy + sz * sz);
            sx /= sl;
            sy /= sl;
            sz /= sl;
            double ux = dy * sz - dz * sy, uy = dz * sx - dx * sz, uz = dx * sy - dy * sx;
            for (int i = 0; i < n; i++) {
                double a0 = i * Math.PI * 2 / n, a1 = (i + 1) * Math.PI * 2 / n;
                double c0 = Math.cos(a0), s0 = Math.sin(a0), c1 = Math.cos(a1), s1 = Math.sin(a1);
                float uc = 0.5f, vc = 0.5f, ua = (float) (0.5 + c0 * 0.5), va = (float) (0.5 - s0 * 0.5),
                        ub = (float) (0.5 + c1 * 0.5), vb = (float) (0.5 - s1 * 0.5);
                double x0 = px + (sx * c0 + ux * s0) * r, y0 = py + (sy * c0 + uy * s0) * r, z0 = pz + (sz * c0 + uz * s0) * r;
                double x1 = px + (sx * c1 + ux * s1) * r, y1 = py + (sy * c1 + uy * s1) * r, z1 = pz + (sz * c1 + uz * s1) * r;
                if (rect == null) {
                    quad(px, py, pz, x0, y0, z0, x1, y1, z1, px, py, pz, col, material);
                } else {
                    grow();
                    quadUv(px, py, pz, x0, y0, z0, x1, y1, z1, px, py, pz, col, material, 0, 0, 0, 0);
                    int t = (quads - 1) * 8;
                    float ru0 = rect.u0(), ru1 = rect.u1(), rv0 = rect.v0(), rv1 = rect.v1();
                    float[] us = {uc, ua, ub, uc}, vs = {vc, va, vb, vc};
                    for (int k = 0; k < 4; k++) {
                        uv[t + k * 2] = ru0 + us[k] * (ru1 - ru0);
                        uv[t + k * 2 + 1] = rv0 + vs[k] * (rv1 - rv0);
                    }
                }
            }
        }

        /**
         * Printed panel. Origin is the bottom-left corner seen from the front; {@code u} runs to the right and
         * {@code v} up, both unit vectors; the front is u x v.
         */
        public void decal(double ox, double oy, double oz, double ux, double uy, double uz, double vx, double vy, double vz,
                          double w, double h, Rect rect, int col, boolean glow) {
            double nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
            double nl = Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (nl > 1.0e-9) {
                ox += nx / nl * DECAL_LIFT;
                oy += ny / nl * DECAL_LIFT;
                oz += nz / nl * DECAL_LIFT;
            }
            double ax = ox + ux * w, ay = oy + uy * w, az = oz + uz * w;
            quadUv(ox, oy, oz, ax, ay, az, ax + vx * h, ay + vy * h, az + vz * h, ox + vx * h, oy + vy * h, oz + vz * h,
                    col, glow ? DECAL_GLOW : DECAL, rect.u0(), rect.v1(), rect.u1(), rect.v0());
        }

        /** Printed panel on a face looking at -z (the front of a machine): u is +x, v is +y. */
        public void decalFront(double x0, double y0, double z, double w, double h, Rect rect) {
            decal(x0 + w, y0, z, -1, 0, 0, 0, 1, 0, w, h, rect, 0xFFFFFFFF, false);
        }

        /** Printed panel on a face looking at +z: seen from behind, u is +x. */
        public void decalBack(double x0, double y0, double z, double w, double h, Rect rect) {
            decal(x0, y0, z, 1, 0, 0, 0, 1, 0, w, h, rect, 0xFFFFFFFF, false);
        }

        /** Printed panel on a face looking at -x: the viewer there has +z on the right. */
        public void decalLeft(double x, double y0, double z0, double w, double h, Rect rect) {
            decal(x, y0, z0, 0, 0, 1, 0, 1, 0, w, h, rect, 0xFFFFFFFF, false);
        }

        /** Printed panel on a face looking at +x. */
        public void decalRight(double x, double y0, double z0, double w, double h, Rect rect) {
            decal(x, y0, z0 + w, 0, 0, -1, 0, 1, 0, w, h, rect, 0xFFFFFFFF, false);
        }

        /** Printed panel lying on top, read standing on the -z side (facing +z, whose right hand is -x). */
        public void decalTop(double x0, double y, double z0, double w, double d, Rect rect) {
            decal(x0 + w, y, z0, -1, 0, 0, 0, 0, 1, w, d, rect, 0xFFFFFFFF, false);
        }

        public Mesh finish() {
            return new Mesh(Arrays.copyOf(xyz, quads * 12), Arrays.copyOf(uv, quads * 8),
                    Arrays.copyOf(mat, quads), Arrays.copyOf(color, quads), quads);
        }
    }

    public static final class Mesh {
        final float[] xyz;
        final float[] uv;
        final byte[] mat;
        final int[] color;
        public final int quads;

        Mesh(float[] xyz, float[] uv, byte[] mat, int[] color, int quads) {
            this.xyz = xyz;
            this.uv = uv;
            this.mat = mat;
            this.color = color;
            this.quads = quads;
        }

        public void draw(PoseStack.Pose pose, VertexConsumer vc, int light) {
            var atlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
            draw(pose, vc, atlas.getSprite(PLAIN), atlas.getSprite(DECALS), light);
        }

        public void draw(PoseStack.Pose pose, VertexConsumer vc, TextureAtlasSprite plain, TextureAtlasSprite decals, int light) {
            Matrix4f m = pose.pose();
            Matrix3f n = pose.normal();
            float pu = (plain.getU0() + plain.getU1()) * 0.5f, pv = (plain.getV0() + plain.getV1()) * 0.5f;
            float du0 = decals.getU0(), du1 = decals.getU1(), dv0 = decals.getV0(), dv1 = decals.getV1();
            for (int q = 0; q < quads; q++) {
                int material = mat[q];
                boolean printed = material == DECAL || material == DECAL_GLOW;
                int lit = material == GLOW || material == DECAL_GLOW ? LightTexture.FULL_BRIGHT : light;
                int argb = color[q];
                int r = (argb >> 16) & 255, g = (argb >> 8) & 255, b = argb & 255;
                int base = q * 12;
                for (int v = 0; v < 4; v++) {
                    int i = base + v * 3;
                    float uu = printed ? du0 + uv[q * 8 + v * 2] * (du1 - du0) : pu;
                    float vv = printed ? dv0 + uv[q * 8 + v * 2 + 1] * (dv1 - dv0) : pv;
                    vc.vertex(m, xyz[i], xyz[i + 1], xyz[i + 2]).color(r, g, b, 255).uv(uu, vv)
                            .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lit).normal(n, 0, 1, 0).endVertex();
                }
            }
        }
    }
}
