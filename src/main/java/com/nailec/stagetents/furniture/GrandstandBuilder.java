package com.nailec.stagetents.furniture;

import com.nailec.stagetents.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a stepped grandstand from one bleacher: rows climb one block up and one block back, aisles cut through at a
 * regular interval, and aluminium supports fill the space under the raised rows. Only replaceable blocks (air,
 * grass, ...) are touched.
 */
public final class GrandstandBuilder {
    public static final int MAX_ROWS = 16, MAX_WIDTH = 32, MAX_AISLE = 16;

    private GrandstandBuilder() {}

    /**
     * @param origin front-left seat of the grandstand (the bleacher the wrench was used on)
     * @param aisle  seats between two aisles, 0 for no aisle
     * @return number of blocks placed
     */
    public static int build(Level level, BlockPos origin, Direction facing, int rows, int width, int aisle, DyeColor color) {
        rows = Mth.clamp(rows, 1, MAX_ROWS);
        width = Mth.clamp(width, 1, MAX_WIDTH);
        aisle = Mth.clamp(aisle, 0, MAX_AISLE);
        Direction right = facing.getClockWise(), back = facing.getOpposite();
        BlockState seat = ModRegistry.BLEACHER.get().defaultBlockState().setValue(FurnitureBlock.FACING, facing).setValue(FurnitureBlock.COLOR, color);
        BlockState stairs = ModRegistry.BLEACHER_AISLE.get().defaultBlockState().setValue(FurnitureBlock.FACING, facing);
        BlockState support = ModRegistry.BLEACHER_SUPPORT.get().defaultBlockState().setValue(FurnitureBlock.FACING, facing);
        List<BlockPos> placed = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int i = 0; i < width; i++) {
                BlockPos p = origin.relative(right, i).relative(back, r).above(r);
                boolean isAisle = aisle > 0 && i % (aisle + 1) == aisle;
                if (place(level, p, isAisle ? stairs : seat)) placed.add(p);
                // Fill the gap down to the ground level of the first row.
                for (int k = 1; k <= r; k++) place(level, p.below(k), support);
            }
        }
        // Each row learns about its neighbours once everything is in place.
        for (BlockPos p : placed) {
            BlockState s = level.getBlockState(p);
            BlockState updated = Block.updateFromNeighbourShapes(s, level, p);
            if (updated != s) level.setBlock(p, updated, Block.UPDATE_CLIENTS);
        }
        return placed.size();
    }

    private static boolean place(Level level, BlockPos p, BlockState state) {
        if (level.isOutsideBuildHeight(p)) return false;
        BlockState current = level.getBlockState(p);
        boolean sameKind = current.getBlock() == state.getBlock();
        if (!current.canBeReplaced() && !sameKind) return false;
        level.setBlock(p, state, Block.UPDATE_ALL);
        return true;
    }
}
