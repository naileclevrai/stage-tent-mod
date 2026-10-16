package com.nailec.stagetents.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nailec.stagetents.furniture.FestoonBlock;
import com.nailec.stagetents.furniture.FestoonBlockEntity;
import com.nailec.stagetents.furniture.FurnitureBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
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
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.HashSet;
import java.util.Set;

/** Mast, hoop and the cables this post owns. A cable runs to any linked post, diagonal included. */
public class FestoonRenderer implements BlockEntityRenderer<FestoonBlockEntity> {
    private static final ResourceLocation WOOL = new ResourceLocation("minecraft", "block/white_wool");
    private static final ResourceLocation IRON = new ResourceLocation("minecraft", "block/iron_block");
    private static final ResourceLocation CABLE = new ResourceLocation("minecraft", "block/black_concrete");
    private static final int STEEL = 0xFF3E434A;
    private static final int STEEL_DARK = 0xFF2A2E34;
    private static final int WIRE = 0xFF241F1C;
    private static final float LAMP = 0.07F;

    private static double frameId = Double.NaN;
    private static final Set<Long> drawn = new HashSet<>();

    public FestoonRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public boolean shouldRenderOffScreen(FestoonBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public void render(FestoonBlockEntity be, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        Level level = be.getLevel();
        if (level == null) return;
        double id = level.getGameTime() + partialTick;
        if (id != frameId) {
            frameId = id;
            drawn.clear();
        }
        if (!drawn.add(be.getBlockPos().asLong())) return;

        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof FestoonBlock)) return;
        boolean lit = state.getValue(FestoonBlock.LIT);
        DyeColor dye = state.getValue(FurnitureBlock.COLOR);
        float[] rgb = dye.getTextureDiffuseColors();
        int color = pack(rgb[0], rgb[1], rgb[2], lit ? 1F : 0.5F);
        int bulbLight = lit ? LightTexture.FULL_BRIGHT : light;
        float top = FestoonBlock.top(state);

        var atlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        TextureAtlasSprite wool = atlas.getSprite(WOOL);
        TextureAtlasSprite iron = atlas.getSprite(IRON);
        TextureAtlasSprite cable = atlas.getSprite(CABLE);
        PoseStack.Pose pose = ps.last();

