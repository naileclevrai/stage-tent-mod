package com.nailec.stagetents.furniture;

import com.nailec.stagetents.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Separate keg. Breaking it keeps the remaining servings on the item. */
public class BeerKegBlock extends FurnitureBlock implements EntityBlock {
    public BeerKegBlock(Properties props) {
        super(props, Spec.of(false, DyeColor.WHITE, new double[]{2.2, 0, 2.2, 13.8, 15.0, 13.8}));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DraftBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModRegistry.DRAFT_BE.get()) return null;
        return (l, p, s, be) -> {
            if ((l.getGameTime() + p.asLong()) % 40 == 0) ((DraftBlockEntity) be).prune();
        };
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof DraftBlockEntity be) {
            be.detach();
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && (player.getItemInHand(hand).isEmpty() || player.isShiftKeyDown())) {
            DraftBeer.status(level, pos, player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        ItemStack stack = super.getCloneItemStack(level, pos, state);
        if (level.getBlockEntity(pos) instanceof DraftBlockEntity be) DraftBlockItem.store(stack, be, false);
        return stack;
    }
}
