package com.nailec.stagetents.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.nailec.stagetents.furniture.DrapeBlockEntity;
import com.nailec.stagetents.furniture.Drapes;
import com.nailec.stagetents.furniture.FriseBlock;
import com.nailec.stagetents.furniture.FurnitureBlock;
import com.nailec.stagetents.furniture.PendrillonBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * One pleated cloth for a whole run, hung on one pipe. A frise is a short border with a scalloped hem. A pendrillon
 * is a full leg with a gathered heading. Neighbours move together because the mesh is built once, across every block.
 */
public class DrapeRenderer implements BlockEntityRenderer<DrapeBlockEntity> {
    private static final ResourceLocation WOOL = new ResourceLocation("minecraft", "block/white_wool");
    private static final ResourceLocation IRON = new ResourceLocation("minecraft", "block/iron_block");
    private static final int PIPE = 0xFFD0D4D8;
    private static final float PIPE_Y = 0.94F;
    private static final float Z = 0.42F;
    private static final float PIPE_R = 0.035F;

    /** Which run was already drawn this frame. The foot is submitted twice (chunk pass and off-screen pass). */
    private static double frameId = Double.NaN;
    private static final Set<Long> drawn = new HashSet<>();

    public DrapeRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public boolean shouldRenderOffScreen(DrapeBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void render(DrapeBlockEntity be, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        Level level = be.getLevel();
        BlockState state = be.getBlockState();
        if (level == null) return;
        boolean frise = state.getBlock() instanceof FriseBlock;
        if (!frise && !(state.getBlock() instanceof PendrillonBlock)) return;

        List<BlockPos> run = Drapes.run(level, be.getBlockPos(), state);
        int index = run.indexOf(be.getBlockPos());
        if (index < 0 || run.isEmpty()) return;
        double id = level.getGameTime() + partialTick;
        if (id != frameId) {
            frameId = id;
            drawn.clear();
        }
        if (!drawn.add(run.get(0).asLong())) return;

        Direction facing = state.getValue(FurnitureBlock.FACING);
        float time = level.getGameTime() + partialTick;
        DyeColor dye = state.getValue(FurnitureBlock.COLOR);
        float[] rgb = dye.getTextureDiffuseColors();

        var atlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        TextureAtlasSprite wool = atlas.getSprite(WOOL);
        TextureAtlasSprite iron = atlas.getSprite(IRON);

        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(-facing.toYRot() + 180));
        ps.translate(-0.5, 0, -0.5);
        PoseStack.Pose pose = ps.last();

        // Model +X runs toward the right-hand neighbour, so the start of the run sits at -index.
        float x0 = -index;
        float x1 = x0 + run.size();
        float pipeY = frise ? PIPE_Y : state.getValue(PendrillonBlock.HEIGHT) - 0.06F;
        if (frise) {
            float drop = FriseBlock.length(state.getValue(FriseBlock.DROP));
            cloth(vc, pose, wool, level, be.getBlockPos(), facing, x0, x1, pipeY - PIPE_R, pipeY - PIPE_R - drop, 0.14F, true, time, rgb, true, light);
        } else {
            cloth(vc, pose, wool, level, be.getBlockPos(), facing, x0, x1, pipeY - PIPE_R, 0.02F, 0.24F, false, time, rgb, false, light);
        }
        pipe(vc, pose, iron, x0 - 0.04F, x1 + 0.04F, pipeY, light);
        ps.popPose();
    }

