package com.nailec.stagetents.furniture;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Marks one port, then the matching one. A beer line joins keg and tap. A gas line joins cylinder and keg. */
public class BeerHoseItem extends Item {
    public BeerHoseItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        ItemStack hose = ctx.getItemInHand();
        boolean marked = hose.getTag() != null && hose.getTag().contains(DraftBeer.LINK_KEY);
        if (DraftBeer.kind(level.getBlockState(ctx.getClickedPos())) != null) {
            if (!level.isClientSide && ctx.getPlayer() != null) {
                DraftBeer.link(level, ctx.getClickedPos(), ctx.getPlayer(), hose);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!marked) return InteractionResult.PASS;
        if (!level.isClientSide && ctx.getPlayer() != null) DraftBeer.route(level, ctx, ctx.getPlayer(), hose);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack hose = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && hose.getTag() != null && hose.getTag().contains(DraftBeer.LINK_KEY)) {
            if (!level.isClientSide) DraftBeer.unpin(player, hose);
            return InteractionResultHolder.sidedSuccess(hose, level.isClientSide);
        }
        return InteractionResultHolder.pass(hose);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().contains(DraftBeer.LINK_KEY);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.stagetents.hose").withStyle(ChatFormatting.GRAY));
        if (isFoil(stack)) tooltip.add(Component.translatable("tooltip.stagetents.hose_marked").withStyle(ChatFormatting.GOLD));
    }
}
