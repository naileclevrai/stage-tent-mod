package com.nailec.stagetents.block;

import com.nailec.stagetents.client.ClientHooks;
import com.nailec.stagetents.tent.TentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import com.nailec.stagetents.ModRegistry;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * Ground plate at the centre of a tent. Right-click opens the tent settings; breaking it takes the tent down and
 * drops the plate with its settings.
 */
public class TentBlock extends HorizontalDirectionalBlock implements EntityBlock {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 3, 16);
    /** Plate hidden from the tent settings: not drawn, no collision, outline only for wrench holders. */
    public static final BooleanProperty HIDDEN = BooleanProperty.create("hidden");

    public final TentType type;

    public TentBlock(TentType type, Properties props) {
        super(props);
        this.type = type;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.EAST).setValue(HIDDEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HIDDEN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // The front (entrance) end faces the player who places it.
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        if (!state.getValue(HIDDEN)) return SHAPE;
        if (type == TentType.OPUS_4200) return Shapes.block(); // centre of the deck stays buildable
        return ctx instanceof EntityCollisionContext ec && ec.getEntity() != null && ctx.isHoldingItem(ModRegistry.WRENCH.get())
                ? SHAPE : Shapes.empty();
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        if (!state.getValue(HIDDEN)) return SHAPE;
        // A hidden plate still carries the floor or stage that would otherwise have a hole in the middle.
        if (level.getBlockEntity(pos) instanceof TentBlockEntity be) {
            var s = be.shape();
            double top = s.isStage(0, 0) ? 1 : s.floorTop();
            if (top > 0) return Block.box(0, 0, 0, 16, Math.min(16, top * 16), 16);
        }
        return Shapes.empty();
    }

    @Override
    @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TentBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof TentBlockEntity be) {
            be.placeCells();
            if (!be.params().showPlate) level.setBlock(pos, state.setValue(HIDDEN, true), Block.UPDATE_ALL);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TentBlockEntity be) {
            be.clearCells();
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof TentWrenchItem) return InteractionResult.PASS;
        if (type == TentType.OPUS_4200 && player.getItemInHand(hand).getItem() instanceof BlockItem) return InteractionResult.PASS;
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openTentScreen(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        ItemStack stack = super.getCloneItemStack(level, pos, state);
        if (level.getBlockEntity(pos) instanceof TentBlockEntity be) {
            TentBlockItem.storeParams(stack, be.params());
        }
        return stack;
    }
}
