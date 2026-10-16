package com.nailec.stagetents.client;

import com.nailec.stagetents.StageTents;
import com.nailec.stagetents.furniture.BeerHoseItem;
import com.nailec.stagetents.furniture.DraftBeer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/** The hose being laid, so each pin is visible before the far end is chosen. */
@Mod.EventBusSubscriber(modid = StageTents.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DraftHosePreview {
    private static final ResourceLocation WOOL = new ResourceLocation("minecraft", "block/white_wool");

    private DraftHosePreview() {}

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        ItemStack hose = mc.player.getMainHandItem();
        if (!(hose.getItem() instanceof BeerHoseItem)) hose = mc.player.getOffhandItem();
        if (!(hose.getItem() instanceof BeerHoseItem)) return;
        CompoundTag tag = hose.getTag();
        if (tag == null || !tag.contains(DraftBeer.LINK_KEY)) return;
        BlockPos from = BlockPos.of(tag.getLong(DraftBeer.LINK_KEY));
        if (!mc.level.isLoaded(from) || DraftBeer.kind(mc.level.getBlockState(from)) == null) return;

        Vec3 cam = event.getCamera().getPosition();
        List<Vec3> points = new ArrayList<>();
        points.add(DraftBeer.looseAnchor(mc.level, from).subtract(cam));
        for (Vec3 pin : DraftBeer.pins(hose)) points.add(pin.subtract(cam));
        HitResult hit = mc.hitResult;
        if (hit instanceof BlockHitResult block && block.getType() == HitResult.Type.BLOCK) {
            Direction face = block.getDirection();
            Vec3 loose = block.getLocation().add(face.getStepX() * 0.04, face.getStepY() * 0.04, face.getStepZ() * 0.04);
            if (points.get(points.size() - 1).distanceToSqr(loose.subtract(cam)) > 0.05) points.add(loose.subtract(cam));
        }
        if (points.size() < 2) return;

        TextureAtlasSprite wool = mc.getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(WOOL);
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        RenderType type = RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS);
        DraftHoseRenderer.tube(buffers.getBuffer(type), event.getPoseStack().last(), wool, points, color(mc.level, from), 0.015F, 0x00F000F0);
        buffers.endBatch(type);
    }

    private static int color(Level level, BlockPos pos) {
        DraftBeer.Kind kind = DraftBeer.kind(level.getBlockState(pos));
        if (kind == DraftBeer.Kind.CYLINDER) return 0xFF1A4E86;
        if (kind == DraftBeer.Kind.TAP) return 0xFF6B3A16;
        return 0xFF3C342C;
    }
}
