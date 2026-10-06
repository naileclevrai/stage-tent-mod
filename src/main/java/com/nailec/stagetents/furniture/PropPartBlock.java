package com.nailec.stagetents.furniture;

import net.minecraft.core.BlockPos;
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
        FULL("full"), PANEL_X("panel_x"), PANEL_Z("panel_z"), WALL_X("wall_x"), WALL_Z("wall_z"), POLE("pole");

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
        return switch (state.getValue(FORM)) {
            case PANEL_X -> PANEL_X;
            case PANEL_Z -> PANEL_Z;
            case WALL_X -> WALL_X;
            case WALL_Z -> WALL_Z;
            case POLE -> POLE;
            default -> Shapes.block();
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
