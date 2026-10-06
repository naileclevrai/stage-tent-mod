package com.nailec.stagetents.furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * A three-block pine picnic table, half again the first size: plank top, a bench on each long side, A-frames at
 * both ends. Empty-hand use sits on the bench that was clicked. One person per bench.
 */
public class PicnicTableBlock extends MultiPropBlock {
    public PicnicTableBlock(Properties props) {
        super(props, Spec.of(true, DyeColor.WHITE,
                new double[]{0.8, 16.8, 0.8, 47.2, 18.3, 15.2},
                new double[]{2.2, 9.9, -6.4, 45.8, 11.0, -0.7},
                new double[]{2.2, 9.9, 16.7, 45.8, 11.0, 22.4}),
                new int[][]{{0, 0}, {1, 0}, {2, 0}});
    }

    @Override
    protected PropPartBlock.Form partForm(Direction facing) {
        return facing.getAxis() == Direction.Axis.Z ? PropPartBlock.Form.TABLE_X : PropPartBlock.Form.TABLE_Z;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionResult parent = super.use(state, level, pos, player, hand, hit);
        if (parent != InteractionResult.PASS) return parent;
        if (!player.getItemInHand(hand).isEmpty() || player.isShiftKeyDown()) return InteractionResult.PASS;
        if (hit.getLocation().y - pos.getY() > 0.85) return InteractionResult.PASS;
        if (!level.isClientSide && !sit(level, pos, state, hit, player)) return InteractionResult.PASS;
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private boolean sit(Level level, BlockPos pos, BlockState state, BlockHitResult hit, Player player) {
        Direction facing = state.getValue(FACING);
        Direction along = facing.getClockWise();
        double lx = hit.getLocation().x - (pos.getX() + 0.5);
        double lz = hit.getLocation().z - (pos.getZ() + 0.5);
        boolean front = lx * facing.getStepX() + lz * facing.getStepZ() >= 0;
        List<BlockPos> extra = parts(pos, facing);
        BlockPos at = front || extra.isEmpty() ? pos : extra.get(0);
        double face = front ? 0.72 : -0.72;
        double slide = front ? 1.0 : 0.0;
        double ox = facing.getStepX() * face + along.getStepX() * slide;
        double oz = facing.getStepZ() * face + along.getStepZ() * slide;
        return SeatEntity.sit(level, at, ox, 0.69, oz, player);
    }
}
