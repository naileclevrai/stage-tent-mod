package com.nailec.stagetents.block;

import com.nailec.stagetents.tent.TentType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Foot of an extra stretch-tent mast. Place it where the canvas should be pushed up; right-click raises the mast
 * (sneak to lower). Stretch tents nearby pick it up automatically.
 */
public class StretchPoleBlock extends Block {
    public static final IntegerProperty HEIGHT = IntegerProperty.create("height", 2, 20);
    /** How far a stretch tent plate looks for poles. */
    public static final int RANGE = 24;
    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 4, 12);

    public StretchPoleBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(HEIGHT, 6));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HEIGHT);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.getItemInHand(hand).isEmpty() && !(player.getItemInHand(hand).getItem() instanceof TentWrenchItem)) {
            return InteractionResult.PASS;
        }
        int h = state.getValue(HEIGHT) + (player.isShiftKeyDown() ? -1 : 1);
        if (h < 2 || h > 20) return InteractionResult.sidedSuccess(level.isClientSide);
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(HEIGHT, h), Block.UPDATE_ALL);
            player.displayClientMessage(Component.translatable("message.stagetents.pole_height", h), true);
            refreshTents(level, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!old.is(this)) refreshTents(level, pos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        super.onRemove(state, level, pos, newState, moving);
        if (!newState.is(this)) refreshTents(level, pos);
    }

    /** Re-reads the poles of every stretch tent close enough to use this one. */
    public static void refreshTents(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) return;
        int r = RANGE + 1;
        for (int cx = (pos.getX() - r) >> 4; cx <= (pos.getX() + r) >> 4; cx++) {
            for (int cz = (pos.getZ() - r) >> 4; cz <= (pos.getZ() + r) >> 4; cz++) {
                LevelChunk chunk = server.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (BlockEntity e : chunk.getBlockEntities().values().toArray(new BlockEntity[0])) {
                    if (e instanceof TentBlockEntity be && be.type() == TentType.STRETCH
                            && Math.abs(be.getBlockPos().getX() - pos.getX()) <= RANGE
                            && Math.abs(be.getBlockPos().getZ() - pos.getZ()) <= RANGE) {
                        be.rescanStretchPoles(be.params());
                    }
                }
            }
        }
    }
}
