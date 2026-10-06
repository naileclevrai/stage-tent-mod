package com.nailec.stagetents.furniture;

import com.nailec.stagetents.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/**
 * Tripod turnstile. The cabinet stands on the left of the lane (seen walking through, in the facing direction) and the
 * three-arm rotor turns a third of a turn for every person who goes through.
 *
 * <ul>
 *     <li>{@link Mode#FREE}: always open, the rotor just turns.</li>
 *     <li>{@link Mode#BADGE}: closed; using the reader opens it for one passage (or a few seconds).</li>
 *     <li>{@link Mode#LOCKED}: closed.</li>
 * </ul>
 * A redstone signal opens it in any mode. The rigging wrench cycles the mode; sneaking with it opens the settings.
 * In badge mode the reader only accepts an {@link AccessBadgeItem} whose id matches.
 */
public class TurnstileBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public enum Mode implements StringRepresentable {
        FREE("free"), BADGE("badge"), LOCKED("locked");

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        public static Mode byId(int id) {
            Mode[] v = values();
            return v[Math.floorMod(id, v.length)];
        }
    }

    public static final EnumProperty<Mode> MODE = EnumProperty.create("mode", Mode.class);
    /** Rotor free to turn (no barrier, green arrow). */
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");

    /** How long a badge keeps the gate open when nobody goes through. */
    public static final int BADGE_TICKS = 100;

    // Model space (facing north, passage along z, cabinet on the -x side), in pixels.
    private static final double[][] CABINET = {{0.2, 0, 1.6, 5.8, 13.5, 14.4}, {0, 13.4, 1, 5.5, 17.1, 15}};
    private static final double[] BARRIER = {5.4, 0, 7.4, 16, 24, 8.6};
    /** What the crosshair sees of the rotor: the horizontal arm. */
    private static final double[] ARM_OUTLINE = {5.4, 13.5, 7, 16, 16, 9};
    private final Map<Direction, VoxelShape> cabinet = new EnumMap<>(Direction.class);
    private final Map<Direction, VoxelShape> closed = new EnumMap<>(Direction.class);
    private final Map<Direction, VoxelShape> outline = new EnumMap<>(Direction.class);

    public TurnstileBlock(Properties props) {
        super(props);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            VoxelShape s = Shapes.empty();
            for (double[] b : CABINET) s = Shapes.or(s, FurnitureBlock.rotated(b, d));
            cabinet.put(d, s.optimize());
            closed.put(d, Shapes.or(s, FurnitureBlock.rotated(BARRIER, d)).optimize());
            outline.put(d, Shapes.or(s, FurnitureBlock.rotated(ARM_OUTLINE, d)).optimize());
        }
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(MODE, Mode.FREE)
                .setValue(OPEN, true).setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, MODE, OPEN, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        boolean powered = ctx.getLevel().hasNeighborSignal(ctx.getClickedPos());
        // People go through in the direction the player was looking.
        BlockState s = defaultBlockState().setValue(FACING, ctx.getHorizontalDirection()).setValue(POWERED, powered);
        return s.setValue(OPEN, shouldBeOpen(s));
    }

    // ------------------------------------------------------------------ shapes

    @Override
    @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return outline.get(state.getValue(FACING));
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return (state.getValue(OPEN) ? cabinet : closed).get(state.getValue(FACING));
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    // ------------------------------------------------------------------ behaviour

    public static boolean shouldBeOpen(BlockState state) {
        return state.getValue(MODE) == Mode.FREE || state.getValue(POWERED);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        if (level.isClientSide) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            BlockState s = state.setValue(POWERED, powered);
            level.setBlock(pos, s.setValue(OPEN, shouldBeOpen(s) || (state.getValue(OPEN) && !powered && hasCredit(level, pos))), Block.UPDATE_ALL);
        }
    }

    private static boolean hasCredit(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TurnstileBlockEntity be && be.credit() > 0;
    }

    /** Using the reader: a matching badge opens it, anything else is refused. */
    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof com.nailec.stagetents.block.TentWrenchItem) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof TurnstileBlockEntity be)) return InteractionResult.CONSUME;
        Mode mode = state.getValue(MODE);
        if (mode == Mode.LOCKED || (mode == Mode.BADGE && !state.getValue(OPEN))) {
            if (mode == Mode.LOCKED) {
                refuse(level, pos, player, be, "message.stagetents.turnstile_locked");
                return InteractionResult.CONSUME;
            }
            ItemStack stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof AccessBadgeItem)) {
                refuse(level, pos, player, be, "message.stagetents.turnstile_need_badge");
                return InteractionResult.CONSUME;
            }
            if (!be.accepts(stack)) {
                refuse(level, pos, player, be, "message.stagetents.turnstile_denied");
                return InteractionResult.CONSUME;
            }
            if (AccessBadgeItem.kind(stack) == AccessBadgeItem.Kind.SINGLE && !player.getAbilities().instabuild) stack.shrink(1);
            be.grant(be.holdTicks());
            level.setBlock(pos, state.setValue(OPEN, true), Block.UPDATE_ALL);
            if (be.clicks()) level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 0.6F, 1.9F);
            player.displayClientMessage(Component.translatable("message.stagetents.turnstile_granted"), true);
        }
        return InteractionResult.CONSUME;
    }

    private static void refuse(Level level, BlockPos pos, Player player, TurnstileBlockEntity be, String key) {
        if (be.clicks()) level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 0.6F, 0.6F);
        player.displayClientMessage(Component.translatable(key), true);
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof TurnstileBlockEntity be)) return 0;
        if (be.emitting()) return 15;
        return Math.min(15, be.passages());
    }

    /** Wrench: next mode. */
    public static void cycleMode(Level level, BlockPos pos, BlockState state, Player player) {
        Mode next = Mode.values()[(state.getValue(MODE).ordinal() + 1) % Mode.values().length];
        BlockState s = state.setValue(MODE, next);
        level.setBlock(pos, s.setValue(OPEN, shouldBeOpen(s)), Block.UPDATE_ALL);
        player.displayClientMessage(Component.translatable("message.stagetents.turnstile_mode",
                Component.translatable("screen.stagetents.turnstile." + next.getSerializedName())), true);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TurnstileBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ModRegistry.TURNSTILE_BE.get() ? (l, p, s, be) -> ((TurnstileBlockEntity) be).tick() : null;
    }
}
