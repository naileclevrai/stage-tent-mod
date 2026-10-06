package com.nailec.stagetents.furniture;

import com.nailec.stagetents.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A connected run that also occupies the blocks above it (site fence, cyclorama). The extra cells are invisible
 * walls; the mesh of the main block draws the whole height.
 */
public class TallRunBlock extends ConnectedFurnitureBlock {
    private final int above;

    public TallRunBlock(Properties props, Spec spec, String group, int above) {
        super(props, spec, group);
        this.above = above;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState s = super.getStateForPlacement(ctx);
        if (s == null) return null;
        Level level = ctx.getLevel();
        for (int i = 1; i <= above; i++) {
            BlockPos p = ctx.getClickedPos().above(i);
            if (!level.getBlockState(p).canBeReplaced(ctx) || level.isOutsideBuildHeight(p)) return null;
        }
        return s;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) return;
        BlockState part = wall(state.getValue(FACING));
        for (int i = 1; i <= above; i++) level.setBlock(pos.above(i), part, Block.UPDATE_ALL);
    }

    private static BlockState wall(Direction facing) {
        PropPartBlock.Form form = facing.getAxis() == Direction.Axis.Z ? PropPartBlock.Form.WALL_X : PropPartBlock.Form.WALL_Z;
        return ModRegistry.PROP_PART.get().defaultBlockState().setValue(PropPartBlock.FORM, form);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            for (int i = 1; i <= above; i++) {
                BlockPos p = pos.above(i);
                if (level.getBlockState(p).is(ModRegistry.PROP_PART.get())) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
