package com.nailec.stagetents.furniture;

import com.nailec.stagetents.ModRegistry;
import com.nailec.stagetents.client.ClientHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.ArrayList;
import java.util.List;

/**
 * A site light tower on its own generator, two blocks long. Right-click starts and stops it.
 * Sneak with the wrench sets the mast height, the way the lamp bank turns, and how far the floods tilt down.
 */
public class LightTowerBlock extends MultiPropBlock implements EntityBlock {
    public static final BooleanProperty RUNNING = BooleanProperty.create("running");
    /** Mast height in metres, from a lowered tower to the full 8 m. */
    public static final int MIN_HEIGHT = 2;
    public static final int MAX_HEIGHT = 8;
    public static final IntegerProperty HEIGHT = IntegerProperty.create("height", MIN_HEIGHT, MAX_HEIGHT);

    /** Mast centre in the main block, matching the mesh. */
    public static final float MAST_X = 0.62F;
    public static final float MAST_Z = 0.50F;
    public static final float MAST_BASE = 1.22F;
    public static final float EXHAUST_X = 1.50F;
    public static final float EXHAUST_Z = 0.68F;
    public static final float EXHAUST_Y = 1.34F;

    private static final double[] BODY = {4.8, 0, 0, 16, 22, 16};

    public LightTowerBlock(Properties props) {
        super(props, Spec.of(true, DyeColor.WHITE, BODY), new int[][]{{0, 0}, {1, 0}});
        registerDefaultState(defaultBlockState().setValue(RUNNING, true).setValue(HEIGHT, 6));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(RUNNING, HEIGHT);
    }

    @Override
    protected PropPartBlock.Form partForm(Direction facing, int[] cell) {
        return facing.getAxis() == Direction.Axis.Z ? PropPartBlock.Form.TOWER_X : PropPartBlock.Form.TOWER_Z;
    }

    public static void applyHeight(Level level, BlockPos pos, BlockState state, int height) {
        height = Mth.clamp(height, MIN_HEIGHT, MAX_HEIGHT);
        if (state.getValue(HEIGHT) != height) level.setBlock(pos, state.setValue(HEIGHT, height), Block.UPDATE_ALL);
    }

    /** Where the floods throw their light: the head, and one block along the aim. */
    public static List<BlockPos> lightPositions(BlockPos origin, BlockState state, int yaw, int tilt) {
        int height = state.getValue(HEIGHT);
        Direction facing = state.getValue(FACING);
        double[] at = modelToWorld(facing, MAST_X, MAST_Z);
        double tiltRad = Math.toRadians(tilt);
        double yawRad = Math.toRadians(yaw);
        double ax = Math.cos(tiltRad) * Math.sin(yawRad);
        double az = -Math.cos(tiltRad) * Math.cos(yawRad);
        double[] dir = modelDir(facing, ax, az);
        double y = origin.getY() + height - 0.15 - Math.sin(tiltRad) * 0.4;
        List<BlockPos> out = new ArrayList<>(2);
        out.add(BlockPos.containing(origin.getX() + at[0], y, origin.getZ() + at[1]));
        BlockPos aimed = BlockPos.containing(origin.getX() + at[0] + dir[0], y, origin.getZ() + at[1] + dir[1]);
        if (!aimed.equals(out.get(0))) out.add(aimed);
        return out;
    }

    /** Model point to an offset from the main block's corner. Same turn as the blockstate. */
    public static double[] modelToWorld(Direction facing, double mx, double mz) {
        double dx = mx - 0.5, dz = mz - 0.5;
        return switch (facing) {
            case EAST -> new double[]{0.5 - dz, 0.5 + dx};
            case SOUTH -> new double[]{0.5 - dx, 0.5 - dz};
            case WEST -> new double[]{0.5 + dz, 0.5 - dx};
            default -> new double[]{mx, mz};
        };
    }

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
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return collision(state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return collision(state);
    }

    private static VoxelShape collision(BlockState state) {
        Direction facing = state.getValue(FACING);
        int height = state.getValue(HEIGHT);
        double x0 = (MAST_X - 0.14) * 16;
        double x1 = (MAST_X + 0.14) * 16;
        double z0 = (MAST_Z - 0.14) * 16;
        double z1 = (MAST_Z + 0.14) * 16;
        double head = height * 16.0;
        VoxelShape mast = rotated(new double[]{x0, MAST_BASE * 16, z0, x1, head, z1}, facing);
        VoxelShape lamps = rotated(new double[]{MAST_X * 16 - 9, head - 6, MAST_Z * 16 - 9, MAST_X * 16 + 9, head + 8, MAST_Z * 16 + 9}, facing);
        return Shapes.or(rotated(BODY, facing), mast, lamps);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LightTowerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide && type == ModRegistry.LIGHT_TOWER_BE.get()
                ? (l, p, s, be) -> LightTowerBlockEntity.serverTick(l, p, s, (LightTowerBlockEntity) be) : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof LightTowerBlockEntity be) be.refreshLights();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof LightTowerBlockEntity be) {
            be.clearLights();
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof DyeItem) {
            return super.use(state, level, pos, player, hand, hit);
        }
        if (!level.isClientSide) {
            boolean running = !state.getValue(RUNNING);
            level.setBlock(pos, state.setValue(RUNNING, running), Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof LightTowerBlockEntity be) be.refreshLights();
            player.displayClientMessage(Component.translatable(running
                    ? "message.stagetents.light_tower_on" : "message.stagetents.light_tower_off"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(RUNNING)) return;
        Direction facing = state.getValue(FACING);
        double[] at = modelToWorld(facing, EXHAUST_X, EXHAUST_Z);
        level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                pos.getX() + at[0], pos.getY() + EXHAUST_Y, pos.getZ() + at[1], 0, 0.04, 0);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.playGenerator(level, pos));
    }
}
