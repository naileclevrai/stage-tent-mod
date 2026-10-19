package com.nailec.stagetents.furniture;

import com.nailec.stagetents.ModRegistry;
import com.nailec.stagetents.client.ClientHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A forklift or a scissor lift. The main block holds the settings and draws the whole machine; invisible
 * {@link MachinePartBlock}s fill the cells the machine reaches into, and they all share the main block's shape,
 * so the platform can be stood on at any height. Right-click opens the controls, a dye repaints the bodywork.
 */
public class MachineBlock extends FurnitureBlock implements EntityBlock {
    public final MachineKind kind;

    public MachineBlock(Properties props, MachineKind kind) {
        super(props, Spec.of(true, kind.color, new double[]{0, 0, 0, 16, 16, 16}));
        this.kind = kind;
    }

    @Override
    @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ModRegistry.MACHINE_BE.get()
                ? (l, p, s, be) -> MachineBlockEntity.tick(l, p, s, (MachineBlockEntity) be) : null;
    }

    // ------------------------------------------------------------------ geometry

    /** Model point to an offset from the main block's corner. Same turn as the renderer. */
    public static double[] modelToWorld(Direction facing, double mx, double mz) {
        return ControlTowerBlock.modelToWorld(facing, mx, mz);
    }

    /** Model box (metres) to a box relative to the main block's corner: {x0, y0, z0, x1, y1, z1}. */
    public static double[] worldBox(Direction facing, double[] b) {
        double[] p = modelToWorld(facing, b[0], b[2]);
        double[] q = modelToWorld(facing, b[3], b[5]);
        return new double[]{Math.min(p[0], q[0]), b[1], Math.min(p[1], q[1]), Math.max(p[0], q[0]), b[4], Math.max(p[1], q[1])};
    }

    public static VoxelShape shapeOf(MachineKind kind, Direction facing, double rise, int load, boolean deck) {
        VoxelShape s = Shapes.empty();
        for (double[] b : kind.boxes(rise, load, deck)) {
            double[] w = worldBox(facing, b);
            s = Shapes.or(s, Shapes.box(w[0], w[1], w[2], w[3], w[4], w[5]));
        }
        return s.optimize();
    }

    /** Cells (offsets from the main block) the machine reaches into, the main block excluded. */
    public static Set<BlockPos> cells(MachineKind kind, Direction facing, double rise, int load, boolean deck) {
        Set<BlockPos> out = new LinkedHashSet<>();
        for (double[] b : kind.boxes(rise, load, deck)) {
            double[] w = worldBox(facing, b);
            int x0 = (int) Math.floor(w[0] + 1.0e-4), x1 = (int) Math.floor(w[3] - 1.0e-4);
            int y0 = (int) Math.floor(Math.max(0, w[1]) + 1.0e-4), y1 = (int) Math.floor(w[4] - 1.0e-4);
            int z0 = (int) Math.floor(w[2] + 1.0e-4), z1 = (int) Math.floor(w[5] - 1.0e-4);
            for (int x = x0; x <= x1; x++) {
                for (int y = y0; y <= y1; y++) {
                    for (int z = z0; z <= z1; z++) {
                        if (x != 0 || y != 0 || z != 0) out.add(new BlockPos(x, y, z));
                    }
                }
            }
        }
        return out;
    }

    /** Whether every cell is free or already ours. */
    public static boolean fits(Level level, BlockPos main, Set<BlockPos> cells) {
        for (BlockPos c : cells) {
            BlockPos at = main.offset(c);
            if (level.isOutsideBuildHeight(at)) return false;
            if (MachinePartBlock.mainOf(level, at) != null && main.equals(MachinePartBlock.mainOf(level, at))) continue;
            if (!level.getBlockState(at).canBeReplaced()) return false;
        }
        return true;
    }

    /** Puts a part in every free cell. Returns the cells that now hold one of ours. */
    public static List<BlockPos> placeParts(Level level, BlockPos main, Set<BlockPos> cells) {
        List<BlockPos> placed = new ArrayList<>();
        for (BlockPos c : cells) {
            BlockPos at = main.offset(c);
            if (main.equals(MachinePartBlock.mainOf(level, at))) {
                placed.add(at);
                continue;
            }
            if (level.isOutsideBuildHeight(at) || !level.getBlockState(at).canBeReplaced()) continue;
            BlockState part = MachinePartBlock.stateFor(c);
            if (part == null) continue;
            level.setBlock(at, part, Block.UPDATE_ALL);
            placed.add(at);
        }
        return placed;
    }

    // ------------------------------------------------------------------ block

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState s = super.getStateForPlacement(ctx);
        if (s == null) return null;
        Set<BlockPos> need = cells(kind, s.getValue(FACING), 0, 0, false);
        return fits(ctx.getLevel(), ctx.getClickedPos(), need) ? s : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MachineBlockEntity be) be.refreshParts(0);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof MachineBlockEntity be) {
            be.clearParts();
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return level.getBlockEntity(pos) instanceof MachineBlockEntity be ? be.shape() : Shapes.block();
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return getShape(state, level, pos, ctx);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof DyeItem) return super.use(state, level, pos, player, hand, hit);
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openMachineScreen(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
