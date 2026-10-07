package com.nailec.stagetents.furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Invisible cell filling the footprint of a multi-block prop around its main block. The main block places and
 * removes them; they can't be aimed at, so the prop is always broken through its main block.
 */
public class PropPartBlock extends Block {
    /** Collision of the cell: a full block, a tall barrier panel, a one-block wall, or a pole. */
    public enum Form implements StringRepresentable {
        FULL("full"), PANEL_X("panel_x"), PANEL_Z("panel_z"), WALL_X("wall_x"), WALL_Z("wall_z"), POLE("pole"),
        TABLE_X("table_x"), TABLE_Z("table_z"),
        FOLD_X("fold_x"), FOLD_Z("fold_z"),
        FOLD_END_X("fold_end_x"), FOLD_END_Z("fold_end_z"),
        CANNON_SIDE_N("cannon_side_n"), CANNON_SIDE_E("cannon_side_e"),
        CANNON_SIDE_S("cannon_side_s"), CANNON_SIDE_W("cannon_side_w"),
        CANNON_NOSE_N("cannon_nose_n"), CANNON_NOSE_E("cannon_nose_e"),
        CANNON_NOSE_S("cannon_nose_s"), CANNON_NOSE_W("cannon_nose_w");

        private final String name;

        Form(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final EnumProperty<Form> FORM = EnumProperty.create("form", Form.class);
    private static final VoxelShape PANEL_X = Block.box(0, 0, 7, 16, 24, 9);
    private static final VoxelShape PANEL_Z = Block.box(7, 0, 0, 9, 24, 16);
    private static final VoxelShape WALL_X = Block.box(0, 0, 6, 16, 16, 10);
    private static final VoxelShape WALL_Z = Block.box(6, 0, 0, 10, 16, 16);
    private static final VoxelShape POLE = Block.box(6, 0, 6, 10, 16, 10);
    /** Picnic table slice: top and the two benches. Length runs along X. */
    private static final VoxelShape TABLE_X = Shapes.or(
            Block.box(0, 16.8, 0.8, 16, 18.3, 15.2),
            Block.box(0, 9.9, -6.4, 16, 11.0, -0.7),
            Block.box(0, 9.9, 16.7, 16, 11.0, 22.4));
    /** Middle block of the folding table: the top only. Length along X. */
    private static final VoxelShape FOLD_X = Block.box(0, 16.85, -0.4, 16, 17.76, 16.4);
    /** End block: the rest of the top and that end's legs. Length along X. */
    private static final VoxelShape FOLD_END_X = Shapes.or(
            Block.box(0, 16.85, -0.4, 13.6, 17.76, 16.4),
            Block.box(5.9, 0, 1.3, 12.6, 16.85, 14.7));
    /** Middle block with the length running along Z. */
    private static final VoxelShape FOLD_Z = Block.box(-0.4, 16.85, 0, 16.4, 17.76, 16);
    /** End block with the length running along Z. */
    private static final VoxelShape FOLD_END_Z = Shapes.or(
            Block.box(-0.4, 16.85, 0, 16.4, 17.76, 13.6),
            Block.box(1.3, 0, 5.9, 14.7, 16.85, 12.6));
    /** Same slice with the length running along Z. */
    private static final VoxelShape TABLE_Z = Shapes.or(
            Block.box(0.8, 16.8, 0, 15.2, 18.3, 16),
            Block.box(-6.4, 9.9, 0, -0.7, 11.0, 16),
            Block.box(16.7, 9.9, 0, 22.4, 11.0, 16));
    /** Right-hand half of the water-cannon skid, one shape per facing of the main block. */
    private static final VoxelShape CANNON_SIDE_N = Block.box(-0.8, 0, 0.3, 12.5, 6.7, 15.7);
    private static final VoxelShape CANNON_SIDE_E = Block.box(0.3, 0, -0.8, 15.7, 6.7, 12.5);
    private static final VoxelShape CANNON_SIDE_S = Block.box(3.5, 0, 0.3, 16.8, 6.7, 15.7);
    private static final VoxelShape CANNON_SIDE_W = Block.box(0.3, 0, 3.5, 15.7, 6.7, 16.8);
    /** Mouth of the barrel, in the block the cannon faces. */
    private static final VoxelShape CANNON_NOSE_N = Block.box(-0.8, 6.4, 6.7, 16.8, 33.6, 16.8);
    private static final VoxelShape CANNON_NOSE_E = Block.box(-0.8, 6.4, -0.8, 9.3, 33.6, 16.8);
    private static final VoxelShape CANNON_NOSE_S = Block.box(-0.8, 6.4, -0.8, 16.8, 33.6, 9.3);
    private static final VoxelShape CANNON_NOSE_W = Block.box(6.7, 6.4, -0.8, 16.8, 33.6, 16.8);

    public PropPartBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FORM, Form.FULL));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORM);
    }

    @Override
    @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        Form form = state.getValue(FORM);
        VoxelShape local = switch (form) {
            case PANEL_X -> PANEL_X;
            case PANEL_Z -> PANEL_Z;
            case WALL_X -> WALL_X;
            case WALL_Z -> WALL_Z;
            case POLE -> POLE;
            case TABLE_X -> TABLE_X;
            case TABLE_Z -> TABLE_Z;
            case FOLD_X -> FOLD_X;
            case FOLD_Z -> FOLD_Z;
            case FOLD_END_X -> FOLD_END_X;
            case FOLD_END_Z -> FOLD_END_Z;
            case CANNON_SIDE_N -> CANNON_SIDE_N;
            case CANNON_SIDE_E -> CANNON_SIDE_E;
            case CANNON_SIDE_S -> CANNON_SIDE_S;
            case CANNON_SIDE_W -> CANNON_SIDE_W;
            case CANNON_NOSE_N -> CANNON_NOSE_N;
            case CANNON_NOSE_E -> CANNON_NOSE_E;
            case CANNON_NOSE_S -> CANNON_NOSE_S;
            case CANNON_NOSE_W -> CANNON_NOSE_W;
            default -> Shapes.block();
        };
        Direction facing = cannonFacing(form);
        if (level == null || facing == null) return local;
        boolean nose = form == Form.CANNON_NOSE_N || form == Form.CANNON_NOSE_E || form == Form.CANNON_NOSE_S || form == Form.CANNON_NOSE_W;
        BlockPos main = nose ? pos.relative(facing.getOpposite()) : pos.relative(facing.getCounterClockWise());
        BlockState cannon = level.getBlockState(main);
        if (!(cannon.getBlock() instanceof WaterCannonBlock)) return local;
        int yaw = level.getBlockEntity(main) instanceof WaterCannonBlockEntity be ? be.yaw() : 0;
        VoxelShape barrel = WaterCannonBlock.barrelShape(cannon.getValue(WaterCannonBlock.PITCH), yaw, facing);
        return Shapes.or(local, barrel.move(main.getX() - pos.getX(), 0, main.getZ() - pos.getZ()));
    }

    private static Direction cannonFacing(Form form) {
        return switch (form) {
            case CANNON_SIDE_E, CANNON_NOSE_E -> Direction.EAST;
            case CANNON_SIDE_S, CANNON_NOSE_S -> Direction.SOUTH;
            case CANNON_SIDE_W, CANNON_NOSE_W -> Direction.WEST;
            case CANNON_SIDE_N, CANNON_NOSE_N -> Direction.NORTH;
            default -> null;
        };
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return false;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }
}
