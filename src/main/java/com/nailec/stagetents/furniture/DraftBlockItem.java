package com.nailec.stagetents.furniture;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.List;

/** Keg or cylinder item. A missing tag means a full container straight from the crafting table. */
public class DraftBlockItem extends BlockItem {
    private final boolean gas;

    public DraftBlockItem(Block block, Properties props, boolean gas) {
        super(block, props);
        this.gas = gas;
    }

    static void store(ItemStack stack, DraftBlockEntity be, boolean gas) {
        CompoundTag tag = new CompoundTag();
        if (gas) tag.putInt("Gas", be.gas());
        else tag.putInt("Beer", be.beer());
        stack.addTagElement("BlockEntityTag", tag);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTagElement("BlockEntityTag");
        String key = gas ? "Gas" : "Beer";
        int max = gas ? DraftBeer.CYLINDER_SERVINGS : DraftBeer.KEG_SERVINGS;
        int value = tag != null && tag.contains(key) ? tag.getInt(key) : max;
        tooltip.add(Component.translatable(gas ? "tooltip.stagetents.cylinder" : "tooltip.stagetents.keg", value, max)
                .withStyle(ChatFormatting.GRAY));
    }
}
