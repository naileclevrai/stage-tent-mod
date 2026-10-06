package com.nailec.stagetents.block;

import com.nailec.stagetents.client.ClientHooks;
import com.nailec.stagetents.tent.TentParams;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Rigging wrench.
 * <ul>
 *     <li>Sneak + use on a tent plate: copy its settings.</li>
 *     <li>Use on a tent plate: paste the copied settings (full copy for the same kind of tent, style only otherwise),
 *     or open the settings when nothing is copied.</li>
 *     <li>Use in the air: open the settings of the tent you are standing in.</li>
 *     <li>Sneak + use in the air: forget the copied settings.</li>
 * </ul>
 */
public class TentWrenchItem extends Item {
    private static final String KEY = "CopiedTent";

    public TentWrenchItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        // On a turnstile: sneak opens the settings, a plain click cycles the mode.
        if (level.getBlockState(ctx.getClickedPos()).getBlock() instanceof com.nailec.stagetents.furniture.TurnstileBlock) {
            Player player = ctx.getPlayer();
            if (player != null && player.isShiftKeyDown()) {
                if (level.isClientSide) {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openTurnstileScreen(ctx.getClickedPos()));
                }
            } else if (!level.isClientSide && player != null) {
                com.nailec.stagetents.furniture.TurnstileBlock.cycleMode(level, ctx.getClickedPos(), level.getBlockState(ctx.getClickedPos()), player);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        // On a stage deck: next height.
        if (level.getBlockState(ctx.getClickedPos()).getBlock() instanceof com.nailec.stagetents.furniture.StageDeckBlock) {
            if (!level.isClientSide && ctx.getPlayer() != null) {
                com.nailec.stagetents.furniture.StageDeckBlock.cycleHeight(level, ctx.getClickedPos(), level.getBlockState(ctx.getClickedPos()), ctx.getPlayer());
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.getBlockState(ctx.getClickedPos()).getBlock() instanceof com.nailec.stagetents.furniture.WaterCannonBlock) {
            Player player = ctx.getPlayer();
            if (player != null && player.isShiftKeyDown() && level.isClientSide) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openCannonScreen(ctx.getClickedPos()));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.getBlockState(ctx.getClickedPos()).getBlock() instanceof com.nailec.stagetents.furniture.CableRampBlock ramp) {
            Player player = ctx.getPlayer();
            if (player != null && player.isShiftKeyDown()) {
                if (level.isClientSide) {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openCableRampScreen(ctx.getClickedPos()));
                }
            } else if (!level.isClientSide && player != null) {
                ramp.toggle(level, ctx.getClickedPos(), level.getBlockState(ctx.getClickedPos()));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.getBlockState(ctx.getClickedPos()).getBlock() instanceof com.nailec.stagetents.furniture.FriseBlock
                || level.getBlockState(ctx.getClickedPos()).getBlock() instanceof com.nailec.stagetents.furniture.PendrillonBlock) {
            if (level.isClientSide) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openDrapeScreen(ctx.getClickedPos()));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        // On a bleacher: open the grandstand builder.
        if (level.getBlockState(ctx.getClickedPos()).getBlock() instanceof com.nailec.stagetents.furniture.ConnectedFurnitureBlock c
                && c.group.equals("grandstand")) {
            if (level.isClientSide) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openGrandstandScreen(ctx.getClickedPos()));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!(level.getBlockEntity(ctx.getClickedPos()) instanceof TentBlockEntity be)) return InteractionResult.PASS;
        Player player = ctx.getPlayer();
        ItemStack stack = ctx.getItemInHand();
        CompoundTag copied = stack.getTagElement(KEY);
        if (player != null && player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                stack.getOrCreateTag().put(KEY, be.params().save());
                player.displayClientMessage(Component.translatable("message.stagetents.copied"), true);
            }
        } else if (copied != null) {
            if (!level.isClientSide) {
                TentParams src = TentParams.load(copied, TentParams.typeOf(copied, be.type()));
                TentParams dst;
                if (src.type == be.type()) {
                    dst = src;
                } else {
                    dst = be.params().copy();
                    dst.copyStyle(src);
                }
                be.applyParams(dst);
                if (player != null) player.displayClientMessage(Component.translatable("message.stagetents.pasted"), true);
            }
        } else if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openTentScreen(ctx.getClickedPos()));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide && stack.getTagElement(KEY) != null) {
                stack.removeTagKey(KEY);
                player.displayClientMessage(Component.translatable("message.stagetents.cleared"), true);
            }
        } else if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientHooks::openTentScreenAround);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.getTagElement(KEY) != null;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (stack.getTagElement(KEY) != null) {
            tooltip.add(Component.translatable("tooltip.stagetents.wrench.copied").withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(Component.translatable("tooltip.stagetents.wrench.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.stagetents.wrench.2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.stagetents.wrench.3").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.stagetents.wrench.4").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.stagetents.wrench.5").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.stagetents.wrench.6").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.stagetents.wrench.7").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.stagetents.wrench.8").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.stagetents.wrench.9").withStyle(ChatFormatting.GRAY));
    }
}
