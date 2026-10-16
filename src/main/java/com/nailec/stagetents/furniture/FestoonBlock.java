package com.nailec.stagetents.furniture;

import com.nailec.stagetents.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Festoon post. The wrench sets the height and strings a cable to another post, in any direction. */
public class FestoonBlock extends FurnitureBlock implements EntityBlock {
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    public static final int MIN_HEIGHT = 2;
    public static final int MAX_HEIGHT = 6;
    public static final IntegerProperty HEIGHT = IntegerProperty.create("height", MIN_HEIGHT, MAX_HEIGHT);
    /** Longest cable, measured flat across the ground. */
    public static final int REACH = 16;
    public static final int MAX_LINKS = 6;
    public static final String LINK_KEY = "FestoonLink";

    private static final VoxelShape[] SHAPES = new VoxelShape[MAX_HEIGHT + 1];

    static {
        for (int h = MIN_HEIGHT; h <= MAX_HEIGHT; h++) {
            SHAPES[h] = Shapes.or(
                    Block.box(4.2, 0, 4.2, 11.8, 3.2, 11.8),
                    Block.box(6.4, 0, 6.4, 9.6, h * 16.0, 9.6));
        }
    }

    public FestoonBlock(Properties props) {
        super(props.lightLevel(state -> state.getValue(LIT) ? 12 : 0), Spec.of(false, DyeColor.YELLOW,
                new double[]{4.2, 0, 4.2, 11.8, 3.2, 11.8}));
    }

    /** Height of the ring the cable leaves, for a post of this height. */
    public static float top(BlockState state) {
        return state.getValue(HEIGHT) - 0.08F;
    }

    @Override
    protected BlockState fillDefault(BlockState state) {
        return state.setValue(LIT, false).setValue(HEIGHT, 3);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT, HEIGHT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState state = super.getStateForPlacement(ctx);
        return state == null ? null : state.setValue(LIT, dark(ctx.getLevel()));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPES[state.getValue(HEIGHT)];
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FestoonBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModRegistry.FESTOON_BE.get()) return null;
        return (l, p, s, be) -> serverTick(l, p, s, (FestoonBlockEntity) be);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof FestoonBlockEntity be) {
            be.detach();
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof DyeItem) return super.use(state, level, pos, player, hand, hit);
        return InteractionResult.PASS;
    }

    /** A venue without a sky stays lit. Outdoors, the bulbs follow dusk. */
    public static boolean dark(Level level) {
        return !level.dimensionType().hasSkyLight() || level.getSkyDarken() >= 4;
    }

    /** Sneak with the wrench: next mast height, then back to the shortest. */
    public static void cycleHeight(Level level, BlockPos pos, BlockState state, Player player) {
        int next = state.getValue(HEIGHT) + 1;
        if (next > MAX_HEIGHT) next = MIN_HEIGHT;
        level.setBlock(pos, state.setValue(HEIGHT, next), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.METAL_PRESSURE_PLATE_CLICK_ON, SoundSource.BLOCKS, 0.5F, 1.2F);
        player.displayClientMessage(Component.translatable("message.stagetents.festoon_height", next), true);
    }

    /**
     * Wrench: the first post is remembered, the second grows or drops a cable. Works on a diagonal and between
     * different heights. Clicking the remembered post again forgets it.
     */
    public static void link(Level level, BlockPos pos, Player player, ItemStack wrench) {
        if (wrench.getTag() != null && wrench.getTag().contains(LINK_KEY)) {
            BlockPos from = BlockPos.of(wrench.getTag().getLong(LINK_KEY));
            if (from.equals(pos)) {
                wrench.removeTagKey(LINK_KEY);
                player.displayClientMessage(Component.translatable("message.stagetents.festoon_cancel"), true);
                return;
            }
            if (!(level.getBlockEntity(from) instanceof FestoonBlockEntity source) || !level.getBlockState(from).is(ModRegistry.FESTOON.get())) {
                wrench.removeTagKey(LINK_KEY);
                player.displayClientMessage(Component.translatable("message.stagetents.festoon_gone"), true);
                return;
            }
            if (!(level.getBlockEntity(pos) instanceof FestoonBlockEntity target)) return;
            if (source.linkedTo(pos)) {
                source.removeLink(pos);
                target.removeLink(from);
                level.playSound(null, pos, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 0.6F, 1.1F);
                player.displayClientMessage(Component.translatable("message.stagetents.festoon_unlink"), true);
                wrench.removeTagKey(LINK_KEY);
                return;
            }
            double dx = pos.getX() - from.getX();
            double dz = pos.getZ() - from.getZ();
            double flat = Math.sqrt(dx * dx + dz * dz);
            if (flat < 0.5 || flat > REACH || Math.abs(pos.getY() - from.getY()) > 8) {
                player.displayClientMessage(Component.translatable("message.stagetents.festoon_far"), true);
                return;
            }
            if (source.linkCount() >= MAX_LINKS || target.linkCount() >= MAX_LINKS) {
                player.displayClientMessage(Component.translatable("message.stagetents.festoon_full"), true);
                return;
            }
            source.addLink(pos);
            target.addLink(from);
            level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.7F, 1.05F);
            player.displayClientMessage(Component.translatable("message.stagetents.festoon_link"), true);
            wrench.removeTagKey(LINK_KEY);
            return;
        }
        wrench.getOrCreateTag().putLong(LINK_KEY, pos.asLong());
        player.displayClientMessage(Component.translatable("message.stagetents.festoon_pick"), true);
    }

    private static void serverTick(Level level, BlockPos pos, BlockState state, FestoonBlockEntity be) {
        if ((level.getGameTime() + pos.asLong()) % 20 != 0) return;
        boolean dark = dark(level);
        if (state.getValue(LIT) != dark) level.setBlock(pos, state.setValue(LIT, dark), Block.UPDATE_ALL);
        be.prune();
    }
}
