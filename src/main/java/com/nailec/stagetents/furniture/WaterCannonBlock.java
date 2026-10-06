package com.nailec.stagetents.furniture;

import com.nailec.stagetents.ModRegistry;
import com.nailec.stagetents.block.TentWrenchItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mist cannon on a chequer-plate skid, two blocks wide, with the mouth in the block it faces.
 * Right-click starts and stops the jet. Sneak with the wrench sets tilt, pan and how far the plume carries.
 * A dye recolours the barrel. Not a water bowser.
 */
public class WaterCannonBlock extends MultiPropBlock implements EntityBlock {
    public static final BooleanProperty RUNNING = BooleanProperty.create("running");
    /** 0 is 20° above the horizontal, 10 is 70°. The barrel meshes use the same steps. */
    public static final int MAX = 10;
    public static final IntegerProperty PITCH = IntegerProperty.create("pitch", 0, MAX);
    private static final double[] SKID = {0.3, 0, 0.3, 16.8, 6.7, 15.7};

    /** pitch, facing and pan share one collision box. Pan is not a block property, so this is filled as it is used. */
    private final Map<Integer, VoxelShape> shapes = new HashMap<>();

    public WaterCannonBlock(Properties props) {
        super(props, Spec.of(true, DyeColor.BLUE, SKID), new int[][]{{0, 0}});
        registerDefaultState(defaultBlockState().setValue(RUNNING, true).setValue(PITCH, 7));
    }

    /** Degrees above the horizontal. */
    public static int elevation(int pitch) {
        return 20 + Mth.clamp(pitch, 0, MAX) * 5;
    }

    /**
     * Barrel collision at this tilt and pan, already turned to {@code facing}.
     * Positive pan swings the mouth to the right of the skid. The box may leave the main block.
     */
    public static VoxelShape barrelShape(int pitch, int yaw, Direction facing) {
        return rotated(barrelBox(pitch, yaw), facing);
    }

    /** Axis-aligned barrel after tilt and pan, in north-facing pixels. Matches the rendered mesh. */
    private static double[] barrelBox(int pitch, int yaw) {
        double tilt = Math.toRadians(90 - elevation(pitch));
        double pan = Math.toRadians(WaterCannonBlockEntity.snapYaw(yaw));
        double c = Math.cos(tilt), s = Math.sin(tilt);
        double cp = Math.cos(pan), sp = Math.sin(pan);
        double minX = 99, minY = 99, minZ = 99, maxX = -99, maxY = -99, maxZ = -99;
        for (double ly : new double[]{-0.62, 0.90}) {
            for (double lx : new double[]{-0.55, 0.55}) {
                for (double lz : new double[]{-0.55, 0.55}) {
                    double x = lx;
                    double y = 1.08 + ly * c + lz * s;
                    double z = -ly * s + lz * c;
                    // Same yaw as the renderer: positive pan turns the mouth to the right.
                    double rx = x * cp - z * sp;
                    double rz = x * sp + z * cp;
                    x = 0.5 + rx;
                    z = 0.37 + rz;
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    minZ = Math.min(minZ, z);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                    maxZ = Math.max(maxZ, z);
                }
            }
        }
        return new double[]{minX * 16, Math.max(0, minY * 16), minZ * 16, maxX * 16, maxY * 16, maxZ * 16};
    }

    /** Nozzle point and jet direction in model space, after tilt and pan. x, y, z, dx, dy, dz. */
    public static double[] aim(int pitch, int yaw) {
        double tilt = Math.toRadians(90 - elevation(pitch));
        double pan = Math.toRadians(WaterCannonBlockEntity.snapYaw(yaw));
        double c = Math.cos(tilt), s = Math.sin(tilt);
        double cp = Math.cos(pan), sp = Math.sin(pan);
        double lz = -0.84 * s;
        return new double[]{
                0.5 - lz * sp, 1.08 + 0.84 * c, 0.37 + lz * cp,
                s * sp, c, -s * cp
        };
    }

