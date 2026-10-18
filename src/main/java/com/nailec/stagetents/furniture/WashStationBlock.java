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
                new double[]{1.3, 0, 1.0, 14.7, 15.7, 12.8},
                new double[]{1.3, 15.7, 10.7, 14.7, 24.0, 12.8},
                new double[]{15.0, 5.8, 5.8, 17.0, 11.4, 10.2},
                new double[]{6.4, 0.2, 1.0, 9.6, 0.7, 4.2}));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof DyeItem) return super.use(state, level, pos, player, hand, hit);
        Direction facing = state.getValue(FACING);
        Direction right = facing.getClockWise();
        // Spout in model space, just over the basin. Model -z is the front.
        double dx = 0.0, dz = 0.44 - 0.5;
        double x = pos.getX() + 0.5 + right.getStepX() * dx + facing.getStepX() * -dz;
        double z = pos.getZ() + 0.5 + right.getStepZ() * dx + facing.getStepZ() * -dz;
        if (level.isClientSide) {
            for (int i = 0; i < 8; i++) {
                level.addParticle(ParticleTypes.FALLING_WATER, x, pos.getY() + 1.03, z, 0, -0.04, 0);
                level.addParticle(ParticleTypes.SPLASH, x, pos.getY() + 0.84, z, 0, 0, 0);
            }
        } else {
            level.playSound(null, pos, SoundEvents.POINTED_DRIPSTONE_DRIP_WATER, SoundSource.BLOCKS, 0.7F, 1.05F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
