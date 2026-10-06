package com.nailec.stagetents.furniture;

import com.nailec.stagetents.block.TentWrenchItem;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

/** Two-block site cabin. Right-click opens the door; a dye recolours the shell. */
public class SiteToiletBlock extends MultiPropBlock {
    public static final BooleanProperty OPEN = BooleanProperty.create("open");

    public SiteToiletBlock(Properties props) {
        super(props, Spec.of(true, DyeColor.BLUE, new double[]{2, 0, 2, 14, 16, 14}), new int[][]{{0, 0}, {0, 1}});
        registerDefaultState(defaultBlockState().setValue(OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(OPEN);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof DyeItem || player.getItemInHand(hand).getItem() instanceof TentWrenchItem) {
            return super.use(state, level, pos, player, hand, hit);
        }
        if (!level.isClientSide) {
            boolean open = !state.getValue(OPEN);
            level.setBlock(pos, state.setValue(OPEN, open), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.4F, open ? 1.2F : 0.9F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
