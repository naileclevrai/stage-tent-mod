package com.nailec.stagetents.furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/**
 * A stage border: pleated cloth with a scalloped hem, hung from a pipe. Place blocks in a line at the height of
 * the pipe. The menu sets how far the whole run drops, from 25 cm up to 16 m. Dye recolours it.
 */
public class FriseBlock extends ConnectedFurnitureBlock implements EntityBlock {
    /** Each step is 25 cm. 1 is a short valance, 64 is a 16 m border. */
    public static final int MAX = 64;
    public static final IntegerProperty DROP = IntegerProperty.create("drop", 1, MAX);

    private final Map<Direction, VoxelShape>[] shapes = new Map[MAX + 1];

    public FriseBlock(Properties props) {
        super(props, Spec.of(true, DyeColor.BLACK, new double[]{0, 2, 5, 16, 16, 11}), "frise");
        for (int d = 1; d <= MAX; d++) {
            double y0 = Math.max(0, (0.96 - length(d) - 0.06) * 16);
            Map<Direction, VoxelShape> byFacing = new EnumMap<>(Direction.class);
            double[] box = {0, y0, 5, 16, 16, 11};
            for (Direction facing : Direction.Plane.HORIZONTAL) byFacing.put(facing, rotated(box, facing));
            shapes[d] = byFacing;
        }
        registerDefaultState(defaultBlockState().setValue(DROP, 3));
    }

    /** How far the hem hangs below the pipe, in blocks. */
    public static float length(int drop) {
        return net.minecraft.util.Mth.clamp(drop, 1, MAX) * 0.25F;
    }

    /** Menu label: 75 cm under a metre, then metres. */
    public static String label(int drop) {
        int cm = Math.round(length(drop) * 100F);
        if (cm < 100) return cm + " cm";
        if (cm % 100 == 0) return (cm / 100) + " m";
        if (cm % 10 == 0) return (cm / 100) + "." + ((cm % 100) / 10) + " m";
        return (cm / 100) + "." + (cm % 100 < 10 ? "0" : "") + (cm % 100) + " m";
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DROP);
    }

    @Override
    @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapes[state.getValue(DROP)].get(state.getValue(FACING));
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return getShape(state, level, pos, ctx);
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

    /** Menu: set the drop of the whole run so the hem stays level. */
    public static void applyDrop(Level level, BlockPos pos, BlockState state, int drop) {
        int next = net.minecraft.util.Mth.clamp(drop, 1, MAX);
        for (BlockPos p : Drapes.run(level, pos, state)) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof FriseBlock) level.setBlock(p, s.setValue(DROP, next), net.minecraft.world.level.block.Block.UPDATE_ALL);
        }
    }
}
