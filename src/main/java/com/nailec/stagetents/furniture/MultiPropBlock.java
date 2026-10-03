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

import java.util.ArrayList;
import java.util.List;

/**
 * Prop bigger than one block (the shooting gallery). The main block is the centre of the bottom row; invisible
 * {@link PropPartBlock}s fill the rest of the footprint. Placement is refused when the footprint isn't free.
 */
public class MultiPropBlock extends FurnitureBlock {
    /** Footprint in model space (facing north): {dx along +x, dy}, the main block being {0, 0}. */
    private final int[][] cells;
    /** Whether the extra cells are thin panels (crowd barriers) instead of full blocks. */
    private final boolean panels;

    public MultiPropBlock(Properties props, Spec spec, int[][] cells) {
        this(props, spec, cells, false);
    }

    public MultiPropBlock(Properties props, Spec spec, int[][] cells, boolean panels) {
        super(props, spec);
        this.cells = cells;
        this.panels = panels;
    }

    private BlockState partState(Direction facing) {
        BlockState part = ModRegistry.PROP_PART.get().defaultBlockState();
        if (!panels) return part;
        // The panel runs across the facing, like the barrier itself.
        return part.setValue(PropPartBlock.FORM, facing.getAxis() == Direction.Axis.Z ? PropPartBlock.Form.PANEL_X : PropPartBlock.Form.PANEL_Z);
    }

    /** World positions of the extra cells for a main block at {@code pos} facing {@code facing}. */
    public List<BlockPos> parts(BlockPos pos, Direction facing) {
        List<BlockPos> out = new ArrayList<>();
        Direction right = facing.getClockWise();
        for (int[] c : cells) {
            if (c[0] == 0 && c[1] == 0) continue;
            out.add(pos.relative(right, c[0]).above(c[1]));
        }
        return out;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState s = super.getStateForPlacement(ctx);
        if (s == null) return null;
        Level level = ctx.getLevel();
        for (BlockPos p : parts(ctx.getClickedPos(), s.getValue(FACING))) {
            if (!level.getBlockState(p).canBeReplaced(ctx) || level.isOutsideBuildHeight(p)) return null;
        }
        return s;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) return;
        BlockState part = partState(state.getValue(FACING));
        for (BlockPos p : parts(pos, state.getValue(FACING))) level.setBlock(p, part, Block.UPDATE_ALL);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            for (BlockPos p : parts(pos, state.getValue(FACING))) {
                if (level.getBlockState(p).is(ModRegistry.PROP_PART.get())) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
