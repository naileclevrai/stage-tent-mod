package com.nailec.stagetents.furniture;

import com.nailec.stagetents.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A stage leg: a full pleated curtain with a gathered heading, hung from a pipe. The block sits on the floor and
 * the cloth rises. The wrench sets the height of the whole run; blocks side by side become one curtain.
 */
public class PendrillonBlock extends ConnectedFurnitureBlock implements EntityBlock {
    public static final IntegerProperty HEIGHT = IntegerProperty.create("height", 2, 32);
    /** Tallest leg, in blocks. A full fly tower, not just a 6 m drape. */
    public static final int MAX = 32;

    public PendrillonBlock(Properties props) {
        super(props, Spec.of(true, DyeColor.BLACK, new double[]{0, 0, 5, 16, 16, 11}), "pendrillon");
        registerDefaultState(defaultBlockState().setValue(HEIGHT, 4));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HEIGHT);
    }

    @Override
    @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState s = super.getStateForPlacement(ctx);
        if (s == null) return null;
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        for (int i = 1; i < s.getValue(HEIGHT); i++) {
            BlockPos p = pos.above(i);
            if (!level.getBlockState(p).canBeReplaced(ctx) || level.isOutsideBuildHeight(p)) return null;
        }
        return s;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) syncParts(level, pos, state.getValue(FACING), state.getValue(HEIGHT));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) syncParts(level, pos, state.getValue(FACING), 1);
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DrapeBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof DyeItem dye) {
            if (!level.isClientSide && !Drapes.dye(level, pos, state, dye.getDyeColor())) return InteractionResult.PASS;
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (held.isEmpty()) return Drapes.openMenu(level, pos);
        return super.use(state, level, pos, player, hand, hit);
    }

    /** Menu: set the height of the whole run, if every leg has room above it. */
    public static boolean applyHeight(Level level, BlockPos pos, BlockState state, int height, Player player) {
        int next = net.minecraft.util.Mth.clamp(height, 2, MAX);
        int cur = state.getValue(HEIGHT);
        var run = Drapes.run(level, pos, state);
        if (next > cur) {
            for (BlockPos p : run) {
                if (!room(level, p, next)) {
                    if (player != null) player.displayClientMessage(Component.translatable("message.stagetents.drape_blocked"), true);
                    return false;
                }
            }
        }
        for (BlockPos p : run) {
            BlockState s = level.getBlockState(p);
            if (!(s.getBlock() instanceof PendrillonBlock)) continue;
            level.setBlock(p, s.setValue(HEIGHT, next), Block.UPDATE_ALL);
            syncParts(level, p, s.getValue(FACING), next);
        }
        return true;
    }

    private static boolean room(Level level, BlockPos pos, int height) {
        for (int i = 1; i < height; i++) {
            BlockPos p = pos.above(i);
            if (level.isOutsideBuildHeight(p)) return false;
            BlockState s = level.getBlockState(p);
            if (!s.isAir() && !s.is(ModRegistry.PROP_PART.get())) return false;
        }
        return true;
    }

    /** Fill or clear the column above the foot. Height 1 clears everything (the block itself is being removed). */
    static void syncParts(Level level, BlockPos pos, Direction facing, int height) {
        BlockState wall = wall(facing);
        for (int i = 1; i < MAX; i++) {
            BlockPos p = pos.above(i);
            BlockState s = level.getBlockState(p);
            if (i < height) {
                if (s.isAir() || s.is(ModRegistry.PROP_PART.get())) level.setBlock(p, wall, Block.UPDATE_ALL);
            } else if (s.is(ModRegistry.PROP_PART.get())) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    private static BlockState wall(Direction facing) {
        PropPartBlock.Form form = facing.getAxis() == Direction.Axis.Z ? PropPartBlock.Form.WALL_X : PropPartBlock.Form.WALL_Z;
        return ModRegistry.PROP_PART.get().defaultBlockState().setValue(PropPartBlock.FORM, form);
    }
}
