package com.nailec.stagetents.furniture;

import com.nailec.stagetents.client.ClientHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.ArrayList;
import java.util.List;

/** A frise or a pendrillon run: neighbours of the same block, facing the same way. */
public final class Drapes {
    private Drapes() {}

    public static List<BlockPos> run(Level level, BlockPos origin, BlockState state) {
        Direction facing = state.getValue(FurnitureBlock.FACING);
        Direction left = facing.getCounterClockWise();
        Direction right = facing.getClockWise();
        Block block = state.getBlock();
        BlockPos start = origin;
        for (int i = 0; i < 48; i++) {
            BlockPos next = start.relative(left);
            if (!continues(level, next, block, facing)) break;
            start = next;
        }
        List<BlockPos> out = new ArrayList<>();
        BlockPos p = start;
        for (int i = 0; i < 64; i++) {
            out.add(p);
            BlockPos next = p.relative(right);
            if (!continues(level, next, block, facing)) break;
            p = next;
        }
        return out;
    }

    private static boolean continues(Level level, BlockPos pos, Block block, Direction facing) {
        BlockState other = level.getBlockState(pos);
        return other.getBlock() == block && other.getValue(FurnitureBlock.FACING) == facing;
    }

    /** One dye recolours the whole run, so a border does not turn into a patchwork. */
    static boolean dye(Level level, BlockPos origin, BlockState state, DyeColor color) {
        List<BlockPos> run = run(level, origin, state);
        boolean changed = false;
        for (BlockPos p : run) {
            BlockState s = level.getBlockState(p);
            if (s.getValue(FurnitureBlock.COLOR) == color) continue;
            level.setBlock(p, s.setValue(FurnitureBlock.COLOR, color), Block.UPDATE_ALL);
            changed = true;
        }
        return changed;
    }

    /** Right-click or the wrench: the same settings screen as the other stage pieces. */
    static InteractionResult openMenu(Level level, BlockPos pos) {
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openDrapeScreen(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
