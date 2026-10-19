package com.nailec.stagetents.furniture;

import com.nailec.stagetents.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Invisible cell of a machine. It knows where its main block is and borrows the main block's shape, so the
 * platform and the forks collide in whatever cell they have reached. Hitting or using it acts on the machine.
 */
public class MachinePartBlock extends Block {
    public static final int REACH = 4;
    public static final IntegerProperty OX = IntegerProperty.create("ox", 0, REACH * 2);
    public static final IntegerProperty OY = IntegerProperty.create("oy", 0, 15);
    public static final IntegerProperty OZ = IntegerProperty.create("oz", 0, REACH * 2);

    public MachinePartBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(OX, REACH).setValue(OY, 0).setValue(OZ, REACH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OX, OY, OZ);
    }

    /** Part for the cell at {@code offset} from the main block, or null if it is out of reach. */
    public static BlockState stateFor(BlockPos offset) {
        int x = offset.getX() + REACH, y = offset.getY(), z = offset.getZ() + REACH;
        if (x < 0 || x > REACH * 2 || z < 0 || z > REACH * 2 || y < 0 || y > 15) return null;
        return ModRegistry.MACHINE_PART.get().defaultBlockState().setValue(OX, x).setValue(OY, y).setValue(OZ, z);
    }

    /** Main block of the part at {@code pos}, or null if there is no part there. */
    public static BlockPos mainOf(BlockGetter level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (!(s.getBlock() instanceof MachinePartBlock)) return null;
        return pos.offset(REACH - s.getValue(OX), -s.getValue(OY), REACH - s.getValue(OZ));
    }

    private static BlockPos main(BlockState state, BlockPos pos) {
        return pos.offset(REACH - state.getValue(OX), -state.getValue(OY), REACH - state.getValue(OZ));
    }

    @Override
    @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        BlockPos main = main(state, pos);
        if (level.getBlockEntity(main) instanceof MachineBlockEntity be) {
            return be.shape().move(main.getX() - pos.getX(), main.getY() - pos.getY(), main.getZ() - pos.getZ());
        }
        return Shapes.empty();
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return getShape(state, level, pos, ctx);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockPos main = main(state, pos);
        BlockState ms = level.getBlockState(main);
        if (!(ms.getBlock() instanceof MachineBlock)) return InteractionResult.PASS;
        return ms.use(level, player, hand, new BlockHitResult(hit.getLocation(), hit.getDirection(), main, hit.isInside()));
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockPos main = main(state, pos);
        if (!level.isClientSide && level.getBlockState(main).getBlock() instanceof MachineBlock) {
            level.destroyBlock(main, !player.isCreative(), player);
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    @SuppressWarnings("deprecation")
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        BlockPos main = main(state, pos);
        BlockState ms = level.getBlockState(main);
        return ms.getBlock() instanceof MachineBlock ? ms.getDestroyProgress(player, level, main) : 1.0F;
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        BlockState ms = level.getBlockState(main(state, pos));
        return ms.getBlock() instanceof MachineBlock ? new ItemStack(ms.getBlock()) : ItemStack.EMPTY;
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