        mast(vc, pose, iron, top, light);
        lamp(vc, pose, wool, 0.5F, top - 0.16F, 0.5F, color, bulbLight);
        BlockPos origin = be.getBlockPos();
        for (BlockPos partner : be.links()) {
            if (!owns(origin, partner)) continue;
            BlockState other = level.getBlockState(partner);
            if (!(other.getBlock() instanceof FestoonBlock)) continue;
            float x1 = 0.5F + (partner.getX() - origin.getX());
            float z1 = 0.5F + (partner.getZ() - origin.getZ());
            span(vc, pose, wool, cable, 0.5F, top, 0.5F, x1, FestoonBlock.top(other), z1, color, bulbLight, light);
        }
    }

    /** The post with the smaller position draws the cable, so a span is never drawn twice. */
    private static boolean owns(BlockPos a, BlockPos b) {
        if (a.getX() != b.getX()) return a.getX() < b.getX();
        if (a.getZ() != b.getZ()) return a.getZ() < b.getZ();
        return a.getY() <= b.getY();
    }

    private static void mast(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite iron, float top, int light) {
        prism(vc, pose, iron, 0.18F, top - 0.20F, 0.07F, 0.05F, 8, STEEL, light);
        prism(vc, pose, iron, top - 0.20F, top - 0.06F, 0.062F, 0.062F, 8, STEEL_DARK, light);
        hoop(vc, pose, iron, top - 0.02F, 0.11F, 0.028F, STEEL_DARK, light);
        prism(vc, pose, iron, top - 0.02F, top + 0.06F, 0.028F, 0.02F, 6, STEEL, light);
    }

    private static void span(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite wool, TextureAtlasSprite cableTex,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             int color, int bulbLight, int wireLight) {
        float dx = x1 - x0;
        float dz = z1 - z0;
        float horiz = (float) Math.sqrt(dx * dx + dz * dz);
        float sag = Math.max(0.18F, horiz * 0.065F);
        int steps = Mth.clamp((int) (horiz * 5F), 6, 80);
        float sideX = horiz < 1e-3F ? 0.01F : -dz / horiz * 0.01F;
        float sideZ = horiz < 1e-3F ? 0.01F : dx / horiz * 0.01F;
        float walked = 0F;
        float next = 0.85F;
        float px = x0, py = y0, pz = z0;
        for (int i = 1; i <= steps; i++) {
            float t = i / (float) steps;
            float x = Mth.lerp(t, x0, x1);
            float y = Mth.lerp(t, y0, y1) - sag * 4F * t * (1F - t);
            float z = Mth.lerp(t, z0, z1);
            ribbon(vc, pose, cableTex, px, py, pz, x, y, z, sideX, sideZ, wireLight);
            float seg = (float) Math.sqrt((x - px) * (x - px) + (y - py) * (y - py) + (z - pz) * (z - pz));
            while (seg > 1e-4F && walked + seg >= next) {
                float u = (next - walked) / seg;
                lamp(vc, pose, wool, px + (x - px) * u, py + (y - py) * u - 0.08F, pz + (z - pz) * u, color, bulbLight);
                next += 0.85F;
            }
            walked += seg;
            px = x;
            py = y;
            pz = z;
        }
    }

    private static void prism(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite sprite,
                              float y0, float y1, float r0, float r1, int sides, int color, int light) {
        for (int i = 0; i < sides; i++) {
            float a0 = (float) (Math.PI * 2 * i / sides);
            float a1 = (float) (Math.PI * 2 * (i + 1) / sides);
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            quad(vc, pose, sprite,
                    0.5F + c0 * r0, y0, 0.5F + s0 * r0,
                    0.5F + c1 * r0, y0, 0.5F + s1 * r0,
                    0.5F + c1 * r1, y1, 0.5F + s1 * r1,
                    0.5F + c0 * r1, y1, 0.5F + s0 * r1,
                    color, light);
        }
    }

    /** A flat hoop with an outer lip, so the cable has something to leave from. */
    private static void hoop(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite sprite,
                             float y, float radius, float thick, int color, int light) {
        int n = 16;
        for (int i = 0; i < n; i++) {
            float a0 = (float) (Math.PI * 2 * i / n);
            float a1 = (float) (Math.PI * 2 * (i + 1) / n);
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            float inner = radius - thick;
            quad(vc, pose, sprite,
                    0.5F + c0 * inner, y, 0.5F + s0 * inner,
                    0.5F + c1 * inner, y, 0.5F + s1 * inner,
                    0.5F + c1 * radius, y, 0.5F + s1 * radius,
                    0.5F + c0 * radius, y, 0.5F + s0 * radius,
                    color, light);
            quad(vc, pose, sprite,
                    0.5F + c0 * radius, y, 0.5F + s0 * radius,
                    0.5F + c1 * radius, y, 0.5F + s1 * radius,
                    0.5F + c1 * radius, y - thick, 0.5F + s1 * radius,
                    0.5F + c0 * radius, y - thick, 0.5F + s0 * radius,
                    color, light);
        }
    }

    private static void ribbon(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite sprite,
                               float x0, float y0, float z0, float x1, float y1, float z1,
                               float sideX, float sideZ, int light) {
        quad(vc, pose, sprite,
                x0 - sideX, y0 + 0.008F, z0 - sideZ, x1 - sideX, y1 + 0.008F, z1 - sideZ,
                x1 + sideX, y1 - 0.008F, z1 + sideZ, x0 + sideX, y0 - 0.008F, z0 + sideZ, WIRE, light);
        quad(vc, pose, sprite,
                x0, y0 + 0.008F, z0, x1, y1 + 0.008F, z1,
                x1, y1 - 0.008F, z1, x0, y0 - 0.008F, z0, WIRE, light);
    }

    private static void lamp(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite sprite,
                             float x, float y, float z, int color, int light) {
        float r = LAMP;
        quad(vc, pose, sprite, x - r, y - r, z, x + r, y - r, z, x + r, y + r, z, x - r, y + r, z, color, light);
        quad(vc, pose, sprite, x, y - r, z - r, x, y - r, z + r, x, y + r, z + r, x, y + r, z - r, color, light);
        quad(vc, pose, sprite, x - r, y, z - r, x + r, y, z - r, x + r, y, z + r, x - r, y, z + r, color, light);
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite sprite,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3, int color, int light) {
        float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        int r = (color >> 16) & 255, g = (color >> 8) & 255, b = color & 255;
        vc.vertex(m, x0, y0, z0).color(r, g, b, 255).uv(u0, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0, 1, 0).endVertex();
        vc.vertex(m, x1, y1, z1).color(r, g, b, 255).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0, 1, 0).endVertex();
        vc.vertex(m, x2, y2, z2).color(r, g, b, 255).uv(u1, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0, 1, 0).endVertex();
        vc.vertex(m, x3, y3, z3).color(r, g, b, 255).uv(u0, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0, 1, 0).endVertex();
    }

    private static int pack(float r, float g, float b, float gain) {
        int ri = Math.min(255, Math.round(r * gain * 255F));
        int gi = Math.min(255, Math.round(g * gain * 255F));
        int bi = Math.min(255, Math.round(b * gain * 255F));
        return (ri << 16) | (gi << 8) | bi;
    }
}