    public static void applyPitch(Level level, BlockPos pos, BlockState state, int pitch) {
        pitch = Mth.clamp(pitch, 0, MAX);
        if (state.getValue(PITCH) != pitch) level.setBlock(pos, state.setValue(PITCH, pitch), Block.UPDATE_ALL);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(RUNNING, PITCH);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        int yaw = level.getBlockEntity(pos) instanceof WaterCannonBlockEntity cannon ? cannon.yaw() : 0;
        Direction facing = state.getValue(FACING);
        int key = state.getValue(PITCH) + facing.ordinal() * 11 + (WaterCannonBlockEntity.snapYaw(yaw) + 180) * 44;
        return shapes.computeIfAbsent(key, k -> Shapes.or(rotated(SKID, facing), barrelShape(state.getValue(PITCH), yaw, facing)).optimize());
    }

    /** Side cell, then the block the mouth reaches into. */
    @Override
    public List<BlockPos> parts(BlockPos pos, Direction facing) {
        return List.of(pos.relative(facing.getClockWise()), pos.relative(facing));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (level.isClientSide) return;
        Direction facing = state.getValue(FACING);
        level.setBlock(pos.relative(facing.getClockWise()), part(sideForm(facing)), Block.UPDATE_ALL);
        level.setBlock(pos.relative(facing), part(noseForm(facing)), Block.UPDATE_ALL);
    }

    private static BlockState part(PropPartBlock.Form form) {
        return ModRegistry.PROP_PART.get().defaultBlockState().setValue(PropPartBlock.FORM, form);
    }

    private static PropPartBlock.Form sideForm(Direction facing) {
        return switch (facing) {
            case EAST -> PropPartBlock.Form.CANNON_SIDE_E;
            case SOUTH -> PropPartBlock.Form.CANNON_SIDE_S;
            case WEST -> PropPartBlock.Form.CANNON_SIDE_W;
            default -> PropPartBlock.Form.CANNON_SIDE_N;
        };
    }

    private static PropPartBlock.Form noseForm(Direction facing) {
        return switch (facing) {
            case EAST -> PropPartBlock.Form.CANNON_NOSE_E;
            case SOUTH -> PropPartBlock.Form.CANNON_NOSE_S;
            case WEST -> PropPartBlock.Form.CANNON_NOSE_W;
            default -> PropPartBlock.Form.CANNON_NOSE_N;
        };
    }

    /** Model point to a horizontal offset from the main block's corner. Rotation matches the blockstate. */
    public static double[] modelToWorld(Direction facing, double mx, double mz) {
        double dx = mx - 0.5, dz = mz - 0.5;
        return switch (facing) {
            case EAST -> new double[]{0.5 - dz, 0.5 + dx};
            case SOUTH -> new double[]{0.5 - dx, 0.5 - dz};
            case WEST -> new double[]{0.5 + dz, 0.5 - dx};
            default -> new double[]{mx, mz};
        };
    }

    /** Horizontal model direction, rotated the same way. Length is preserved. */
    public static double[] modelDir(Direction facing, double dx, double dz) {
        return switch (facing) {
            case EAST -> new double[]{-dz, dx};
            case SOUTH -> new double[]{-dx, -dz};
            case WEST -> new double[]{dz, -dx};
            default -> new double[]{dx, dz};
        };
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof DyeItem || player.getItemInHand(hand).getItem() instanceof TentWrenchItem) {
            return super.use(state, level, pos, player, hand, hit);
        }
        if (!level.isClientSide) {
            boolean running = !state.getValue(RUNNING);
            level.setBlock(pos, state.setValue(RUNNING, running), Block.UPDATE_ALL);
            player.displayClientMessage(Component.translatable(running ? "message.stagetents.cannon_on" : "message.stagetents.cannon_off"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WaterCannonBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide && type == ModRegistry.WATER_CANNON_BE.get()
                ? (l, p, s, be) -> WaterCannonBlockEntity.clientTick(l, p, s) : null;
    }
}
