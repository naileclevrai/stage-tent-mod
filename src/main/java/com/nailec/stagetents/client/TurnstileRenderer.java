package com.nailec.stagetents.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.nailec.stagetents.StageTents;
import com.nailec.stagetents.furniture.TurnstileBlock;
import com.nailec.stagetents.furniture.TurnstileBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Draws the moving parts of a turnstile: the three-arm rotor, and the LED displays at full brightness.
 *
 * <p>The rotor is a tripod: its arms point along the three edges of a cube corner, the rotation axis is the cube
 * diagonal, tilted so one arm lies horizontal across the lane while the other two hang down beside the cabinet. A
 * third of a turn brings the next arm up into the horizontal position.
 */
public class TurnstileRenderer implements BlockEntityRenderer<TurnstileBlockEntity> {
    // Shared with tools/props/props.py (TURNSTILE_HUB, TURNSTILE_ARM); model space, facing north.
    private static final float HUB_X = 0.385F, HUB_Y = 0.92F, HUB_Z = 0.5F, ARM = 0.60F, ARM_R = 0.019F;
    private static final float S = (float) Math.sqrt(0.5);
    private static final float[][] ARMS = {{1, 0, 0}, {0, -S, -S}, {0, -S, S}};
    private static final float[] AXIS = normalize(new float[]{1, -(float) Math.sqrt(2), 0});
    private static final ResourceLocation CHROME = new ResourceLocation(StageTents.MOD_ID, "block/props/chrome");
    private static final ResourceLocation ARROW = new ResourceLocation(StageTents.MOD_ID, "block/props/led_arrow");
    private static final ResourceLocation CROSS = new ResourceLocation(StageTents.MOD_ID, "block/props/led_cross");