    /**
     * @param head how far the gathered heading hangs below the pipe
     * @param scallop frise hem waves; a pendrillon keeps an almost straight hem
     */
    private static void cloth(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite sprite, Level level, BlockPos origin,
                              Direction facing, float x0, float x1, float top, float hem, float head, boolean scallop,
                              float time, float[] rgb, boolean border, int fallbackLight) {
        int span = Math.max(1, (int) Math.round(x1 - x0));
        int cols = span * 8;
        float spanY = Math.max(0.2F, top - hem - head);
        float step = border ? Math.max(0.12F, spanY / 40F) : Math.max(0.4F, spanY / 48F);
        int body = Math.max(4, (int) Math.ceil(spanY / step));
        int rows = 4 + body;
        float[][] xs = new float[cols + 1][rows + 1];
        float[][] ys = new float[cols + 1][rows + 1];
        float[][] zs = new float[cols + 1][rows + 1];
        float[][] shade = new float[cols + 1][rows + 1];
        for (int c = 0; c <= cols; c++) {
            float u = c / (float) cols;
            float x = x0 + (x1 - x0) * u;
            float pleat = (float) Math.sin(x * (Math.PI * 2 / 0.2));
            float gather = (float) Math.sin(x * (Math.PI * 2 / 0.1));
            for (int r = 0; r <= rows; r++) {
                float t = r / (float) rows;
                float y = top + (hem - top) * t;
                float fromPipe = top - y;
                float inHead = head <= 0 ? 0 : clamp(1F - fromPipe / head, 0, 1);
                float open = clamp((fromPipe - head * 0.35F) / Math.max(0.2F, top - hem), 0, 1);
                float z = Z + pleat * (0.015F + 0.07F * open) + gather * 0.04F * inHead * (1F - inHead) * 4F;
                z += (float) Math.sin(time * 0.08F + x * 1.3F) * (border ? 0.008F : 0.015F) * open;
                if (scallop) y += (float) Math.sin(x * (Math.PI * 2 / 0.5)) * 0.055F * open * open;
                if (r == 0) {
                    y = top;
                    z = Z;
                }
                xs[c][r] = x;
                ys[c][r] = y;
                zs[c][r] = z;
                float ridge = 0.5F + 0.5F * (float) Math.cos(x * (Math.PI * 2 / 0.2));
                shade[c][r] = (0.55F + 0.45F * ridge) * (0.82F + 0.18F * (1F - open));
            }
        }
        Direction right = facing.getClockWise();
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                float yMid = (ys[c][r] + ys[c + 1][r] + ys[c][r + 1]) / 3F;
                float xMid = (xs[c][r] + xs[c + 1][r]) * 0.5F;
                int lit = lightAt(level, origin, right, xMid, yMid, fallbackLight);
                int c00 = color(rgb, shade[c][r]), c10 = color(rgb, shade[c + 1][r]);
                int c11 = color(rgb, shade[c + 1][r + 1]), c01 = color(rgb, shade[c][r + 1]);
                quad(vc, pose, sprite, xs[c][r], ys[c][r], zs[c][r], xs[c + 1][r], ys[c + 1][r], zs[c + 1][r],
                        xs[c + 1][r + 1], ys[c + 1][r + 1], zs[c + 1][r + 1], xs[c][r + 1], ys[c][r + 1], zs[c][r + 1],
                        c00, c10, c11, c01, lit);
            }
        }
    }

    /** One round tube for the whole run. Flat metal, no second shell, no per-block joint. */
    private static void pipe(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite sprite,
                             float x0, float x1, float y, int light) {
        int sides = 12;
        float u = (sprite.getU0() + sprite.getU1()) * 0.5F;
        float v = (sprite.getV0() + sprite.getV1()) * 0.5F;
        for (int i = 0; i < sides; i++) {
            float a0 = (float) (Math.PI * 2 * i / sides);
            float a1 = (float) (Math.PI * 2 * (i + 1) / sides);
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            float y0 = y + c0 * PIPE_R, z0 = Z + s0 * PIPE_R;
            float y1 = y + c1 * PIPE_R, z1 = Z + s1 * PIPE_R;
            float nx = 0, ny = (c0 + c1) * 0.5F, nz = (s0 + s1) * 0.5F;
            solid(vc, pose, x0, y0, z0, x0, y1, z1, x1, y1, z1, x1, y0, z0, PIPE, nx, ny, nz, u, v, light);
        }
        cap(vc, pose, x0, y, false, u, v, light);
        cap(vc, pose, x1, y, true, u, v, light);
    }

    private static void cap(VertexConsumer vc, PoseStack.Pose pose, float x, float y, boolean positive,
                            float u, float v, int light) {
        int sides = 12;
        float nx = positive ? 1F : -1F;
        for (int i = 0; i < sides; i++) {
            float a0 = (float) (Math.PI * 2 * i / sides);
            float a1 = (float) (Math.PI * 2 * (i + 1) / sides);
            float y0 = y + (float) Math.cos(a0) * PIPE_R, z0 = Z + (float) Math.sin(a0) * PIPE_R;
            float y1 = y + (float) Math.cos(a1) * PIPE_R, z1 = Z + (float) Math.sin(a1) * PIPE_R;
            if (positive) solid(vc, pose, x, y, Z, x, y0, z0, x, y1, z1, x, y, Z, PIPE, nx, 0, 0, u, v, light);
            else solid(vc, pose, x, y, Z, x, y1, z1, x, y0, z0, x, y, Z, PIPE, nx, 0, 0, u, v, light);
        }
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite sprite,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3,
                             int c0, int c1, int c2, int c3, int light) {
        float nx = (y1 - y0) * (z2 - z0) - (z1 - z0) * (y2 - y0);
        float ny = (z1 - z0) * (x2 - x0) - (x1 - x0) * (z2 - z0);
        float nz = (x1 - x0) * (y2 - y0) - (y1 - y0) * (x2 - x0);
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-6F) return;
        nx /= len; ny /= len; nz /= len;
        put(vc, pose, sprite, x0, y0, z0, c0, nx, ny, nz, light);
        put(vc, pose, sprite, x1, y1, z1, c1, nx, ny, nz, light);
        put(vc, pose, sprite, x2, y2, z2, c2, nx, ny, nz, light);
        put(vc, pose, sprite, x3, y3, z3, c3, nx, ny, nz, light);
        float o = 0.004F;
        put(vc, pose, sprite, x0 - nx * o, y0 - ny * o, z0 - nz * o, c0, -nx, -ny, -nz, light);
        put(vc, pose, sprite, x3 - nx * o, y3 - ny * o, z3 - nz * o, c3, -nx, -ny, -nz, light);
        put(vc, pose, sprite, x2 - nx * o, y2 - ny * o, z2 - nz * o, c2, -nx, -ny, -nz, light);
        put(vc, pose, sprite, x1 - nx * o, y1 - ny * o, z1 - nz * o, c1, -nx, -ny, -nz, light);
    }

    private static void solid(VertexConsumer vc, PoseStack.Pose pose,
                              float x0, float y0, float z0, float x1, float y1, float z1,
                              float x2, float y2, float z2, float x3, float y3, float z3,
                              int color, float nx, float ny, float nz, float u, float v, int light) {
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 1e-6F) { nx /= len; ny /= len; nz /= len; }
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        int r = (color >> 16) & 255, g = (color >> 8) & 255, b = color & 255;
        vc.vertex(m, x0, y0, z0).color(r, g, b, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, nx, ny, nz).endVertex();
        vc.vertex(m, x1, y1, z1).color(r, g, b, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, nx, ny, nz).endVertex();
        vc.vertex(m, x2, y2, z2).color(r, g, b, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, nx, ny, nz).endVertex();
        vc.vertex(m, x3, y3, z3).color(r, g, b, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, nx, ny, nz).endVertex();
    }

    private static void put(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite sprite,
                            float x, float y, float z, int color, float nx, float ny, float nz, int light) {
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        float uf = x * 2F;
        float vf = y * 2F;
        uf -= (float) Math.floor(uf);
        vf -= (float) Math.floor(vf);
        float u = sprite.getU(uf * 16F);
        float v = sprite.getV(vf * 16F);
        vc.vertex(m, x, y, z).color((color >> 16) & 255, (color >> 8) & 255, color & 255, 255)
                .uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, nx, ny, nz).endVertex();
    }

    private static int lightAt(Level level, BlockPos origin, Direction right, float localX, float y, int fallback) {
        if (level == null) return fallback;
        int step = (int) Math.floor(localX);
        BlockPos at = origin.relative(right, step).offset(0, (int) Math.floor(y), 0);
        return LevelRenderer.getLightColor(level, at);
    }

    private static int color(float[] rgb, float shade) {
        int r = clamp255(rgb[0] * shade);
        int g = clamp255(rgb[1] * shade);
        int b = clamp255(rgb[2] * shade);
        return (r << 16) | (g << 8) | b;
    }

    private static int clamp255(float v) {
        return Math.max(0, Math.min(255, (int) (v * 255F)));
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
