package com.nailec.stagetents.furniture;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/** Empty mug, or a filled one that goes back to an empty mug after drinking. */
public class BeerMugItem extends Item {
    private final boolean filled;

    public BeerMugItem(Properties props, boolean filled) {
        super(filled ? props.food(new FoodProperties.Builder().nutrition(6).saturationMod(0.6F).alwaysEat().build()) : props);
        this.filled = filled;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return filled ? UseAnim.DRINK : UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return filled ? 32 : 0;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!filled) return InteractionResultHolder.pass(player.getItemInHand(hand));
        return super.use(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack left = super.finishUsingItem(stack, level, entity);
        if (!filled) return left;
        ItemStack mug = new ItemStack(com.nailec.stagetents.ModRegistry.BEER_MUG.get());
        if (entity instanceof Player player && !player.getAbilities().instabuild) {
            if (left.isEmpty()) return mug;
            if (!player.getInventory().add(mug)) player.drop(mug, false);
        }
        return left;
    }
}