    public TurnstileRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(TurnstileBlockEntity be, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof TurnstileBlock)) return;
        Direction facing = state.getValue(TurnstileBlock.FACING);
        var atlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));

        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(-facing.toYRot() + 180));
        ps.translate(-0.5, 0, -0.5);
        PoseStack.Pose pose = ps.last();

        // Rotor: walking forward (towards -z in model space) pulls the horizontal arm down and forward.
        float theta = -be.angle(partialTick) * (float) (Math.PI * 2 / 3);
        TextureAtlasSprite chrome = atlas.getSprite(CHROME);
        float[] hub = {HUB_X, HUB_Y, HUB_Z};
        for (float[] arm : ARMS) {
            float[] d = rotate(arm, AXIS, theta);
            float[] a = {hub[0] + d[0] * 0.05F, hub[1] + d[1] * 0.05F, hub[2] + d[2] * 0.05F};
            float[] b = {hub[0] + d[0] * ARM, hub[1] + d[1] * ARM, hub[2] + d[2] * ARM};
            tube(vc, pose, chrome, a, b, ARM_R, light, overlay);
            // Rubber cap at the end of each arm.
            float[] c = {hub[0] + d[0] * (ARM + 0.02F), hub[1] + d[1] * (ARM + 0.02F), hub[2] + d[2] * (ARM + 0.02F)};
            tube(vc, pose, chrome, b, c, ARM_R * 1.1F, light, overlay);
        }
        // Rotor head on the axis.
        float[] h0 = {hub[0] - AXIS[0] * 0.02F, hub[1] - AXIS[1] * 0.02F, hub[2] - AXIS[2] * 0.02F};
        float[] h1 = {hub[0] + AXIS[0] * 0.07F, hub[1] + AXIS[1] * 0.07F, hub[2] + AXIS[2] * 0.07F};
        tube(vc, pose, chrome, h0, h1, 0.05F, light, overlay);

        // LED displays: green arrow when open, red cross when closed. The display facing people coming the wrong
        // way always shows a cross unless the gate is free.
        boolean open = state.getValue(TurnstileBlock.OPEN);
        boolean free = state.getValue(TurnstileBlock.MODE) == TurnstileBlock.Mode.FREE && !be.oneWay();
        TextureAtlasSprite go = atlas.getSprite(ARROW), stop = atlas.getSprite(CROSS);
        TextureAtlasSprite entry = open ? go : stop, exit = free ? go : stop;
        int full = LightTexture.FULL_BRIGHT;
        // Top display on the sloped head, arrow pointing the way through (-z).
        float yA = 1.07F - 0.07F * 0.10F / 0.34F + 0.006F, yB = 1.07F - 0.07F * 0.25F / 0.34F + 0.006F;
        quad(vc, pose, entry, new float[][]{{0.10F, yA, 0.63F}, {0.25F, yB, 0.63F}, {0.25F, yB, 0.37F}, {0.10F, yA, 0.37F}},
                new float[]{0.2F, 0.98F, 0}, full, overlay);
        // End displays: the +z end faces people walking in, the -z end those coming the wrong way.
        quad(vc, pose, entry, new float[][]{{0.07F, 0.885F, 0.948F}, {0.27F, 0.885F, 0.948F}, {0.27F, 0.995F, 0.948F}, {0.07F, 0.995F, 0.948F}},
                new float[]{0, 0, 1}, full, overlay);
        quad(vc, pose, exit, new float[][]{{0.27F, 0.885F, 0.052F}, {0.07F, 0.885F, 0.052F}, {0.07F, 0.995F, 0.052F}, {0.27F, 0.995F, 0.052F}},
                new float[]{0, 0, -1}, full, overlay);
        ps.popPose();
    }

    // ------------------------------------------------------------------ geometry

    private static float[] normalize(float[] v) {
        float l = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return new float[]{v[0] / l, v[1] / l, v[2] / l};
    }

    /** Rodrigues rotation of {@code v} around unit axis {@code k} by {@code t} radians. */
    private static float[] rotate(float[] v, float[] k, float t) {
        float c = (float) Math.cos(t), s = (float) Math.sin(t);
        float dot = v[0] * k[0] + v[1] * k[1] + v[2] * k[2];
        float cx = k[1] * v[2] - k[2] * v[1], cy = k[2] * v[0] - k[0] * v[2], cz = k[0] * v[1] - k[1] * v[0];
        return new float[]{
                v[0] * c + cx * s + k[0] * dot * (1 - c),
                v[1] * c + cy * s + k[1] * dot * (1 - c),
                v[2] * c + cz * s + k[2] * dot * (1 - c)};
    }

    /** Eight-sided tube from a to b, textured with {@code sprite}. */
    private static void tube(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite sprite, float[] a, float[] b, float r, int light, int overlay) {
        float dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2];
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-5F) return;
        dx /= len; dy /= len; dz /= len;
        float rx = 0, ry = 1, rz = 0;
        if (Math.abs(dy) > 0.9F) { rx = 1; ry = 0; }
        float ux = dy * rz - dz * ry, uy = dz * rx - dx * rz, uz = dx * ry - dy * rx;
        float ul = (float) Math.sqrt(ux * ux + uy * uy + uz * uz);
        ux /= ul; uy /= ul; uz /= ul;
        float wx = dy * uz - dz * uy, wy = dz * ux - dx * uz, wz = dx * uy - dy * ux;
        int sides = 8;
        for (int k = 0; k < sides; k++) {
            double a0 = Math.PI * 2 * k / sides, a1 = Math.PI * 2 * (k + 1) / sides;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            float[] n0 = {ux * c0 + wx * s0, uy * c0 + wy * s0, uz * c0 + wz * s0};
            float[] n1 = {ux * c1 + wx * s1, uy * c1 + wy * s1, uz * c1 + wz * s1};
            float u0 = sprite.getU((double) k / sides * 16), u1 = sprite.getU((double) (k + 1) / sides * 16);
            float v0 = sprite.getV(0), v1 = sprite.getV(Math.min(16, len * 16));
            vertex(vc, pose, a[0] + n0[0] * r, a[1] + n0[1] * r, a[2] + n0[2] * r, u0, v0, n0, light, overlay);
            vertex(vc, pose, a[0] + n1[0] * r, a[1] + n1[1] * r, a[2] + n1[2] * r, u1, v0, n1, light, overlay);
            vertex(vc, pose, b[0] + n1[0] * r, b[1] + n1[1] * r, b[2] + n1[2] * r, u1, v1, n1, light, overlay);
            vertex(vc, pose, b[0] + n0[0] * r, b[1] + n0[1] * r, b[2] + n0[2] * r, u0, v1, n0, light, overlay);
        }
    }

    /** Flat textured quad, corners counter-clockwise seen from the front (v up = arrow up). */
    private static void quad(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite sprite, float[][] p, float[] n, int light, int overlay) {
        float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
        vertex(vc, pose, p[0][0], p[0][1], p[0][2], u0, v1, n, light, overlay);
        vertex(vc, pose, p[1][0], p[1][1], p[1][2], u1, v1, n, light, overlay);
        vertex(vc, pose, p[2][0], p[2][1], p[2][2], u1, v0, n, light, overlay);
        vertex(vc, pose, p[3][0], p[3][1], p[3][2], u0, v0, n, light, overlay);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float u, float v, float[] n, int light, int overlay) {
        Matrix4f m = pose.pose();
        Matrix3f nm = pose.normal();
        vc.vertex(m, x, y, z).color(255, 255, 255, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nm, n[0], n[1], n[2]).endVertex();
    }
}
