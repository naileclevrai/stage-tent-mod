package com.nailec.stagetents.furniture;

import com.nailec.stagetents.client.ClientHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.List;

/**
 * A theatre curtain hung from a pipe. Place the blocks in a line at pipe height. The whole run is one cloth:
 * closed, tied back with ropes, or parted along the track. Dye recolours the cloth. The ropes stay gold.
 */
public class CurtainBlock extends ConnectedFurnitureBlock implements EntityBlock {
    /** Each step is 25 cm. 16 is a 4 m curtain, 48 is 12 m. */
    public static final int MIN_DROP = 4;
    public static final int MAX_DROP = 48;
    public static final int MAX_OPEN = 8;

    public static final IntegerProperty DROP = IntegerProperty.create("drop", MIN_DROP, MAX_DROP);
    public static final IntegerProperty OPEN = IntegerProperty.create("open", 0, MAX_OPEN);
    public static final EnumProperty<Mode> MODE = EnumProperty.create("mode", Mode.class);

    public CurtainBlock(Properties props) {
        super(props, Spec.of(true, DyeColor.RED, new double[]{0, 14, 6, 16, 16, 10}), "curtain");
        registerDefaultState(defaultBlockState()
                .setValue(DROP, 16)
                .setValue(OPEN, 6)
                .setValue(MODE, Mode.CLOSED));
    }

    public enum Mode implements StringRepresentable {
        CLOSED("closed"),
        TIED("tied"),
        PARTED("parted");

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    /** How far the hem hangs below the pipe, in blocks. */
    public static float length(int drop) {
        return net.minecraft.util.Mth.clamp(drop, MIN_DROP, MAX_DROP) * 0.25F;
    }

    public static String label(int drop) {
        int cm = Math.round(length(drop) * 100F);
        if (cm < 100) return cm + " cm";
        if (cm % 100 == 0) return (cm / 100) + " m";
        if (cm % 10 == 0) return (cm / 100) + "." + ((cm % 100) / 10) + " m";
        return (cm / 100) + "." + (cm % 100 < 10 ? "0" : "") + (cm % 100) + " m";
    }

    /** How many blocks of cloth stay piled at each jamb when the curtain is parted by {@code amount} (0 to 1). */
    public static float partedStack(int span, float amount) {
        float s = Math.max(1, span);
        float a = net.minecraft.util.Mth.clamp(amount, 0F, 1F);
        float floor = Math.min(0.42F, s * 0.22F);
        return Math.max(floor, s * 0.5F * (1F - 0.74F * a));
    }

    /** Widest part of a tied-back panel, at the pipe. */
    public static float tiedBudget(int span) {
        float s = Math.max(1, span);
        float wanted = s * 0.20F;
        float floor = Math.min(0.62F, s * 0.36F);
        return net.minecraft.util.Mth.clamp(wanted, floor, 1.25F);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DROP, OPEN, MODE);
    }

    @Override
    @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        VoxelShape body = getCollisionShape(state, level, pos, ctx);
        if (!body.isEmpty()) return body;
        return rotated(new double[]{0, 14, 6, 16, 16, 10}, state.getValue(FACING));
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        double y0 = 0.94 - length(state.getValue(DROP));
        double y1 = 0.98;
        Direction facing = state.getValue(FACING);
        if (state.getValue(MODE) == Mode.CLOSED || !(level instanceof Level world)) {
            return slice(facing, 0, 1, y0, y1);
        }
        List<BlockPos> run = Drapes.run(world, pos, state);
        int index = run.indexOf(pos);
        if (index < 0 || run.isEmpty()) return slice(facing, 0, 1, y0, y1);
        float stack = state.getValue(MODE) == Mode.TIED
                ? tiedBudget(run.size())
                : partedStack(run.size(), state.getValue(OPEN) / (float) MAX_OPEN);
        return coverage(facing, index, run.size(), stack, y0, y1);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    /** Cloth of {@code stack} blocks at each end of a run, clipped to block {@code index}. */
    private static VoxelShape coverage(Direction facing, int index, int span, float stack, double y0, double y1) {
        float left1 = Math.min(1F, Math.max(0F, stack - index));
        float rightStart = span - stack;
        float right0 = 1F;
        float right1 = 0F;
        if (index + 1F > rightStart) {
            right0 = Math.max(0F, rightStart - index);
            right1 = 1F;
        }
        VoxelShape shape = Shapes.empty();
        if (left1 > 0.02F) shape = Shapes.or(shape, slice(facing, 0, left1, y0, y1));
        if (right1 - right0 > 0.02F && right0 < 0.98F) shape = Shapes.or(shape, slice(facing, right0, right1, y0, y1));
        return shape;
    }

    private static VoxelShape slice(Direction facing, float x0, float x1, double y0, double y1) {
        if (x1 - x0 < 0.02F) return Shapes.empty();
        return rotated(new double[]{x0 * 16F, y0 * 16F, 6, x1 * 16F, y1 * 16F, 10}, facing);
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
        if (held.isEmpty()) {
            if (level.isClientSide) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openCurtainScreen(pos));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.use(state, level, pos, player, hand, hit);
    }

    /** Menu: one setting for the whole run, so both panels stay matched. */
    public static void apply(Level level, BlockPos pos, BlockState state, int mode, int drop, int open) {
        int modeIndex = net.minecraft.util.Mth.clamp(mode, 0, Mode.values().length - 1);
        Mode next = Mode.values()[modeIndex];
        int d = net.minecraft.util.Mth.clamp(drop, MIN_DROP, MAX_DROP);
        int o = net.minecraft.util.Mth.clamp(open, 0, MAX_OPEN);
        for (BlockPos p : Drapes.run(level, pos, state)) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof CurtainBlock) {
                level.setBlock(p, s.setValue(MODE, next).setValue(DROP, d).setValue(OPEN, o), Block.UPDATE_ALL);
            }
        }
    }
}
