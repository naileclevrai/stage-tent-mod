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
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Terrace mushroom heater. Right-click lights the burner; a dye recolours the bottle and the reflector. */
public class PatioHeaterBlock extends MultiPropBlock {
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public PatioHeaterBlock(Properties props) {
        super(props.lightLevel(state -> state.getValue(LIT) ? 13 : 0), Spec.of(true, DyeColor.BLACK,
                new double[]{3.2, 0, 3.2, 12.8, 12.0, 12.8},
                new double[]{6.5, 12.0, 6.5, 9.5, 16, 9.5}),
                new int[][]{{0, 0}, {0, 1}, {0, 2}});
    }

    @Override
    protected BlockState fillDefault(BlockState state) {
        return state.setValue(LIT, true);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }

    @Override
    protected PropPartBlock.Form partForm(Direction facing) {
        return PropPartBlock.Form.POLE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof DyeItem) return super.use(state, level, pos, player, hand, hit);
        if (!level.isClientSide) {
            boolean lit = !state.getValue(LIT);
            level.setBlock(pos, state.setValue(LIT, lit), Block.UPDATE_ALL);
            level.playSound(null, pos, lit ? SoundEvents.FLINTANDSTEEL_USE : SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, lit ? 1.1F : 0.9F);
            player.displayClientMessage(Component.translatable(lit ? "message.stagetents.heater_on" : "message.stagetents.heater_off"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;
        double ang = random.nextDouble() * Math.PI * 2;
        double rad = 0.04 + random.nextDouble() * 0.07;
        level.addParticle(ParticleTypes.FLAME,
                pos.getX() + 0.5 + Math.cos(ang) * rad, pos.getY() + 2.08, pos.getZ() + 0.5 + Math.sin(ang) * rad,
                0, 0.01, 0);
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 2.42, pos.getZ() + 0.5, 0, 0.02, 0);
        }
    }
}
