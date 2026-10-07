package com.nailec.stagetents.furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Invisible piece of a control tower: a deck, a standard, a guard rail, a ladder, or a mix of those.
 * The tower owns every cell and removes it itself. Players do not break them.
 */
public class ScaffoldCellBlock extends Block {
    public static final BooleanProperty DECK = BooleanProperty.create("deck");
    /** 0 none, 1 minX/minZ, 2 maxX/minZ, 3 minX/maxZ, 4 maxX/maxZ. Corners are in world space. */
    public static final IntegerProperty POST = IntegerProperty.create("post", 0, 4);
    /** Bit 0 minX, bit 1 maxX, bit 2 minZ, bit 3 maxZ. */
    public static final IntegerProperty RAIL = IntegerProperty.create("rail", 0, 15);
    /** 0 none, otherwise the world face the ladder hugs: 1 minX, 2 maxX, 3 minZ, 4 maxZ. */
    public static final IntegerProperty LADDER = IntegerProperty.create("ladder", 0, 4);

    private static final VoxelShape DECK_SHAPE = Block.box(0, 14, 0, 16, 16, 16);
    private static final VoxelShape[] POSTS = {
            Shapes.empty(),
            Block.box(0, 0, 0, 4, 16, 4),
            Block.box(12, 0, 0, 16, 16, 4),
            Block.box(0, 0, 12, 4, 16, 16),
            Block.box(12, 0, 12, 16, 16, 16)
    };
    /** Deck posts keep going through the empty block above, which stays free for placed blocks. */
    private static final VoxelShape[] TALL_POSTS = {
            Shapes.empty(),
            Block.box(0, 0, 0, 4, 32, 4),
            Block.box(12, 0, 0, 16, 32, 4),
            Block.box(0, 0, 12, 4, 32, 16),
            Block.box(12, 0, 12, 16, 32, 16)
    };
    /** Rails sit on the deck and rise into the block above. That block stays air. */
    private static final VoxelShape[] RAILS = {
            Block.box(0, 16, 0, 2, 32, 16),
            Block.box(14, 16, 0, 16, 32, 16),
            Block.box(0, 16, 0, 16, 32, 2),
            Block.box(0, 16, 14, 16, 32, 16)
    };
    private static final VoxelShape[] LADDERS = {
            Shapes.empty(),
            Block.box(2, 0, 2, 5, 16, 14),
            Block.box(11, 0, 2, 14, 16, 14),
            Block.box(2, 0, 2, 14, 16, 5),
            Block.box(2, 0, 11, 14, 16, 14)
    };

    public ScaffoldCellBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(DECK, false).setValue(POST, 0).setValue(RAIL, 0).setValue(LADDER, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DECK, POST, RAIL, LADDER);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    private static VoxelShape shape(BlockState state) {
        VoxelShape s = Shapes.empty();
        boolean deck = state.getValue(DECK);
        if (deck) s = DECK_SHAPE;
        s = Shapes.or(s, (deck ? TALL_POSTS : POSTS)[state.getValue(POST)]);
        int rail = state.getValue(RAIL);
        for (int i = 0; i < 4; i++) if ((rail & (1 << i)) != 0) s = Shapes.or(s, RAILS[i]);
        s = Shapes.or(s, LADDERS[state.getValue(LADDER)]);
        return s.optimize();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(DECK) ? DECK_SHAPE : Shapes.empty();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shape(state);
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
        return state.getValue(LADDER) != 0;
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        return false;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return false;
    }
}
