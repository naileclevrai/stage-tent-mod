package com.nailec.stagetents.block;

import com.nailec.stagetents.tent.TentParams;
import com.nailec.stagetents.tent.TentType;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Tent plate item. Remembers the tent settings when it was broken or pick-blocked. */
public class TentBlockItem extends BlockItem {
    private final TentType type;

    public TentBlockItem(TentBlock block, Properties props) {
        super(block, props);
        this.type = block.type;
    }

    public static void storeParams(ItemStack stack, TentParams params) {
        stack.getOrCreateTagElement(BLOCK_ENTITY_TAG).put("Tent", params.save());
    }

    @Nullable
    public static CompoundTag storedParams(ItemStack stack) {
        CompoundTag be = stack.getTagElement(BLOCK_ENTITY_TAG);
        return be != null && be.contains("Tent") ? be.getCompound("Tent") : null;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = storedParams(stack);
        TentParams p = tag != null ? TentParams.load(tag, type) : type.defaults();
        int len = type == TentType.FRAME || type == TentType.DJ_ARCH ? p.length : p.width + p.length;
        tooltip.add(Component.translatable("tooltip.stagetents.size", p.width, len, p.height).withStyle(ChatFormatting.GRAY));
        if (tag == null) {
            tooltip.add(Component.translatable("tooltip.stagetents.hint").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
