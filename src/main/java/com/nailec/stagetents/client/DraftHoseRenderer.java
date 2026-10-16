package com.nailec.stagetents.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nailec.stagetents.furniture.DraftBeer;
import com.nailec.stagetents.furniture.DraftBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Beer hose, gas hose, and the short stream while a tap is pouring. */
public class DraftHoseRenderer implements BlockEntityRenderer<DraftBlockEntity> {
    private static final ResourceLocation WOOL = new ResourceLocation("minecraft", "block/white_wool");
    private static final int BEER = 0xFF6B3A16;
    private static final int GAS = 0xFF1A4E86;
    private static final int STREAM = 0xFFE0A04A;
    private static final int GLASS = 0xFFD5E2EA;
    private static final int ALE = 0xFFC47820;
    private static final int FOAM = 0xFFF4EBD4;
    private static final float PINT_H = 0.40F;
    private static final float PINT_RB = 0.058F;
    private static final float PINT_RT = 0.080F;

    private static double frameId = Double.NaN;
    private static final Set<Long> drawn = new HashSet<>();

    public DraftHoseRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public boolean shouldRenderOffScreen(DraftBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public void render(DraftBlockEntity be, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        Level level = be.getLevel();
        if (level == null) return;
        double id = level.getGameTime() + partialTick;
        if (id != frameId) {
            frameId = id;
            drawn.clear();
        }
        var atlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
        TextureAtlasSprite wool = atlas.getSprite(WOOL);
        PoseStack.Pose pose = ps.last();
        BlockPos origin = be.getBlockPos();
        VertexConsumer hose = buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        hose(level, be, origin, DraftBeer.Line.BEER, hose, pose, wool, BEER, 0.016F, light);
        hose(level, be, origin, DraftBeer.Line.GAS, hose, pose, wool, GAS, 0.010F, light);
        if (level.getGameTime() < be.pourUntil()) {
            Vec3 faucet = DraftBeer.faucet(be.getBlockState(), origin);
            float x = (float) (faucet.x - origin.getX());
            float y = (float) (faucet.y - origin.getY());
            float z = (float) (faucet.z - origin.getZ());
            float base = (float) DraftBeer.tray(be.getBlockState());
            float progress = 1F - (be.pourUntil() - (float) id) / DraftBeer.POUR_TICKS;
            progress = Mth.clamp(progress, 0F, 1F);
            VertexConsumer liquid = buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
            float beerTop = pint(liquid, pose, wool, x, base, z, progress, light);
            ribbon(liquid, pose, wool, x, y, z, x, beerTop, z, 0.006F, 0F, STREAM, 230, light);
            ribbon(liquid, pose, wool, x, y, z, x, beerTop, z, 0F, 0.006F, STREAM, 230, light);
        }
    }

    private static void hose(Level level, DraftBlockEntity be, BlockPos origin, DraftBeer.Line line,
                             VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite sprite, int color, float width, int light) {
        BlockPos peer = be.peer(line);
        if (peer == null || !level.isLoaded(peer)) return;
        if (DraftBeer.kind(level.getBlockState(peer)) == null) return;
        long key = Math.min(origin.asLong(), peer.asLong()) ^ (line == DraftBeer.Line.GAS ? 0x9E3779B97F4A7C15L : 0L);
        if (!drawn.add(key)) return;
        Vec3 a = DraftBeer.anchor(level, origin, line);
        Vec3 b = DraftBeer.anchor(level, peer, line);
        List<Vec3> points = new ArrayList<>();
        points.add(a.subtract(origin.getX(), origin.getY(), origin.getZ()));
        for (Vec3 pin : be.path(line)) points.add(pin.subtract(origin.getX(), origin.getY(), origin.getZ()));
        points.add(b.subtract(origin.getX(), origin.getY(), origin.getZ()));
        tube(vc, pose, sprite, points, color, width, light);
    }

    /** Round hose through the given points, with a short droop on each open run. Points are already in pose space. */
    public static void tube(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite sprite,
                            List<Vec3> points, int color, float radius, int light) {
        int n = points.size();
        if (n < 2) return;
        List<Vec3> samples = new ArrayList<>();
        for (int i = 0; i < n - 1; i++) {
            Vec3 a = points.get(i);
            Vec3 b = points.get(i + 1);
            Vec3 p0 = points.get(Math.max(0, i - 1));
            Vec3 p3 = points.get(Math.min(n - 1, i + 2));
            double horiz = Math.sqrt((b.x - a.x) * (b.x - a.x) + (b.z - a.z) * (b.z - a.z));
            double sag = horiz > Math.abs(b.y - a.y) ? Mth.clamp((horiz - 0.75) * 0.045, 0.0, 0.40) : 0.0;
            int steps = Mth.clamp((int) (a.distanceTo(b) * 5.0), 3, 14);
            for (int s = 0; s < steps; s++) {
                float t = s / (float) steps;
                Vec3 c = catmull(p0, a, b, p3, t);
                samples.add(c.add(0, -sag * 4.0 * t * (1.0 - t), 0));
            }
        }
        samples.add(points.get(n - 1));
        int count = samples.size();
        int sides = 6;
        Vec3 up = new Vec3(0, 1, 0);
        float[] rx = new float[sides];
        float[] ry = new float[sides];
        float[] rz = new float[sides];
        float[] nx = new float[sides];
        float[] ny = new float[sides];
        float[] nz = new float[sides];
        boolean have = false;
        for (int i = 0; i < count; i++) {
            Vec3 center = samples.get(i);
            Vec3 ahead = samples.get(Math.min(count - 1, i + 1));
            Vec3 behind = samples.get(Math.max(0, i - 1));
            Vec3 tangent = ahead.subtract(behind);
            if (tangent.lengthSqr() < 1e-8) tangent = new Vec3(0, 1, 0);
            else tangent = tangent.normalize();
            Vec3 side = tangent.cross(up);
            if (side.lengthSqr() < 1e-6) side = tangent.cross(new Vec3(1, 0, 0));
            side = side.normalize();
            up = side.cross(tangent).normalize();
            float reach = radius * (i < 2 || i > count - 3 ? 1.45F : 1F);
            float[] px = new float[sides];
            float[] py = new float[sides];
            float[] pz = new float[sides];
            for (int k = 0; k < sides; k++) {
                double ang = Math.PI * 2.0 * k / sides;
                Vec3 p = center.add(side.scale(Math.cos(ang) * reach)).add(up.scale(Math.sin(ang) * reach));
                px[k] = (float) p.x;
                py[k] = (float) p.y;
                pz[k] = (float) p.z;
            }
            if (have) {
                for (int k = 0; k < sides; k++) {
                    int k1 = (k + 1) % sides;
                    int shade = k == 0 ? lighten(color, 42) : k == 3 ? lighten(color, -38) : k == 1 || k == 5 ? lighten(color, 16) : color;
                    quad(vc, pose, sprite,
                            nx[k], ny[k], nz[k], nx[k1], ny[k1], nz[k1],
                            px[k1], py[k1], pz[k1], px[k], py[k], pz[k], shade, 255, light);
                }
            }
            System.arraycopy(px, 0, rx, 0, sides);
            System.arraycopy(py, 0, ry, 0, sides);
            System.arraycopy(pz, 0, rz, 0, sides);
            nx = rx;
            ny = ry;
            nz = rz;
            rx = px;
            ry = py;
            rz = pz;
            have = true;
        }
    }

    private static Vec3 catmull(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        return new Vec3(
                cm(p0.x, p1.x, p2.x, p3.x, t, t2, t3),
                cm(p0.y, p1.y, p2.y, p3.y, t, t2, t3),
                cm(p0.z, p1.z, p2.z, p3.z, t, t2, t3));
    }

    private static double cm(double p0, double p1, double p2, double p3, float t, float t2, float t3) {
        return 0.5 * ((2 * p1) + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 + (-p0 + 3 * p1 - 3 * p2 + p3) * t3);
    }

    private static int lighten(int color, int delta) {
        int r = Mth.clamp(((color >> 16) & 255) + delta, 0, 255);
        int g = Mth.clamp(((color >> 8) & 255) + delta, 0, 255);
        int b = Mth.clamp((color & 255) + delta, 0, 255);
        return (color & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    private static void ribbon(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite sprite,
                               float x0, float y0, float z0, float x1, float y1, float z1,
                               float sideX, float sideZ, int color, int alpha, int light) {
        quad(vc, pose, sprite,
                x0 - sideX, y0, z0 - sideZ, x1 - sideX, y1, z1 - sideZ,
                x1 + sideX, y1, z1 + sideZ, x0 + sideX, y0, z0 + sideZ, color, alpha, light);
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite sprite,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3, int color, int alpha, int light) {
        float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        int r = (color >> 16) & 255, g = (color >> 8) & 255, b = color & 255;
        vc.vertex(m, x0, y0, z0).color(r, g, b, alpha).uv(u0, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0, 1, 0).endVertex();
        vc.vertex(m, x1, y1, z1).color(r, g, b, alpha).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0, 1, 0).endVertex();
        vc.vertex(m, x2, y2, z2).color(r, g, b, alpha).uv(u1, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0, 1, 0).endVertex();
        vc.vertex(m, x3, y3, z3).color(r, g, b, alpha).uv(u0, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0, 1, 0).endVertex();
    }

    /** Pint standing on the tray. Beer rises with the pour, then a foam head. Returns the height the stream should meet. */
    private static float pint(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite sprite,
                              float x, float y, float z, float progress, int light) {
        float wall = 0.007F;
        taper(vc, pose, sprite, x, y + 0.022F, z, PINT_RB, y + PINT_H, z, PINT_RT, GLASS, 78, light, true);
        taper(vc, pose, sprite, x, y + 0.030F, z, PINT_RB - wall, y + PINT_H - 0.004F, z, PINT_RT - wall, GLASS, 55, light, false);
        disc(vc, pose, sprite, x, y + PINT_H, z, PINT_RT - wall, PINT_RT, GLASS, 90, light, true);
        taper(vc, pose, sprite, x, y, z, PINT_RB + 0.004F, y + 0.026F, z, PINT_RB + 0.004F, GLASS, 110, light, true);
        disc(vc, pose, sprite, x, y + 0.026F, z, 0.001F, PINT_RB, GLASS, 120, light, true);
        float beer = y + 0.032F + (PINT_H - 0.07F) * progress;
        float rb = PINT_RB - wall - 0.003F;
        float rt = Mth.lerp(progress, rb, PINT_RT - wall - 0.004F);
        if (progress > 0.02F) {
            taper(vc, pose, sprite, x, y + 0.032F, z, rb, beer, z, rt, ALE, 225, light, true);
            disc(vc, pose, sprite, x, beer, z, 0.001F, rt, ALE, 235, light, true);
        }
        if (progress > 0.72F) {
            float head = (progress - 0.72F) / 0.28F;
            float foamTop = beer + 0.004F + 0.018F * head;
            taper(vc, pose, sprite, x, beer - 0.004F, z, rt * 0.94F, foamTop, z, rt + 0.002F, FOAM, 240, light, true);
            disc(vc, pose, sprite, x, foamTop, z, 0.001F, rt + 0.002F, FOAM, 245, light, true);
            return foamTop + 0.01F;
        }
        return Math.max(y + 0.05F, beer);
    }

    private static void taper(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite sprite,
                              float x0, float y0, float z0, float r0, float y1, float z1, float r1,
                              int color, int alpha, int light, boolean outward) {
        int n = 16;
        for (int i = 0; i < n; i++) {
            float a0 = (float) (Math.PI * 2 * i / n);
            float a1 = (float) (Math.PI * 2 * (i + 1) / n);
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            float xa = x0 + c0 * r0, za = z0 + s0 * r0;
            float xb = x0 + c1 * r0, zb = z0 + s1 * r0;
            float xc = x0 + c1 * r1, zc = z1 + s1 * r1;
            float xd = x0 + c0 * r1, zd = z1 + s0 * r1;
            if (outward) quad(vc, pose, sprite, xa, y0, za, xb, y0, zb, xc, y1, zc, xd, y1, zd, color, alpha, light);
            else quad(vc, pose, sprite, xa, y0, za, xd, y1, zd, xc, y1, zc, xb, y0, zb, color, alpha, light);
        }
    }

    private static void disc(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite sprite,
                             float x, float y, float z, float inner, float outer, int color, int alpha, int light, boolean up) {
        int n = 16;
        for (int i = 0; i < n; i++) {
            float a0 = (float) (Math.PI * 2 * i / n);
            float a1 = (float) (Math.PI * 2 * (i + 1) / n);
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            float ix0 = x + c0 * inner, iz0 = z + s0 * inner, ix1 = x + c1 * inner, iz1 = z + s1 * inner;
            float ox0 = x + c0 * outer, oz0 = z + s0 * outer, ox1 = x + c1 * outer, oz1 = z + s1 * outer;
            if (up) quad(vc, pose, sprite, ix0, y, iz0, ix1, y, iz1, ox1, y, oz1, ox0, y, oz0, color, alpha, light);
            else quad(vc, pose, sprite, ix0, y, iz0, ox0, y, oz0, ox1, y, oz1, ix1, y, iz1, color, alpha, light);
        }
    }
}
