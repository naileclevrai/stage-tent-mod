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

/** Marks one socket, then a compatible one. Sneak on a socket that already has a lead removes it. */
public class CableToolItem extends Item {
    public CableToolItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        return PowerCable.interact(ctx.getLevel(), player, ctx.getItemInHand(), ctx.getClickLocation(), ctx.getClickedPos());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        InteractionResult result = PowerCable.interact(level, player, stack, null, null);
        if (result == InteractionResult.PASS) return InteractionResultHolder.pass(stack);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return PowerCable.armed(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.stagetents.cable_tool.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.stagetents.cable_tool.2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.stagetents.cable_tool.3").withStyle(ChatFormatting.GRAY));
        if (PowerCable.armed(stack)) {
            tooltip.add(Component.translatable("tooltip.stagetents.cable_tool.armed").withStyle(ChatFormatting.GOLD));
        }
    }
}
