package com.nailec.stagetents.client;

import com.nailec.stagetents.ModRegistry;
import com.nailec.stagetents.StageTents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import com.nailec.stagetents.furniture.FurnitureBlock;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = StageTents.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModRegistry.TENT_BE.get(), TentRenderer::new);
        event.registerEntityRenderer(ModRegistry.SEAT.get(), NoopRenderer::new);
        event.registerBlockEntityRenderer(ModRegistry.TURNSTILE_BE.get(), TurnstileRenderer::new);
    }

    /** Furniture: tint index 0 is the dyed part (tablecloth, cushion, front panel). */
    @SubscribeEvent
    public static void blockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> tint == 0 ? dyeTint(state) : -1,
                ModRegistry.ROUND_TABLE.get(), ModRegistry.STANDING_TABLE.get(), ModRegistry.BANQUET_CHAIR.get(),
                ModRegistry.BAR_COUNTER.get(), ModRegistry.BLEACHER.get(), ModRegistry.FOLDING_CHAIR.get(),
                ModRegistry.BAR_STOOL.get(), ModRegistry.STANCHION.get(), ModRegistry.BLEACHER_AISLE.get());
    }

    private static int dyeTint(BlockState state) {
        DyeColor c = state.getValue(FurnitureBlock.COLOR);
        float[] f = c.getTextureDiffuseColors();
        return ((int) (f[0] * 255) << 16) | ((int) (f[1] * 255) << 8) | (int) (f[2] * 255);
    }
}
