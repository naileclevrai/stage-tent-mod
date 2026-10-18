package com.nailec.stagetents.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nailec.stagetents.furniture.PowerNodeBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** The leads leaving a generator, a distro or a rack. The block model draws the case. */
public class PowerNodeRenderer implements BlockEntityRenderer<PowerNodeBlockEntity> {
    public PowerNodeRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(PowerNodeBlockEntity be, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        PowerCableClient.drawCables(be, ps, buffers);
    }

    @Override
    public boolean shouldRenderOffScreen(PowerNodeBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
