package com.nailec.stagetents.furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Orange traffic cone with a weighted foot. The colours stay put. */
public class TrafficConeBlock extends FurnitureBlock {
    public TrafficConeBlock(Properties props) {
        super(props, Spec.of(true, DyeColor.ORANGE,
                new double[]{1.6, 0.0, 1.6, 14.4, 1.5, 14.4},
                new double[]{4.2, 1.2, 4.2, 11.8, 8.0, 11.8},
                new double[]{6.2, 7.5, 6.2, 9.8, 14.0, 9.8},
                new double[]{7.0, 13.5, 7.0, 9.0, 19.5, 9.0}));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.PASS;
    }
}
