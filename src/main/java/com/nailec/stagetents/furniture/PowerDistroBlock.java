package com.nailec.stagetents.furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Portable stage power distro. One block, facing the player. The case is black rubber, not dyed. */
public class PowerDistroBlock extends FurnitureBlock {
    public PowerDistroBlock(Properties props) {
        super(props, Spec.of(true, DyeColor.BLACK, new double[]{1.0, 0, 1.6, 15.0, 13.6, 15.4}));
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.PASS;
    }
}
