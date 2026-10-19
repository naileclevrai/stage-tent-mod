package com.nailec.stagetents.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nailec.stagetents.furniture.MachineBlock;
import com.nailec.stagetents.furniture.MachineKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Draws the machine items as a small copy of the machine, fitted into one block. */
public class MachineItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static MachineItemRenderer instance;

    private MachineItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static MachineItemRenderer instance() {
        if (instance == null) instance = new MachineItemRenderer();
        return instance;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        if (!(stack.getItem() instanceof BlockItem item) || !(item.getBlock() instanceof MachineBlock block)) return;
        MachineKind kind = block.kind;
        // Model bounds: x0, x1, z0, z1, height.
        double[] bounds = switch (kind) {
            case FORKLIFT -> new double[]{-0.08, 1.08, 0.0, MachineKind.FL_REAR, MachineKind.FL_GUARD_TOP};
            case SCISSOR -> new double[]{-0.11, 1.11, 0.0, MachineKind.SL08_LENGTH + 0.09, 2.10};
            case RT_SCISSOR -> new double[]{-0.15, 2.15, 0.0, 3.90, 2.95};
        };
        double cx = (bounds[0] + bounds[1]) * 0.5, cz = (bounds[2] + bounds[3]) * 0.5;
        double size = Math.max(Math.max(bounds[1] - bounds[0], bounds[3] - bounds[2]), bounds[4]);
        float scale = (float) (1.0 / size);
        ps.pushPose();
        ps.translate(0.5, 0.5 - bounds[4] * scale * 0.5, 0.5);
        ps.scale(scale, scale, scale);
        ps.translate(-cx, 0, -cz);
        MachineRenderer.Pose pose = new MachineRenderer.Pose(kind, kind.color, 0, 0, kind == MachineKind.FORKLIFT ? 1 : 0, 0, 0);
        MachineRenderer.draw(pose, ps, buffers, dy -> light, false);
        ps.popPose();
    }
}
