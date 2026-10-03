package com.nailec.stagetents.furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Queue post. A velvet rope runs east and south to the next post, whether it stands right next to it or one block
 * further (with nothing solid in between). Each post only draws its east and south ropes, so a rope is never drawn
 * twice.
 */
public class StanchionBlock extends FurnitureBlock {
    /** Distance in blocks to the post the rope goes to, 0 for no rope. */
    public static final IntegerProperty EAST = IntegerProperty.create("east", 0, 2);
    public static final IntegerProperty SOUTH = IntegerProperty.create("south", 0, 2);

    public StanchionBlock(Properties props, Spec spec) {
        super(props, spec);
        registerDefaultState(defaultBlockState().setValue(EAST, 0).setValue(SOUTH, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(EAST, SOUTH);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return ropes(defaultBlockState(), ctx.getLevel(), ctx.getClickedPos());
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return ropes(state, level, pos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        refreshFar(level, pos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        super.onRemove(state, level, pos, newState, moving);
        if (!newState.is(this)) refreshFar(level, pos);
    }

    /** Posts two blocks west or north don't get a neighbour update; tell them about this one. */
    private void refreshFar(Level level, BlockPos pos) {
        if (level.isClientSide) return;
        for (Direction d : new Direction[]{Direction.WEST, Direction.NORTH}) {
            BlockPos far = pos.relative(d, 2);
            BlockState s = level.getBlockState(far);
            if (s.is(this)) {
                BlockState updated = ropes(s, level, far);
                if (updated != s) level.setBlock(far, updated, Block.UPDATE_ALL);
            }
        }
    }

    private BlockState ropes(BlockState state, BlockGetter level, BlockPos pos) {
        return state.setValue(EAST, reach(level, pos, Direction.EAST)).setValue(SOUTH, reach(level, pos, Direction.SOUTH));
    }

    private int reach(BlockGetter level, BlockPos pos, Direction d) {
        if (level.getBlockState(pos.relative(d)).is(this)) return 1;
        BlockState between = level.getBlockState(pos.relative(d));
        if (between.getCollisionShape(level, pos.relative(d)).isEmpty() && level.getBlockState(pos.relative(d, 2)).is(this)) return 2;
        return 0;
    }
}
