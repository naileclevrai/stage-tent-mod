package com.nailec.stagetents.furniture;

import com.nailec.stagetents.StageTents;
import com.nailec.stagetents.WorldRepairQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Stainless queue rail. A run stays straight. The block where two rails meet at a right angle becomes a quarter-turn
 * whose arms point at those two rails.
 * <p>
 * The model, facing north, runs toward -z and toward +x. Blockstate rotation and {@link #rotated} move +x to
 * {@link Direction#getClockWise()} of the facing, so the mesh, the collision and {@link #facingFor} all describe
 * the same arm.
 */
@Mod.EventBusSubscriber(modid = StageTents.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GuideRailBlock extends FurnitureBlock {
    public static final BooleanProperty CORNER = BooleanProperty.create("corner");

    private final Map<Direction, VoxelShape> cornerShapes = new EnumMap<>(Direction.class);

    public GuideRailBlock(Properties props, Spec spec) {
        super(props, spec);
        double[] northArm = {7, 0, 0, 9, 24, 8};
        double[] eastArm = {8, 0, 7, 16, 24, 9};
        for (Direction d : Direction.Plane.HORIZONTAL) {
            cornerShapes.put(d, Shapes.or(rotated(northArm, d), rotated(eastArm, d)).optimize());
        }
        registerDefaultState(defaultBlockState().setValue(CORNER, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CORNER);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return resolve(super.getStateForPlacement(ctx), ctx.getLevel(), ctx.getClickedPos());
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return dir.getAxis().isHorizontal() ? resolve(state, level, pos) : state;
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(CORNER) ? cornerShapes.get(state.getValue(FACING)) : super.getShape(state, level, pos, ctx);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, Mirror mirror) {
        if (!state.getValue(CORNER)) return super.mirror(state, mirror);
        Direction facing = state.getValue(FACING);
        return state.setValue(FACING, facingFor(mirror.mirror(facing), mirror.mirror(facing.getClockWise())));
    }

    /** Neighbour directions whose rail runs into this block. A corner counts along both of its arms. */
    private List<Direction> links(BlockGetter level, BlockPos pos) {
        List<Direction> out = new ArrayList<>(4);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            // Reading an absent neighbouring chunk here can recurse into chunk loading.
            if (level instanceof ServerLevel server && !server.hasChunkAt(pos.relative(d))) continue;
            BlockState n = level.getBlockState(pos.relative(d));
            if (!(n.getBlock() instanceof GuideRailBlock)) continue;
            Direction facing = n.getValue(FACING);
            Direction towardsUs = d.getOpposite();
            boolean linked = n.getValue(CORNER)
                    ? towardsUs == facing || towardsUs == facing.getClockWise()
                    : facing.getAxis() == d.getAxis();
            if (linked) out.add(d);
        }
        return out;
    }

    private BlockState resolve(BlockState state, BlockGetter level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        List<Direction> axial = new ArrayList<>(2);
        List<Direction> side = new ArrayList<>(2);
        for (Direction d : links(level, pos)) {
            (d.getAxis() == facing.getAxis() ? axial : side).add(d);
        }
        // A rail already continues out both ends of this block: it stays a straight piece of the run.
        if (axial.size() >= 2) {
            return state.setValue(CORNER, false).setValue(FACING, facing);
        }
        // Elbow. Both arms are real neighbours, so the bend follows the rails that are actually there.
        if (axial.size() == 1 && !side.isEmpty()) {
            return corner(state, axial.get(0), side.get(0));
        }
        if (axial.isEmpty() && side.size() >= 2) {
            return corner(state, side.get(0), side.get(1));
        }
        if (axial.isEmpty() && side.size() == 1) {
            // Nothing on the run yet. The block faces the player, and the queue is built back toward that
            // side, so the free arm points along facing rather than forward into the empty cell.
            return corner(state, side.get(0), facing);
        }
        Direction straight = !axial.isEmpty() ? facing : (!side.isEmpty() ? side.get(0) : facing);
        return state.setValue(CORNER, false).setValue(FACING, straight);
    }

    private static BlockState corner(BlockState state, Direction a, Direction b) {
        return state.setValue(CORNER, true).setValue(FACING, facingFor(a, b));
    }

    /** Re-aim a rail already in the world. Neighbour updates are left to the second pass. */
    private void align(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockState next = resolve(state, level, pos);
        if (!next.equals(state)) level.setBlock(pos, next, Block.UPDATE_CLIENTS);
    }

    /** Corners placed before the arm direction was fixed keep their old facing until a chunk reloads. */
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getChunk() instanceof LevelChunk chunk)) return;
        List<BlockPos> rails = new ArrayList<>();
        int sectionY = level.getMinSection();
        int x0 = chunk.getPos().getMinBlockX();
        int z0 = chunk.getPos().getMinBlockZ();
        for (LevelChunkSection section : chunk.getSections()) {
            int y0 = sectionY++ << 4;
            if (section.hasOnlyAir()) continue;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        if (section.getBlockState(x, y, z).getBlock() instanceof GuideRailBlock) {
                            rails.add(new BlockPos(x0 + x, y0 + y, z0 + z));
                        }
                    }
                }
            }
        }
        if (rails.isEmpty()) return;
        WorldRepairQueue.submit(level, () -> {
            for (int pass = 0; pass < 2; pass++) {
                for (BlockPos pos : rails) {
                    if (!level.hasChunkAt(pos)) continue;
                    BlockState state = level.getBlockState(pos);
                    if (state.getBlock() instanceof GuideRailBlock rail) rail.align(state, level, pos);
                }
            }
        });
    }

    /** Facing whose model arms (facing, and clockwise of it) are exactly {@code a} and {@code b}. */
    private static Direction facingFor(Direction a, Direction b) {
        for (Direction f : Direction.Plane.HORIZONTAL) {
            if ((f == a && f.getClockWise() == b) || (f == b && f.getClockWise() == a)) return f;
        }
        return a;
    }
}
