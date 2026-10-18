package com.nailec.stagetents.block;

import com.nailec.stagetents.ModRegistry;
import com.nailec.stagetents.tent.TentParams;
import com.nailec.stagetents.tent.TentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Square plinth and leg stub of a DJ cover. Place two of them in line, then use the wrench on one: that foot becomes the cover's
 * plate and the arches spring across to the other one, which the cover then draws itself.
 */
public class DjFootBlock extends Block {
    public static final BooleanProperty LINKED = BooleanProperty.create("linked");
    public static final int MIN_SPAN = 6;
    public static final int MAX_SPAN = 24;
    private static final VoxelShape SHAPE = net.minecraft.world.phys.shapes.Shapes.or(
            Block.box(0.5, 0, 0.5, 15.5, 7.5, 15.5), Block.box(6, 7.5, 6, 10, 16, 10));

    public DjFootBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(LINKED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LINKED);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(LINKED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    /** Wrench on a free foot: finds the nearest free foot in line and raises the cover between the two. */
    public static void raise(Level level, BlockPos pos, Player player) {
        BlockState here = level.getBlockState(pos);
        if (!(here.getBlock() instanceof DjFootBlock) || here.getValue(LINKED)) return;
        Direction best = null;
        int bestSpan = Integer.MAX_VALUE;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            for (int k = 1; k <= MAX_SPAN; k++) {
                BlockState st = level.getBlockState(pos.relative(d, k));
                if (!(st.getBlock() instanceof DjFootBlock)) continue;
                if (k >= MIN_SPAN && !st.getValue(LINKED) && k < bestSpan) {
                    best = d;
                    bestSpan = k;
                }
                break;
            }
        }
        if (best == null) {
            player.displayClientMessage(Component.translatable("message.stagetents.dj_foot.none", MIN_SPAN, MAX_SPAN), true);
            return;
        }
        BlockPos other = pos.relative(best, bestSpan);
        // Local +z of the cover runs toward the other foot.
        Direction facing = best.getCounterClockWise();
        BlockState plate = ModRegistry.DJ_ARCH.get().defaultBlockState().setValue(TentBlock.FACING, facing)
                .setValue(TentBlock.HIDDEN, true);
        level.setBlock(pos, plate, Block.UPDATE_ALL);
        level.setBlock(other, level.getBlockState(other).setValue(LINKED, true), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof TentBlockEntity be) {
            TentParams p = TentType.DJ_ARCH.defaults();
            p.width = bestSpan;
            be.linkFeet(p);
        }
        player.displayClientMessage(Component.translatable("message.stagetents.dj_foot.raised", bestSpan), true);
    }

    /** Far foot of a cover plate, by the plate's facing and span. */
    public static BlockPos partnerOf(BlockPos plate, Direction facing, int span) {
        return plate.relative(facing.getClockWise(), span);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        super.onRemove(state, level, pos, newState, moving);
        if (level.isClientSide || !state.getValue(LINKED) || newState.is(this)) return;
        // The far foot is gone: take the cover down. Its plate drops as a foot.
        for (Direction d : Direction.Plane.HORIZONTAL) {
            for (int k = MIN_SPAN; k <= MAX_SPAN; k++) {
                BlockPos p = pos.relative(d, k);
                if (level.getBlockEntity(p) instanceof TentBlockEntity be && be.type() == TentType.DJ_ARCH
                        && partnerOf(p, be.facing(), be.params().width).equals(pos)) {
                    level.destroyBlock(p, true);
                    return;
                }
            }
        }
    }
}
