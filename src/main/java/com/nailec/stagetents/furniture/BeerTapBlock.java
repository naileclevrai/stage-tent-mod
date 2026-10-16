package com.nailec.stagetents.furniture;

import com.nailec.stagetents.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/** T-bar draft tower. On a bar counter it rises onto the counter top; it pours only when a keg and gas are hosed up. */
public class BeerTapBlock extends FurnitureBlock implements EntityBlock {
    /** Standing on a bar counter, whose top sits above the block line. */
    public static final BooleanProperty BAR = BooleanProperty.create("bar");
    public static final double BAR_TOP = 0.12;

    private static final double[][] BOXES = {
            {2.7, 0, 5.5, 13.3, 0.6, 8.1},
            {6.7, 0, 7.8, 9.3, 9.6, 10.3},
            {3.2, 8.9, 8.3, 12.8, 10.6, 9.8},
            {4.0, 6.3, 6.4, 12.0, 13.3, 8.2},
    };
    private final Map<Direction, VoxelShape> floor = new EnumMap<>(Direction.class);
    private final Map<Direction, VoxelShape> raised = new EnumMap<>(Direction.class);

    public BeerTapBlock(Properties props) {
        super(props, Spec.of(true, DyeColor.WHITE, BOXES));
        for (Direction d : Direction.Plane.HORIZONTAL) {
            VoxelShape s = Shapes.empty();
            for (double[] b : BOXES) s = Shapes.or(s, rotated(b, d));
            floor.put(d, s.optimize());
            raised.put(d, s.move(0, BAR_TOP, 0).optimize());
        }
    }

    @Override
    protected BlockState fillDefault(BlockState state) {
        return state.setValue(BAR, false);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BAR);
    }

    public static boolean onBar(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos.below()).getBlock() instanceof ConnectedFurnitureBlock c && c.group.equals("bar");
    }

    public static double lift(BlockState state) {
        return state.hasProperty(BAR) && state.getValue(BAR) ? BAR_TOP : 0.0;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState s = super.getStateForPlacement(ctx);
        return s == null ? null : s.setValue(BAR, onBar(ctx.getLevel(), ctx.getClickedPos()));
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return dir == Direction.DOWN ? state.setValue(BAR, onBar(level, pos)) : state;
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return (state.getValue(BAR) ? raised : floor).get(state.getValue(FACING));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DraftBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModRegistry.DRAFT_BE.get()) return null;
        return (l, p, s, be) -> {
            if (l.hasChunkAt(p.below())) {
                boolean bar = onBar(l, p);
                if (s.getValue(BAR) != bar) l.setBlock(p, s.setValue(BAR, bar), Block.UPDATE_ALL);
            }
            if ((l.getGameTime() + p.asLong()) % 40 == 0) ((DraftBlockEntity) be).prune();
        };
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof DraftBlockEntity be) {
            be.detach();
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) DraftBeer.useTap(level, pos, player, hand);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof DraftBlockEntity be) || level.getGameTime() >= be.pourUntil()) return;
        Vec3 faucet = DraftBeer.faucet(state, pos);
        level.addParticle(ParticleTypes.FALLING_HONEY, faucet.x, faucet.y, faucet.z, 0, -0.04, 0);
    }
}
