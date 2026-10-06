package com.nailec.stagetents.client;

import com.nailec.stagetents.ModRegistry;
import com.nailec.stagetents.StageTents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
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
        event.registerBlockEntityRenderer(ModRegistry.DRAPE_BE.get(), DrapeRenderer::new);
    }

    /** Furniture: tint index 0 is the dyed part (tablecloth, cushion, front panel). */
    @SubscribeEvent
    public static void blockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> tint == 0 ? dyeTint(state) : -1,
                ModRegistry.ROUND_TABLE.get(), ModRegistry.STANDING_TABLE.get(), ModRegistry.BANQUET_CHAIR.get(),
                ModRegistry.BAR_COUNTER.get(), ModRegistry.BLEACHER.get(), ModRegistry.FOLDING_CHAIR.get(),
                ModRegistry.BAR_STOOL.get(), ModRegistry.STANCHION.get(), ModRegistry.BLEACHER_AISLE.get(),
                ModRegistry.STAGE_DECK.get(), ModRegistry.STAGE_STAIRS.get(), ModRegistry.STAGE_RAMP.get(),
                ModRegistry.CYCLORAMA.get(), ModRegistry.FLIGHT_CASE.get(), ModRegistry.FLIGHT_CASE_TRUNK.get(),
                ModRegistry.FLIGHT_CASE_TALL.get(), ModRegistry.FLIGHT_CASE_XL.get(), ModRegistry.GENERATOR.get(),
                ModRegistry.SITE_TOILET.get(), ModRegistry.SITE_FENCE.get(), ModRegistry.ORIFLAMME.get());
    }

    /** Pixel-art icons of the stage and site props: layer 1 is the dyed part, shown in the block's default colour. */
    @SubscribeEvent
    public static void itemColors(RegisterColorHandlersEvent.Item event) {
        Item[] items = {ModRegistry.STAGE_DECK_ITEM.get(), ModRegistry.STAGE_STAIRS_ITEM.get(), ModRegistry.STAGE_RAMP_ITEM.get(),
                ModRegistry.CYCLORAMA_ITEM.get(), ModRegistry.FLIGHT_CASE_ITEM.get(), ModRegistry.FLIGHT_CASE_TRUNK_ITEM.get(),
                ModRegistry.FLIGHT_CASE_TALL_ITEM.get(), ModRegistry.FLIGHT_CASE_XL_ITEM.get(), ModRegistry.GENERATOR_ITEM.get(),
                ModRegistry.SITE_TOILET_ITEM.get(), ModRegistry.SITE_FENCE_ITEM.get(), ModRegistry.ORIFLAMME_ITEM.get(),
                ModRegistry.FRISE_ITEM.get(), ModRegistry.PENDRILLON_ITEM.get()};
        event.register((stack, tint) -> {
            if (tint != 1 || !(stack.getItem() instanceof BlockItem block)) return -1;
            return dyeTint(block.getBlock().defaultBlockState());
        }, items);
    }

    private static int dyeTint(BlockState state) {
        DyeColor c = state.getValue(FurnitureBlock.COLOR);
        float[] f = c.getTextureDiffuseColors();
        return ((int) (f[0] * 255) << 16) | ((int) (f[1] * 255) << 8) | (int) (f[2] * 255);
    }
}
