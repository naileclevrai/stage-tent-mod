package com.nailec.stagetents.furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Festival plancha. Right-click lights the plate; a dye recolours the fascia and the splashback. */
public class PlanchaBlock extends FurnitureBlock {
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public PlanchaBlock(Properties props) {
        super(props.lightLevel(state -> state.getValue(LIT) ? 11 : 0), Spec.of(true, DyeColor.RED,
                new double[]{-2.6, 0, -2.4, 19.9, 21.9, 15.4},
                new double[]{-2.1, 21.6, 13.4, 18.1, 27.5, 15.4}));
    }

    @Override
    protected BlockState fillDefault(BlockState state) {
        return state.setValue(LIT, false);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionResult dyed = super.use(state, level, pos, player, hand, hit);
        if (dyed != InteractionResult.PASS) return dyed;
        if (!level.isClientSide) {
            boolean lit = !state.getValue(LIT);
            level.setBlock(pos, state.setValue(LIT, lit), Block.UPDATE_ALL);
            level.playSound(null, pos, lit ? SoundEvents.FLINTANDSTEEL_USE : SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7F, lit ? 0.9F : 1.1F);
            player.displayClientMessage(Component.translatable(lit ? "message.stagetents.plancha_on" : "message.stagetents.plancha_off"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;
        double lx = 0.50 + (random.nextDouble() - 0.5) * 0.70;
        double lz = 0.44 + (random.nextDouble() - 0.5) * 0.42;
        Direction facing = state.getValue(FACING);
        double dx = lx - 0.5;
        double dz = lz - 0.5;
        double rx = dx;
        double rz = dz;
        switch (facing) {
            case SOUTH -> { rx = -dx; rz = -dz; }
            case WEST -> { rx = dz; rz = -dx; }
            case EAST -> { rx = -dz; rz = dx; }
            default -> { }
        }
        double x = pos.getX() + 0.5 + rx;
        double z = pos.getZ() + 0.5 + rz;
        if (random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, pos.getY() + 1.52, z, 0, 0.02, 0);
        }
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.FLAME, x, pos.getY() + 1.40, z, 0, 0.012, 0);
        }
        if (random.nextInt(14) == 0) {
            level.playLocalSound(pos, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.35F, 1.0F + random.nextFloat() * 0.2F, false);
        }
    }
}
