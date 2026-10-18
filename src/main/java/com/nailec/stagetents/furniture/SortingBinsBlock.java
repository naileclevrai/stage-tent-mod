package com.nailec.stagetents.furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Pair of corrugated festival sorting bins with cap lids. The colours name the stream, so a dye does nothing. */
public class SortingBinsBlock extends FurnitureBlock {
    public SortingBinsBlock(Properties props) {
        super(props, Spec.of(true, DyeColor.WHITE,
                new double[]{0.2, 0.0, 4.1, 15.8, 17.9, 11.9}));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.PASS;
    }
}
