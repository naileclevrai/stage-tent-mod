package com.nailec.stagetents.furniture;

import com.nailec.stagetents.client.ClientHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * Anchor plate of a scaffolding control tower. The mesh and the decks live around it.
 * Right-click opens the size menu. A dye recolours the roof and the side sheets.
 */
public class ControlTowerBlock extends FurnitureBlock implements EntityBlock {
    /** One scaffold bay, in blocks. Standards and deck panels sit on this step. */
    public static final int BAY = 3;
    /** Height of one lift, in blocks. Decks sit on this step. */
    public static final int LIFT = 3;
    public static final int MIN_BAYS = 2;
    public static final int MAX_WIDTH = 6;
    public static final int MAX_DEPTH = 4;
    public static final int MIN_LEVELS = 2;
    public static final int MAX_LEVELS = 6;

    public ControlTowerBlock(Properties props) {
        super(props, Spec.of(true, DyeColor.WHITE,
                new double[]{0, 0, 0, 16, 2, 16},
                new double[]{0, 0, 0, 4, 16, 4}));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ControlTowerBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ControlTowerBlockEntity be) be.rebuild();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof ControlTowerBlockEntity be) {
            be.clearCells();
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof DyeItem) return super.use(state, level, pos, player, hand, hit);
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openControlTowerScreen(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Model point to an offset from the anchor corner. Same turn as the block renderer. */
    public static double[] modelToWorld(Direction facing, double mx, double mz) {
        double dx = mx - 0.5, dz = mz - 0.5;
        return switch (facing) {
            case EAST -> new double[]{0.5 - dz, 0.5 + dx};
            case SOUTH -> new double[]{0.5 - dx, 0.5 - dz};
            case WEST -> new double[]{0.5 + dz, 0.5 - dx};
            default -> new double[]{mx, mz};
        };
    }

    /** Model block {@code (bx, bz)} to a world block offset from the anchor. */
    public static int[] cell(Direction facing, int bx, int bz) {
        return switch (facing) {
            case SOUTH -> new int[]{-bx, -bz};
            case EAST -> new int[]{-bz, bx};
            case WEST -> new int[]{bz, -bx};
            default -> new int[]{bx, bz};
        };
    }
}
