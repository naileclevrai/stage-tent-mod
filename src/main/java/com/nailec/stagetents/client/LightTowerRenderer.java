package com.nailec.stagetents.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nailec.stagetents.StageTents;
import com.nailec.stagetents.furniture.FurnitureBlock;
import com.nailec.stagetents.furniture.LightTowerBlock;
import com.nailec.stagetents.furniture.LightTowerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Telescopic mast and the four floods. The trailer itself is the block model. */
public class LightTowerRenderer implements BlockEntityRenderer<LightTowerBlockEntity> {
    /** Lamp offset from the yoke, and a few degrees of spread so the outer floods are not parallel. */
    private static final float[][] LAMPS = {
            {-0.22F, 0.11F, 8F},
            {0.22F, 0.11F, -8F},
            {-0.22F, -0.13F, 8F},
            {0.22F, -0.13F, -8F},
    };

    public LightTowerRenderer(BlockEntityRendererProvider.Context ctx) {}

    public static ResourceLocation sectionModel() {
        return new ResourceLocation(StageTents.MOD_ID, "block/props/light_tower_section");
    }

    public static ResourceLocation yokeModel() {
        return new ResourceLocation(StageTents.MOD_ID, "block/props/light_tower_yoke");
    }

    public static ResourceLocation lampModel() {
        return new ResourceLocation(StageTents.MOD_ID, "block/props/light_tower_lamp");
    }

    @Override
    public void render(LightTowerBlockEntity be, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof LightTowerBlock)) return;
        Level level = be.getLevel();
        int height = state.getValue(LightTowerBlock.HEIGHT);
        Direction facing = state.getValue(FurnitureBlock.FACING);
        boolean running = state.getValue(LightTowerBlock.RUNNING);
        var renderer = Minecraft.getInstance().getBlockRenderer().getModelRenderer();
        BakedModel section = Minecraft.getInstance().getModelManager().getModel(sectionModel());
        BakedModel yoke = Minecraft.getInstance().getModelManager().getModel(yokeModel());
        BakedModel lamp = Minecraft.getInstance().getModelManager().getModel(lampModel());
        var buffer = buffers.getBuffer(RenderType.cutout());

        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(-facing.toYRot() + 180));
        ps.translate(-0.5, 0, -0.5);

        float top = height;
        float span = Math.max(0.45F, top - LightTowerBlock.MAST_BASE);
        int count = Math.max(1, Math.round(span));
        float step = span / count;
        BlockPos origin = be.getBlockPos();
        for (int i = 0; i < count; i++) {
            float y = LightTowerBlock.MAST_BASE + i * step;
            float seg = step + 0.16F;
            float scale = Math.max(0.55F, 1F - i * 0.07F);
            int lit = level == null ? light : LevelRenderer.getLightColor(level, origin.above((int) y));
            ps.pushPose();
            ps.translate(LightTowerBlock.MAST_X, y, LightTowerBlock.MAST_Z);
            ps.scale(scale, seg, scale);
            ps.translate(-0.5, 0, -0.5);
            renderer.renderModel(ps.last(), buffer, state, section, 1F, 1F, 1F, lit, OverlayTexture.NO_OVERLAY);
            ps.popPose();
        }

        int headLight = level == null ? light : LevelRenderer.getLightColor(level, origin.above(height));
        int lampLight = running ? 0xF000F0 : headLight;
        ps.pushPose();
        ps.translate(LightTowerBlock.MAST_X, top, LightTowerBlock.MAST_Z);
        ps.mulPose(Axis.YP.rotationDegrees(-be.yaw()));
        ps.pushPose();
        ps.translate(-0.5, 0, -0.5);
        renderer.renderModel(ps.last(), buffer, state, yoke, 1F, 1F, 1F, headLight, OverlayTexture.NO_OVERLAY);
        ps.popPose();
        for (float[] placed : LAMPS) {
            ps.pushPose();
            ps.translate(placed[0], placed[1], -0.04);
            ps.mulPose(Axis.YP.rotationDegrees(placed[2]));
            ps.mulPose(Axis.XP.rotationDegrees(-be.tilt()));
            ps.translate(-0.5F, 0, -0.5F);
            renderer.renderModel(ps.last(), buffer, state, lamp, 1F, 1F, 1F, lampLight, OverlayTexture.NO_OVERLAY);
            ps.popPose();
        }
        ps.popPose();
        ps.popPose();
    }
}
