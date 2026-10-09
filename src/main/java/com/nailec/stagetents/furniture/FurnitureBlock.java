package com.nailec.stagetents.furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/**
 * Event furniture: one dyeable part (tablecloth, cushion, seat shell, panel, rope), optionally a facing, optionally a
 * seat. Right-click with a dye recolours it; right-click with an empty hand sits down on seats.
 */
public class FurnitureBlock extends Block {
    public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    /**
     * How a piece of furniture behaves.
     *
     * @param directional whether it faces the player who places it
     * @param seatHeight  seat height in blocks, negative when it is not a seat
     * @param seatBack    how far the seat sits behind the centre, towards the back (bleachers)
     * @param color       colour of the dyed part when placed
     * @param shape       collision boxes for a block facing north, in pixels: {x0, y0, z0, x1, y1, z1}
     */
    public record Spec(boolean directional, double seatHeight, double seatBack, DyeColor color, double[]... shape) {
        public static Spec of(boolean directional, DyeColor color, double[]... shape) {
            return new Spec(directional, -1, 0, color, shape);
        }

        public static Spec seat(double height, double back, DyeColor color, double[]... shape) {
            return new Spec(true, height, back, color, shape);
        }
    }

    protected final Spec spec;
    private final Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);

    public FurnitureBlock(Properties props, Spec spec) {
        super(props);
        this.spec = spec;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            VoxelShape s = Shapes.empty();
            for (double[] b : spec.shape()) s = Shapes.or(s, rotated(b, d));
            shapes.put(d, s.optimize());
        }
        registerDefaultState(fillDefault(stateDefinition.any().setValue(COLOR, spec.color()).setValue(FACING, Direction.NORTH)));
    }

    /** Extra blockstate properties a subclass registered. The default must mention every one of them. */
    protected BlockState fillDefault(BlockState state) {
        return state;
    }

    /** Rotates a north-facing box (pixels) to face {@code d}. */
    static VoxelShape rotated(double[] b, Direction d) {
        double x0 = b[0], z0 = b[2], x1 = b[3], z1 = b[5];
        return switch (d) {
            case SOUTH -> Block.box(16 - x1, b[1], 16 - z1, 16 - x0, b[4], 16 - z0);
            case WEST -> Block.box(z0, b[1], 16 - x1, z1, b[4], 16 - x0);
            case EAST -> Block.box(16 - z1, b[1], x0, 16 - z0, b[4], x1);
            default -> Block.box(x0, b[1], z0, x1, b[4], z1);
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COLOR, FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState s = defaultBlockState();
        return spec.directional() ? s.setValue(FACING, ctx.getHorizontalDirection().getOpposite()) : s;
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapes.get(spec.directional() ? state.getValue(FACING) : Direction.NORTH);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof DyeItem dye) {
            if (state.getValue(COLOR) == dye.getDyeColor()) return InteractionResult.PASS;
            if (!level.isClientSide) level.setBlock(pos, state.setValue(COLOR, dye.getDyeColor()), Block.UPDATE_ALL);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (spec.seatHeight() >= 0 && held.isEmpty() && !player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                Direction back = state.getValue(FACING).getOpposite();
                double ox = back.getStepX() * spec.seatBack(), oz = back.getStepZ() * spec.seatBack();
                if (SeatEntity.sit(level, pos, ox, spec.seatHeight(), oz, player)) return InteractionResult.CONSUME;
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    /** Furniture is mostly open frames and thin parts: let sky light through instead of casting block shadows. */
    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState rotate(BlockState state, Rotation rot) {
        return spec.directional() ? state.setValue(FACING, rot.rotate(state.getValue(FACING))) : state;
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, Mirror mirror) {
        return spec.directional() ? state.rotate(mirror.getRotation(state.getValue(FACING))) : state;
    }
}
