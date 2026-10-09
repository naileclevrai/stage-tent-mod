package com.nailec.stagetents.block;

import com.nailec.stagetents.client.ClientHooks;
import com.nailec.stagetents.tent.TentParams;
import com.nailec.stagetents.tent.TentPart;
import com.nailec.stagetents.tent.TentType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Invisible collision cell owned by a tent. It has no outline, so it can't be targeted, but any block placed into it
 * replaces it. It blocks motion, which keeps rain out, and lets sky light through like thin canvas.
 */
public class CanvasBlock extends Block {
    public static final EnumProperty<TentPart> PART = EnumProperty.create("part", TentPart.class);
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 15);
    /** Wall cells only: offset of the canvas plane from the cell centre, in eighths (see TentShape.CellSink). */
    public static final IntegerProperty SHIFT = IntegerProperty.create("shift", 0, 7);

    private static final VoxelShape[] ROOF = new VoxelShape[16];
    private static final VoxelShape[] FLOOR = new VoxelShape[16];
    /** Pole columns, 6 px wide, positioned by LEVEL (x, sixteenths) and SHIFT (z, eighths). */
    private static final VoxelShape[][] POLE = new VoxelShape[16][8];

    /** Thin wall panels for 8 directions x 8 offsets, built from 2 px columns on the inner side of the plane. */
    private static final VoxelShape[][] WALL = new VoxelShape[8][8];

    static {
        for (int i = 0; i < 16; i++) {
            int top = i + 1;
            ROOF[i] = Block.box(0, Math.max(0, top - 2), 0, 16, top, 16);
            FLOOR[i] = Block.box(0, 0, 0, 16, top, 16);
        }
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 8; z++) {
                double cx = x + 0.5, cz = z * 2 + 1;
                POLE[x][z] = Block.box(Math.max(0, cx - 3), 0, Math.max(0, cz - 3), Math.min(16, cx + 3), 16, Math.min(16, cz + 3));
            }
        }
        for (int dir = 0; dir < 8; dir++) {
            double a = dir * Math.PI / 4, nx = Math.cos(a), nz = Math.sin(a);
            for (int shift = 0; shift < 8; shift++) {
                double o = (shift + 0.5) / 8 - 0.5;
                VoxelShape shape = Shapes.empty();
                for (int i = 0; i < 8; i++) {
                    for (int j = 0; j < 8; j++) {
                        double cx = (i + 0.5) / 8 - 0.5, cz = (j + 0.5) / 8 - 0.5;
                        double d = cx * nx + cz * nz - o;
                        if (d <= 0.07 && d >= -0.3) {
                            shape = Shapes.or(shape, Block.box(i * 2, 0, j * 2, i * 2 + 2, 16, j * 2 + 2));
                        }
                    }
                }
                WALL[dir][shift] = shape.isEmpty() ? Shapes.block() : shape.optimize();
            }
        }
    }

    public CanvasBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(PART, TentPart.ROOF).setValue(LEVEL, 15).setValue(SHIFT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART, LEVEL, SHIFT);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /**
     * The Opus plate is one block in the middle of a huge trailer, so it is easy to miss.
     * Sneak and use the wall itself: the stage folds or opens. The wrench without sneaking still opens the menu.
     */
    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof BlockItem) return InteractionResult.PASS;
        boolean wrench = held.getItem() instanceof TentWrenchItem;
        if (!player.isShiftKeyDown() && !wrench) return InteractionResult.PASS;
        TentBlockEntity be = TentBlockEntity.findAround(level, pos);
        if (be == null) return InteractionResult.PASS;
        if (player.isShiftKeyDown() && be.type() == TentType.OPUS_4200) {
            if (!level.isClientSide) {
                TentParams params = be.params().copy();
                params.folded = !params.folded;
                be.applyParams(params);
                player.displayClientMessage(Component.translatable(params.folded
                        ? "message.stagetents.stage_folded" : "message.stagetents.stage_open"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!wrench) return InteractionResult.PASS;
        if (level.isClientSide) {
            BlockPos plate = be.getBlockPos();
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openTentScreen(plate));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Full floor cells (stage platforms, one-block floors) behave like ground: they can be aimed at and built on.
     * Every other cell is click-through and gets replaced by whatever is placed into it.
     */
    public static boolean isSolidFloor(BlockState state) {
        return state.getValue(PART) == TentPart.FLOOR && state.getValue(LEVEL) == 15;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return isSolidFloor(state) ? Shapes.block() : Shapes.empty();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(PART)) {
            case ROOF -> ROOF[state.getValue(LEVEL)];
            case WALL -> WALL[state.getValue(LEVEL) & 7][state.getValue(SHIFT)];
            case POLE -> POLE[state.getValue(LEVEL)][state.getValue(SHIFT)];
            case FLOOR -> FLOOR[state.getValue(LEVEL)];
        };
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    /** Players never break tent cells, not even in creative: the tent owns them and removes them itself. */
    @Override
    public boolean onDestroyedByPlayer(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
                                       net.minecraft.world.entity.player.Player player, boolean willHarvest,
                                       net.minecraft.world.level.material.FluidState fluid) {
        return false;
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext ctx) {
        return !isSolidFloor(state);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return false;
    }
}
