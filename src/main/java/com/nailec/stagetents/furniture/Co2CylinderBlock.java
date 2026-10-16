package com.nailec.stagetents.furniture;

import com.nailec.stagetents.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

/** Tall gas cylinder. The block above is only collision for the shoulder and the blue collar. */
public class Co2CylinderBlock extends MultiPropBlock implements EntityBlock {
    public Co2CylinderBlock(Properties props) {
        super(props, Spec.of(false, DyeColor.WHITE, new double[]{4.8, 0, 4.8, 11.2, 28.5, 11.2}),
                new int[][]{{0, 0}, {0, 1}});
    }

    @Override
    protected PropPartBlock.Form partForm(Direction facing) {
        return PropPartBlock.Form.POLE;
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
        if (level.getBlockEntity(pos) instanceof DraftBlockEntity be) DraftBlockItem.store(stack, be, true);
        return stack;
    }
}
