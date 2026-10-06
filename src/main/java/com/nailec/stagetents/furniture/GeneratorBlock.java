package com.nailec.stagetents.furniture;

import com.nailec.stagetents.block.TentWrenchItem;
import com.nailec.stagetents.client.ClientHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
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

/** Site generator, three blocks long and two high. Right-click starts and stops it; a dye recolours the canopy. */
public class GeneratorBlock extends MultiPropBlock {
    public static final BooleanProperty RUNNING = BooleanProperty.create("running");

    public GeneratorBlock(Properties props) {
        super(props, Spec.of(true, DyeColor.ORANGE, new double[]{0, 0, 1, 16, 16, 15}),
                new int[][]{{-1, 0}, {0, 0}, {1, 0}, {-1, 1}, {0, 1}, {1, 1}});
        registerDefaultState(defaultBlockState().setValue(RUNNING, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(RUNNING);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof DyeItem || player.getItemInHand(hand).getItem() instanceof TentWrenchItem) {
            return super.use(state, level, pos, player, hand, hit);
        }
        if (!level.isClientSide) {
            boolean running = !state.getValue(RUNNING);
            level.setBlock(pos, state.setValue(RUNNING, running), Block.UPDATE_ALL);
            player.displayClientMessage(Component.translatable(running ? "message.stagetents.generator_on" : "message.stagetents.generator_off"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(RUNNING)) return;
        // Exhaust stack on the roof: 1.05 towards the model's -x (counter-clockwise of the facing), 0.15 towards its back.
        Direction back = state.getValue(FACING).getOpposite();
        Direction right = state.getValue(FACING).getClockWise();
        double x = pos.getX() + 0.5 + back.getStepX() * 0.15 - right.getStepX() * 1.05;
        double z = pos.getZ() + 0.5 + back.getStepZ() * 0.15 - right.getStepZ() * 1.05;
        level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, pos.getY() + 2.1, z, 0, 0.05, 0);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.playGenerator(level, pos));
    }
}
