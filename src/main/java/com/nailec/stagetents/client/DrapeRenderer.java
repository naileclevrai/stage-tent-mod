package com.nailec.stagetents.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.nailec.stagetents.furniture.CurtainBlock;
import com.nailec.stagetents.furniture.DrapeBlockEntity;
import com.nailec.stagetents.furniture.Drapes;
import com.nailec.stagetents.furniture.FriseBlock;
import com.nailec.stagetents.furniture.FurnitureBlock;
import com.nailec.stagetents.furniture.PendrillonBlock;
import net.minecraft.util.Mth;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One pleated cloth for a whole run, hung on one pipe. A frise is a short border with a scalloped hem. A pendrillon
 * is a full leg with a gathered heading. Neighbours move together because the mesh is built once, across every block.
 */
public class DrapeRenderer implements BlockEntityRenderer<DrapeBlockEntity> {
    private static final ResourceLocation WOOL = new ResourceLocation("minecraft", "block/white_wool");
    private static final ResourceLocation IRON = new ResourceLocation("minecraft", "block/iron_block");
    private static final int PIPE = 0xFFD0D4D8;
    private static final int GOLD = 0xFFE1C36A;
    private static final int GOLD_DIM = 0xFFC49A3E;
    private static final int GOLD_DEEP = 0xFFA67C28;
    private static final float PIPE_Y = 0.94F;
    private static final float Z = 0.42F;
    private static final float PIPE_R = 0.035F;
    /** Shown parting (0) and tie (1), plus the tick they were last moved. Keyed by the start of the run. */
    private static final Map<Long, float[]> MOTION = new HashMap<>();

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
        boolean curtain = state.getBlock() instanceof CurtainBlock;
        if (!frise && !curtain && !(state.getBlock() instanceof PendrillonBlock)) return;

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
        VertexConsumer vc = buffers.getBuffer(TentRenderTypes.cutout(InventoryMenu.BLOCK_ATLAS));
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
        float pipeY = curtain || frise ? PIPE_Y : state.getValue(PendrillonBlock.HEIGHT) - 0.06F;
        if (curtain) {
            float[] shown = motion(run.get(0).asLong(), state, time);
            curtain(vc, pose, wool, level, be.getBlockPos(), facing, x0, x1, time, rgb, shown[0], shown[1],
                    CurtainBlock.length(state.getValue(CurtainBlock.DROP)), light);
        } else if (frise) {
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
        float o = 0.02F;
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

    /** Parting and tie ease toward the block setting. A fresh run starts where it already is. */
    private static float[] motion(long key, BlockState state, float time) {
        if (MOTION.size() > 256) MOTION.clear();
        float partTarget = state.getValue(CurtainBlock.MODE) == CurtainBlock.Mode.PARTED
                ? state.getValue(CurtainBlock.OPEN) / (float) CurtainBlock.MAX_OPEN : 0F;
        float tieTarget = state.getValue(CurtainBlock.MODE) == CurtainBlock.Mode.TIED ? 1F : 0F;
        float[] slot = MOTION.computeIfAbsent(key, k -> new float[]{partTarget, tieTarget, time});
        float dt = Mth.clamp(Math.abs(time - slot[2]) / 20F, 0F, 0.1F);
        slot[2] = time;
        float k = 1F - (float) Math.exp(-dt * 5.5F);
        slot[0] += (partTarget - slot[0]) * k;
        slot[1] += (tieTarget - slot[1]) * k;
        if (Math.abs(slot[0] - partTarget) < 0.004F) slot[0] = partTarget;
        if (Math.abs(slot[1] - tieTarget) < 0.004F) slot[1] = tieTarget;
        return slot;
    }

    /**
     * Two panels on one pipe. {@code part} slides them apart along the track. {@code tie} pulls each one
     * back into a rope, and a swagged valance grows across the opening.
     */
    private static void curtain(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite wool, Level level, BlockPos origin,
                                Direction facing, float x0, float x1, float time, float[] rgb, float part, float tie,
                                float drop, int fallbackLight) {
        float top = PIPE_Y - PIPE_R;
        float hem = top - drop;
        int span = Math.max(1, Math.round(x1 - x0));
        float half = span * 0.5F;
        float parted = CurtainBlock.partedStack(span, part);
        float budget = CurtainBlock.tiedBudget(span);
        panel(vc, pose, wool, level, origin, facing, x0, x1, top, hem, time, rgb, true, part, tie, half, parted, budget, 1F, 0F, 1F, fallbackLight);
        panel(vc, pose, wool, level, origin, facing, x0, x1, top, hem, time, rgb, false, part, tie, half, parted, budget, 1F, 0F, 1F, fallbackLight);
        if (part > 0.3F && tie < 0.25F) {
            panel(vc, pose, wool, level, origin, facing, x0, x1, top, hem, time, rgb, true, part, tie, half, parted, budget, 0.42F, -0.045F, 0.88F, fallbackLight);
            panel(vc, pose, wool, level, origin, facing, x0, x1, top, hem, time, rgb, false, part, tie, half, parted, budget, 0.42F, -0.045F, 0.88F, fallbackLight);
        }
        if (tie > 0.05F) swags(vc, pose, wool, level, origin, facing, x0, x1, top, hem, time, rgb, tie, budget, span, fallbackLight);
        if (tie > 0.45F) {
            float show = clamp((tie - 0.45F) / 0.35F, 0F, 1F);
            float w = gatheredWidth(0.46F, budget);
            float y = top + (hem - top) * 0.46F;
            float len = Math.min(0.72F, drop * 0.22F) * show;
            dress(vc, pose, wool, level, origin, facing, x0 + w * 0.42F, y, Z - 0.03F, w * 0.55F, len, fallbackLight);
            dress(vc, pose, wool, level, origin, facing, x1 - w * 0.42F, y, Z - 0.03F, w * 0.55F, len, fallbackLight);
        }
    }

    /** One panel. {@code scale} keeps only the outer part, used for the pile of cloth at a jamb. */
    private static void panel(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite wool, Level level, BlockPos origin,
                              Direction facing, float x0, float x1, float top, float hem, float time, float[] rgb,
                              boolean left, float part, float tie, float half, float parted, float budget,
                              float scale, float zBias, float shadeMul, int fallbackLight) {
        int pleats = Mth.clamp(Math.round(half / 0.17F), 6, 24);
        int cols = pleats * 2;
        int rows = Mth.clamp((int) ((top - hem) / 0.2F), 14, 36);
        float[][] xs = new float[cols + 1][rows + 1];
        float[][] ys = new float[cols + 1][rows + 1];
        float[][] zs = new float[cols + 1][rows + 1];
        float[][] shade = new float[cols + 1][rows + 1];
        float seam = scale < 0.99F ? 0F : (1F - clamp(part * 5F + tie * 5F, 0F, 1F)) * 0.06F;
        for (int c = 0; c <= cols; c++) {
            float u = c / (float) cols;
            for (int r = 0; r <= rows; r++) {
                float t = r / (float) rows;
                float gathered = gatheredWidth(t, budget);
                float straight = Mth.lerp(part, half, parted);
                float width = (Mth.lerp(tie, straight, gathered) + seam) * scale;
                float x = left ? x0 + width * u : x1 - width * u;
                float y = top + (hem - top) * t;
                float phase = u * pleats * (float) (Math.PI * 2);
                float wave = (float) Math.sin(phase) * 0.8F + (float) Math.sin(phase * 2.1F) * 0.2F;
                float amp = 0.062F + 0.025F * tie;
                float narrow = 1F - clamp(width / Math.max(0.2F, half), 0F, 1F);
                amp += 0.018F * narrow;
                float cinch = (float) Math.exp(-((t - 0.46F) / 0.055F) * ((t - 0.46F) / 0.055F));
                // The two panels overlap at the meeting edge. Flatten that strip and park one just behind the other,
                // otherwise the pleats cross and the cloth flickers.
                float meet = clamp((u - 0.72F) / 0.28F, 0F, 1F);
                wave *= 1F - 0.9F * meet;
                float z = Z + zBias + wave * amp * (0.3F + 0.7F * t);
                z += (left ? 0.045F : -0.045F) * meet;
                z -= cinch * 0.055F * tie;
                z -= u * u * 0.055F * (0.45F + 0.55F * (1F - tie));
                if (u < 0.06F) z += (0.06F - u) / 0.06F * 0.05F;
                z += (float) Math.sin(time * 0.065F + x * 1.2F + (left ? 0F : 1.7F)) * 0.011F * t * (1F - cinch * tie);
                z += (float) Math.sin(time * 0.11F + (left ? 0.4F : 2.1F)) * 0.018F * t * u * part;
                if (r == 0) {
                    y = top;
                    z = Z + zBias + wave * 0.008F;
                }
                float ridge = 0.5F + 0.5F * (float) Math.cos(phase);
                float tone = (0.5F + 0.5F * ridge) * (0.92F + 0.08F * (1F - t));
                if (t > 0.94F) tone *= 0.78F;
                if (u > 0.92F) tone *= 0.86F;
                xs[c][r] = x;
                ys[c][r] = y;
                zs[c][r] = z;
                shade[c][r] = tone * shadeMul;
            }
        }
        paint(vc, pose, wool, level, origin, facing, xs, ys, zs, shade, rgb, fallbackLight);
    }

    /** Width of a tied panel, wide at the pipe, cinched at the rope, then a tail. */
    private static float gatheredWidth(float t, float budget) {
        float cinch = budget * 0.30F;
        float tail = budget * 0.78F;
        float tieT = 0.46F;
        if (t < tieT) {
            float k = t / tieT;
            float e = k * k * (3F - 2F * k);
            return budget + (cinch - budget) * e;
        }
        float k = (t - tieT) / (1F - tieT);
        float e = (float) Math.sin(k * Math.PI * 0.5);
        return cinch + (tail - cinch) * e;
    }

    private static void swags(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite wool, Level level, BlockPos origin,
                              Direction facing, float x0, float x1, float top, float hem, float time, float[] rgb,
                              float tie, float budget, int span, int fallbackLight) {
        int n = Mth.clamp(Math.round(span / 2.2F), 2, 6);
        float dip = Math.min(1.15F, Math.max(0.38F, (top - hem) * 0.22F)) * clamp(tie, 0F, 1F);
        float inset = budget * 0.2F;
        float a0 = x0 + inset;
        float a1 = x1 - inset;
        if (a1 - a0 < 0.4F) {
            a0 = x0;
            a1 = x1;
        }
        float width = a1 - a0;
        int cols = 12;
        int rows = 7;
        for (int i = 0; i < n; i++) {
            float xa = a0 + width * i / n;
            float xb = a0 + width * (i + 1) / n;
            float[][] xs = new float[cols + 1][rows + 1];
            float[][] ys = new float[cols + 1][rows + 1];
            float[][] zs = new float[cols + 1][rows + 1];
            float[][] shade = new float[cols + 1][rows + 1];
            for (int c = 0; c <= cols; c++) {
                float u = c / (float) cols;
                float scallop = (float) Math.sin(Math.PI * u);
                float yHem = top - 0.02F - dip * (float) Math.pow(scallop, 0.85);
                for (int r = 0; r <= rows; r++) {
                    float t = r / (float) rows;
                    float phase = u * (float) (Math.PI * 5);
                    float pleat = (float) Math.sin(phase);
                    xs[c][r] = xa + (xb - xa) * u;
                    ys[c][r] = top + (yHem - top) * t;
                    zs[c][r] = Z - 0.04F * t + pleat * 0.022F * t
                            + (float) Math.sin(time * 0.05F + xs[c][r]) * 0.006F * t;
                    if (r == 0) zs[c][r] = Z - 0.01F;
                    float ridge = 0.5F + 0.5F * (float) Math.cos(phase);
                    shade[c][r] = 0.58F + 0.42F * ridge;
                }
            }
            paint(vc, pose, wool, level, origin, facing, xs, ys, zs, shade, rgb, fallbackLight);
        }
    }

    /** A gold rope around the bunch, and two tassels on the front of the knot. */
    private static void dress(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite wool, Level level, BlockPos origin,
                              Direction facing, float x, float y, float z, float rx, float len, int fallbackLight) {
        float u = (wool.getU0() + wool.getU1()) * 0.5F;
        float v = (wool.getV0() + wool.getV1()) * 0.5F;
        int lit = lightAt(level, origin, facing.getClockWise(), x, y, fallbackLight);
        float rz = Math.max(0.045F, rx * 0.55F);
        loop(vc, pose, x, y, z, rx, rz, GOLD, u, v, lit);
        loop(vc, pose, x, y - 0.028F, z, rx * 0.92F, rz * 0.92F, GOLD_DIM, u, v, lit);
        float knotZ = z - rz - 0.012F;
        box(vc, pose, x - 0.025F, y - 0.05F, knotZ - 0.02F, x + 0.025F, y + 0.01F, knotZ + 0.012F, GOLD, u, v, lit);
        if (len < 0.2F) return;
        tassel(vc, pose, x - 0.018F, y - 0.05F, knotZ, len, u, v, lit);
        tassel(vc, pose, x + 0.018F, y - 0.05F, knotZ, len * 0.92F, u, v, lit);
    }

    private static void loop(VertexConsumer vc, PoseStack.Pose pose, float cx, float cy, float cz,
                             float rx, float rz, int color, float u, float v, int light) {
        int n = 14;
        float rope = 0.012F;
        for (int i = 0; i < n; i++) {
            float a0 = (float) (Math.PI * 2 * i / n);
            float a1 = (float) (Math.PI * 2 * (i + 1) / n);
            float x0 = cx + (float) Math.cos(a0) * rx;
            float z0 = cz + (float) Math.sin(a0) * rz;
            float x1 = cx + (float) Math.cos(a1) * rx;
            float z1 = cz + (float) Math.sin(a1) * rz;
            float nx = (float) Math.cos((a0 + a1) * 0.5);
            float nz = (float) Math.sin((a0 + a1) * 0.5);
            solid(vc, pose, x0, cy - rope, z0, x1, cy - rope, z1, x1, cy + rope, z1, x0, cy + rope, z0, color, nx, 0, nz, u, v, light);
        }
    }

    private static void tassel(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float len,
                               float u, float v, int light) {
        float cord = Math.min(0.16F, len * 0.28F);
        cylinderY(vc, pose, x, y - cord, y, z, 0.008F, GOLD_DIM, u, v, light);
        float bulb0 = y - cord - 0.07F;
        cylinderY(vc, pose, x, bulb0, y - cord, z, 0.022F, GOLD, u, v, light);
        skirt(vc, pose, x, bulb0 - Math.max(0.08F, len - cord - 0.07F), bulb0, z, 0.055F, 0.02F, u, v, light);
    }

    private static void skirt(VertexConsumer vc, PoseStack.Pose pose, float x, float y0, float y1, float z,
                              float r0, float r1, float u, float v, int light) {
        int sides = 7;
        for (int i = 0; i < sides; i++) {
            float a0 = (float) (Math.PI * 2 * i / sides);
            float a1 = (float) (Math.PI * 2 * (i + 1) / sides);
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            solid(vc, pose,
                    x + c0 * r1, y1, z + s0 * r1,
                    x + c1 * r1, y1, z + s1 * r1,
                    x + c1 * r0, y0, z + s1 * r0,
                    x + c0 * r0, y0, z + s0 * r0,
                    GOLD_DEEP, (c0 + c1) * 0.5F, 0, (s0 + s1) * 0.5F, u, v, light);
        }
    }

    private static void cylinderY(VertexConsumer vc, PoseStack.Pose pose, float x, float y0, float y1, float z, float r,
                                  int color, float u, float v, int light) {
        int sides = 8;
        for (int i = 0; i < sides; i++) {
            float a0 = (float) (Math.PI * 2 * i / sides);
            float a1 = (float) (Math.PI * 2 * (i + 1) / sides);
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            solid(vc, pose,
                    x + c0 * r, y0, z + s0 * r,
                    x + c1 * r, y0, z + s1 * r,
                    x + c1 * r, y1, z + s1 * r,
                    x + c0 * r, y1, z + s0 * r,
                    color, (c0 + c1) * 0.5F, 0, (s0 + s1) * 0.5F, u, v, light);
        }
    }

    private static void box(VertexConsumer vc, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1,
                            int color, float u, float v, int light) {
        solid(vc, pose, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, color, 0, 0, -1, u, v, light);
        solid(vc, pose, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, color, 0, 0, 1, u, v, light);
        solid(vc, pose, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, color, -1, 0, 0, u, v, light);
        solid(vc, pose, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, color, 1, 0, 0, u, v, light);
        solid(vc, pose, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, color, 0, 1, 0, u, v, light);
        solid(vc, pose, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, color, 0, -1, 0, u, v, light);
    }

    private static void paint(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite wool, Level level, BlockPos origin,
                              Direction facing, float[][] xs, float[][] ys, float[][] zs, float[][] shade, float[] rgb, int fallbackLight) {
        int cols = xs.length - 1;
        int rows = xs[0].length - 1;
        Direction right = facing.getClockWise();
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                float yMid = (ys[c][r] + ys[c + 1][r] + ys[c][r + 1]) / 3F;
                float xMid = (xs[c][r] + xs[c + 1][r]) * 0.5F;
                int lit = lightAt(level, origin, right, xMid, yMid, fallbackLight);
                quad(vc, pose, wool,
                        xs[c][r], ys[c][r], zs[c][r],
                        xs[c + 1][r], ys[c + 1][r], zs[c + 1][r],
                        xs[c + 1][r + 1], ys[c + 1][r + 1], zs[c + 1][r + 1],
                        xs[c][r + 1], ys[c][r + 1], zs[c][r + 1],
                        color(rgb, shade[c][r]), color(rgb, shade[c + 1][r]),
                        color(rgb, shade[c + 1][r + 1]), color(rgb, shade[c][r + 1]), lit);
            }
        }
    }
}
