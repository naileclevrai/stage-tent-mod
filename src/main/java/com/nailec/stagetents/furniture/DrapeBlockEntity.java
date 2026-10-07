package com.nailec.stagetents.furniture;

import com.nailec.stagetents.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Present so the cloth can be drawn as a mesh. The settings live on the block. */
public class DrapeBlockEntity extends BlockEntity {
    public DrapeBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.DRAPE_BE.get(), pos, state);
    }

    /**
     * The mesh is the whole run, and a leg rises far above its foot. Culling uses this box, including the pass that
     * draws block entities whose own chunk is off screen. A one-block box drops the cloth as soon as the foot leaves
     * the view.
     */
    @Override
    public AABB getRenderBoundingBox() {
        Level level = getLevel();
        if (level == null) return new AABB(worldPosition);
        BlockState state = getBlockState();
        List<BlockPos> run = Drapes.run(level, worldPosition, state);
        if (run.isEmpty()) return new AABB(worldPosition).inflate(1);
        int height = state.getBlock() instanceof PendrillonBlock ? state.getValue(PendrillonBlock.HEIGHT) : 1;
        double drop = 0;
        if (state.getBlock() instanceof FriseBlock) drop = FriseBlock.length(state.getValue(FriseBlock.DROP));
        else if (state.getBlock() instanceof CurtainBlock) drop = CurtainBlock.length(state.getValue(CurtainBlock.DROP));
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos p : run) {
            minX = Math.min(minX, p.getX());
            minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX());
            maxZ = Math.max(maxZ, p.getZ());
        }
        return new AABB(minX, worldPosition.getY() - drop, minZ, maxX + 1, worldPosition.getY() + height, maxZ + 1).inflate(1.5);
    }
}
