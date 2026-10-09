package com.nailec.stagetents.furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Backstage wash stand. A right-click runs the tap for a moment. */
public class WashStationBlock extends FurnitureBlock {
    public WashStationBlock(Properties props) {
        super(props, Spec.of(true, DyeColor.WHITE,
                new double[]{3.5, 0, 4.8, 12.5, 19, 12.2},
                new double[]{3.5, 18, 10.5, 12.5, 27, 12.2},
                new double[]{0.2, 8, 5.5, 3.4, 16, 10.5}));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof DyeItem) return super.use(state, level, pos, player, hand, hit);
        Direction facing = state.getValue(FACING);
        Direction right = facing.getClockWise();
        // Spout in model space, just over the basin. Model -z is the front.
        double dx = 0.0, dz = 0.40 - 0.5;
        double x = pos.getX() + 0.5 + right.getStepX() * dx + facing.getStepX() * -dz;
        double z = pos.getZ() + 0.5 + right.getStepZ() * dx + facing.getStepZ() * -dz;
        if (level.isClientSide) {
            for (int i = 0; i < 8; i++) {
                level.addParticle(ParticleTypes.FALLING_WATER, x, pos.getY() + 1.28, z, 0, -0.05, 0);
                level.addParticle(ParticleTypes.SPLASH, x, pos.getY() + 1.08, z, 0, 0, 0);
            }
        } else {
            level.playSound(null, pos, SoundEvents.POINTED_DRIPSTONE_DRIP_WATER, SoundSource.BLOCKS, 0.7F, 1.05F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
