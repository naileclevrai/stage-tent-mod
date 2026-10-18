package com.nailec.stagetents.furniture;

import net.minecraft.core.BlockPos;
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

/** Site beacon. Right-click lights the caged lens. */
public class WorkLightBlock extends FurnitureBlock {
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public WorkLightBlock(Properties props) {
        super(props.lightLevel(state -> state.getValue(LIT) ? 12 : 0), Spec.of(true, DyeColor.ORANGE,
                new double[]{5.0, 0.0, 5.0, 11.0, 1.6, 11.0},
                new double[]{6.6, 1.0, 6.4, 10.2, 11.4, 9.6}));
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
        if (!level.isClientSide) {
            boolean lit = !state.getValue(LIT);
            level.setBlock(pos, state.setValue(LIT, lit), Block.UPDATE_ALL);
            level.playSound(null, pos, lit ? SoundEvents.FLINTANDSTEEL_USE : SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, lit ? 1.3F : 1.0F);
            player.displayClientMessage(Component.translatable(lit ? "message.stagetents.work_light_on" : "message.stagetents.work_light_off"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT) || random.nextInt(4) != 0) return;
        level.addParticle(ParticleTypes.END_ROD,
                pos.getX() + 0.5, pos.getY() + 0.62, pos.getZ() + 0.5,
                (random.nextDouble() - 0.5) * 0.008, 0.015, (random.nextDouble() - 0.5) * 0.008);
    }
}
