package com.nailec.stagetents.furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/**
 * Stage deck in runs, like the bar. {@link #HEIGHT} is the height in quarter-blocks (25 cm to 1 m). The rigging
 * wrench cycles it. A dyed skirt closes the front and the ends of a run.
 */
public class StageDeckBlock extends ConnectedFurnitureBlock {
    public static final IntegerProperty HEIGHT = IntegerProperty.create("height", 1, 4);

    private final Map<Direction, VoxelShape>[] shapes = new Map[5];

    public StageDeckBlock(Properties props) {
        super(props, Spec.of(true, DyeColor.BLACK, new double[]{0, 0, 0, 16, 16, 16}), "stage");
        for (int h = 1; h <= 4; h++) {
            Map<Direction, VoxelShape> byFacing = new EnumMap<>(Direction.class);
            double[] box = {0, 0, 0, 16, h * 4, 16};
            for (Direction d : Direction.Plane.HORIZONTAL) byFacing.put(d, rotated(box, d));
            shapes[h] = byFacing;
        }
        registerDefaultState(defaultBlockState().setValue(HEIGHT, 4));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HEIGHT);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapes[state.getValue(HEIGHT)].get(state.getValue(FACING));
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return getShape(state, level, pos, ctx);
    }

    /** Wrench: next height, 1 m wrapping back to 25 cm. */
    public static void cycleHeight(Level level, BlockPos pos, BlockState state, Player player) {
        int next = state.getValue(HEIGHT) % 4 + 1;
        level.setBlock(pos, state.setValue(HEIGHT, next), Block.UPDATE_ALL);
        player.displayClientMessage(Component.translatable("message.stagetents.stage_height",
                Component.translatable("screen.stagetents.stage_height." + next)), true);
    }
}
