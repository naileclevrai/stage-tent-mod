package com.nailec.stagetents.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nailec.stagetents.StageTents;
import com.nailec.stagetents.furniture.FurnitureBlock;
import com.nailec.stagetents.furniture.WaterCannonBlock;
import com.nailec.stagetents.furniture.WaterCannonBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/** Draws the barrel, ram and hose, pitched by the block and panned around the yoke. */
public class WaterCannonRenderer implements BlockEntityRenderer<WaterCannonBlockEntity> {
    public WaterCannonRenderer(BlockEntityRendererProvider.Context ctx) {}

    public static ResourceLocation barrelModel(int pitch) {
        return new ResourceLocation(StageTents.MOD_ID, "block/props/water_cannon_barrel_" + pitch);
    }

    @Override
    public void render(WaterCannonBlockEntity be, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof WaterCannonBlock)) return;
        int pitch = state.getValue(WaterCannonBlock.PITCH);
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(barrelModel(pitch));
        Direction facing = state.getValue(FurnitureBlock.FACING);

        ps.pushPose();
        // Same facing turn as the skid's blockstate, then pan in that model space.
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(-facing.toYRot() + 180));
        ps.translate(-0.5, 0, -0.5);
        ps.translate(0.5, 1.08, 0.37);
        ps.mulPose(Axis.YP.rotationDegrees(-be.yaw()));
        ps.translate(-0.5, -1.08, -0.37);

        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
                ps.last(), buffers.getBuffer(RenderType.cutout()), state, model,
                1.0F, 1.0F, 1.0F, light, OverlayTexture.NO_OVERLAY);
        ps.popPose();
    }
}
