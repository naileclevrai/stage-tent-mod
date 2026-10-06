package com.nailec.stagetents.furniture;

import com.nailec.stagetents.block.TentWrenchItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One module of a rubber cable ramp. Right-click opens and closes this module only.
 * Sneak with the wrench sets the channel count from 1 to 5. The module stays one block:
 * five channels fill it, and fewer channels narrow the mesh about the centre so each
 * groove keeps about the same width. The mesh numbers match {@code tools/props/props.py}.
 */
public class CableRampBlock extends Block {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    public static final IntegerProperty CHANNELS = IntegerProperty.create("channels", 1, 5);

    /** Walk-on height of the closed ramp, in pixels. The lid and its lozenges sit inside it. */
    private static final double WALK = 3.6;
    /** Full-width layout (five channels), in block units. Narrower counts scale X about 0.5. */
    private static final double BODY_X0 = 0.010, BODY_X1 = 0.990;
    private static final double LID_X0 = 0.264, LID_Y0 = 0.156, LID_Z0 = 0.010;
    private static final double LID_X1 = 0.742, LID_Y1 = 0.228, LID_Z1 = 0.990;
    private static final double HINGE_X = 0.742, HINGE_Y = 0.189;
    private static final double OPEN_DEG = -70.0;

    private final Map<Integer, VoxelShape> shapes = new HashMap<>();

    public CableRampBlock(Properties props) {
        super(props);
        for (int channels = 1; channels <= 5; channels++) {
            double[] walk = new double[]{sx(BODY_X0, channels) * 16, 0, 0, sx(BODY_X1, channels) * 16, WALK, 16};
            List<double[]> lids = lidBoxes(channels);
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                shapes.put(key(channels, false, facing), FurnitureBlock.rotated(walk, facing));
                VoxelShape open = FurnitureBlock.rotated(walk, facing);
                for (double[] box : lids) open = Shapes.or(open, FurnitureBlock.rotated(box, facing));
                shapes.put(key(channels, true, facing), open.optimize());
            }
        }
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(OPEN, false)
                .setValue(CHANNELS, 2));
    }

    /** Full-width X folded about the centre. Five channels stay put; one channel is a fifth as wide. */
    private static double sx(double x, int channels) {
        return 0.5 + (x - 0.5) * (channels / 5.0);
    }

    private static int key(int channels, boolean open, Direction facing) {
        return channels + (open ? 8 : 0) + facing.ordinal() * 16;
    }

    /** Raised lid as a few boxes, in pixels, for a north-facing module of this width. */
    private static List<double[]> lidBoxes(int channels) {
        List<double[]> out = new ArrayList<>();
        double x0 = sx(LID_X0, channels);
        double x1 = sx(LID_X1, channels);
        double hx = sx(HINGE_X, channels);
        int slices = 8;
        for (int i = 0; i < slices; i++) {
            double a = x0 + (x1 - x0) * i / slices;
            double b = x0 + (x1 - x0) * (i + 1) / slices;
            double minX = 99, minY = 99, minZ = 99, maxX = -99, maxY = -99, maxZ = -99;
            for (double x : new double[]{a, b}) {
                for (double y : new double[]{LID_Y0, LID_Y1}) {
                    for (double z : new double[]{LID_Z0, LID_Z1}) {
                        double[] p = rotZ(x, y, z, hx);
                        minX = Math.min(minX, p[0]);
                        minY = Math.min(minY, p[1]);
                        minZ = Math.min(minZ, p[2]);
                        maxX = Math.max(maxX, p[0]);
                        maxY = Math.max(maxY, p[1]);
                        maxZ = Math.max(maxZ, p[2]);
                    }
                }
            }
            out.add(new double[]{minX * 16, minY * 16, minZ * 16, maxX * 16, maxY * 16, maxZ * 16});
        }
        return out;
    }

    /** Same rotation as {@code mesh.rot_z}, around the hinge of this channel count. */
    private static double[] rotZ(double x, double y, double z, double hx) {
        double c = Math.cos(Math.toRadians(OPEN_DEG));
        double s = Math.sin(Math.toRadians(OPEN_DEG));
        double dx = x - hx, dy = y - HINGE_Y;
        return new double[]{hx + dx * c - dy * s, HINGE_Y + dx * s + dy * c, z};
    }

    public static void toggle(Level level, BlockPos pos, BlockState state) {
        boolean open = !state.getValue(OPEN);
        level.setBlock(pos, state.setValue(OPEN, open), Block.UPDATE_ALL);
        level.playSound(null, pos, open ? SoundEvents.IRON_TRAPDOOR_OPEN : SoundEvents.IRON_TRAPDOOR_CLOSE,
                SoundSource.BLOCKS, 0.35F, open ? 1.05F : 0.88F);
    }

    public static void applyChannels(Level level, BlockPos pos, BlockState state, int channels) {
        channels = Mth.clamp(channels, 1, 5);
        if (state.getValue(CHANNELS) != channels) {
            level.setBlock(pos, state.setValue(CHANNELS, channels), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, CHANNELS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapes.get(key(state.getValue(CHANNELS), state.getValue(OPEN), state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof TentWrenchItem) return InteractionResult.PASS;
        if (!level.isClientSide) toggle(level, pos, state);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

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
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
