package com.nailec.stagetents.furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Powder extinguisher standing on the floor. A click throws a cloud and puts out fire in front of the horn. */
public class ExtinguisherBlock extends FurnitureBlock {
    public ExtinguisherBlock(Properties props) {
        super(props, Spec.of(true, DyeColor.RED,
                new double[]{6.5, 0.0, 6.5, 9.5, 13.0, 9.5},
                new double[]{4.8, 11.8, 7.4, 8.0, 14.9, 9.9},
                new double[]{9.4, 0.0, 7.4, 11.2, 12.5, 8.6}));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof DyeItem) return super.use(state, level, pos, player, hand, hit);
        if (level.isClientSide) return InteractionResult.SUCCESS;
        Direction facing = state.getValue(FACING);
        double[] mouth = world(facing, 0.63, 0.04, 0.50);
        double x = pos.getX() + mouth[0];
        double y = pos.getY() + mouth[1];
        double z = pos.getZ() + mouth[2];
        level.playSound(null, x, y, z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 1.15F);
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CLOUD, x, y, z, 16, 0.12, 0.06, 0.12, 0.03);
            server.sendParticles(ParticleTypes.WHITE_ASH, x, y, z, 10, 0.22, 0.04, 0.22, 0.05);
        }
        for (int step = 1; step <= 2; step++) {
            BlockPos ahead = pos.relative(facing, step);
            douse(level, ahead);
            douse(level, ahead.above());
        }
        return InteractionResult.CONSUME;
    }

    private static void douse(Level level, BlockPos pos) {
        if (level.getBlockState(pos).is(BlockTags.FIRE)) level.removeBlock(pos, false);
    }

    /** Model point (north-facing, front at -Z) rotated into the block. */
    private static double[] world(Direction facing, double x, double y, double z) {
        return switch (facing) {
            case SOUTH -> new double[]{1.0 - x, y, 1.0 - z};
            case EAST -> new double[]{1.0 - z, y, x};
            case WEST -> new double[]{z, y, 1.0 - x};
            default -> new double[]{x, y, z};
        };
    }
}
