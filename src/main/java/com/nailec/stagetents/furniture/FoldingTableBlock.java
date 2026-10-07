package com.nailec.stagetents.furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A folding table at one and a half times the 180 × 70 cm original: about 2.7 m long, three blocks,
 * shown open. The top is not dyed.
 */
public class FoldingTableBlock extends MultiPropBlock {
    public FoldingTableBlock(Properties props) {
        super(props, Spec.of(true, DyeColor.WHITE,
                new double[]{2.4, 16.85, -0.4, 16, 17.76, 16.4},
                new double[]{3.4, 0, 1.3, 10.1, 16.85, 14.7}),
                new int[][]{{0, 0}, {1, 0}, {2, 0}});
    }

    @Override
    protected PropPartBlock.Form partForm(Direction facing, int[] cell) {
        boolean alongX = facing.getAxis() == Direction.Axis.Z;
        boolean end = cell[0] == 2;
        if (alongX) return end ? PropPartBlock.Form.FOLD_END_X : PropPartBlock.Form.FOLD_X;
        return end ? PropPartBlock.Form.FOLD_END_Z : PropPartBlock.Form.FOLD_Z;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.PASS;
    }
}
