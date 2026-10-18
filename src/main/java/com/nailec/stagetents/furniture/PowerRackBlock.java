package com.nailec.stagetents.furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Flight-case distro rack. Sockets on the front, breakers on the back. Cables hang off the sockets. */
public class PowerRackBlock extends FurnitureBlock implements EntityBlock {
    public PowerRackBlock(Properties props) {
        super(props, Spec.of(true, DyeColor.BLACK, new double[]{0.2, 0, 1.6, 15.8, 15.9, 13.8}));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PowerNodeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return PowerNodeBlockEntity.ticker(level, type);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof PowerNodeBlockEntity be) {
            be.cables().detach();
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
