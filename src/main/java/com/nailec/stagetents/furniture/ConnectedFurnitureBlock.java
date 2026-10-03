package com.nailec.stagetents.furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Furniture built in runs (bar counters, bleacher rows): it knows whether the same block, facing the same way, sits
 * on its left and right, so only the ends of a run get their end panels and rails.
 */
public class ConnectedFurnitureBlock extends FurnitureBlock {
    /** Neighbour on the model's -x side (counter-clockwise of the facing). */
    public static final BooleanProperty LEFT = BooleanProperty.create("left");
    /** Neighbour on the model's +x side (clockwise of the facing). */
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");

    /** Blocks of the same group continue each other's run (bleacher seats and aisles form one grandstand). */
    public final String group;

    public ConnectedFurnitureBlock(Properties props, Spec spec, String group) {
        super(props, spec);
        this.group = group;
        registerDefaultState(defaultBlockState().setValue(LEFT, false).setValue(RIGHT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LEFT, RIGHT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState s = super.getStateForPlacement(ctx);
        return s == null ? null : connect(s, ctx.getLevel(), ctx.getClickedPos());
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return dir.getAxis().isHorizontal() ? connect(state, level, pos) : state;
    }

    private BlockState connect(BlockState state, BlockGetter level, BlockPos pos) {
        Direction f = state.getValue(FACING);
        return state.setValue(LEFT, sameRun(level.getBlockState(pos.relative(f.getCounterClockWise())), f))
                .setValue(RIGHT, sameRun(level.getBlockState(pos.relative(f.getClockWise())), f));
    }

    private boolean sameRun(BlockState other, Direction facing) {
        return other.getBlock() instanceof ConnectedFurnitureBlock c && c.group.equals(group) && other.getValue(FACING) == facing;
    }
}
