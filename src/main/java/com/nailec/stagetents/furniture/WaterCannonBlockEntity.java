package com.nailec.stagetents.furniture;

import com.nailec.stagetents.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Present so the jet can be spawned every tick. Tilt is on the block; pan and plume length are kept here. */
public class WaterCannonBlockEntity extends BlockEntity {
    /** 1 is a short plume (2 m), 8 reaches about 16 m. */
    public static final int RANGE_MAX = 8;
    /** Pan runs from -180° to 180° in 15° steps. Positive turns the mouth to the right of the skid. */
    public static final int YAW_MIN = -180;
    public static final int YAW_MAX = 180;
    public static final int YAW_STEP = 15;

    private int range = 4;
    private int yaw;

    public WaterCannonBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.WATER_CANNON_BE.get(), pos, state);
    }

    public int range() {
        return range;
    }

    public int yaw() {
        return yaw;
    }

    /** Nearest pan step, kept inside the menu's range. */
    public static int snapYaw(int degrees) {
        int snapped = Math.round(degrees / (float) YAW_STEP) * YAW_STEP;
        return Mth.clamp(snapped, YAW_MIN, YAW_MAX);
    }

    public static int yawIndex(int degrees) {
        return (snapYaw(degrees) - YAW_MIN) / YAW_STEP;
    }

    public static int yawFromIndex(int index) {
        return Mth.clamp(index, 0, (YAW_MAX - YAW_MIN) / YAW_STEP) * YAW_STEP + YAW_MIN;
    }

    /** How far the plume is meant to carry, in metres. */
    public static int metres(int range) {
        return Mth.clamp(range, 1, RANGE_MAX) * 2;
    }

    public void configure(int yaw, int range) {
        this.yaw = snapYaw(yaw);
        this.range = Mth.clamp(range, 1, RANGE_MAX);
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state) {
        if (!state.getValue(WaterCannonBlock.RUNNING)) return;
        int range = 4;
        int yaw = 0;
        if (level.getBlockEntity(pos) instanceof WaterCannonBlockEntity be) {
            range = be.range;
            yaw = be.yaw;
        }
        Direction facing = state.getValue(FurnitureBlock.FACING);
        double[] aim = WaterCannonBlock.aim(state.getValue(WaterCannonBlock.PITCH), yaw);
        double[] w = WaterCannonBlock.modelToWorld(facing, aim[0], aim[2]);
        double[] d = WaterCannonBlock.modelDir(facing, aim[3], aim[5]);
        double c = aim[4];
        double x = pos.getX() + w[0] + d[0] * 0.25;
        double y = pos.getY() + aim[1] + c * 0.25;
        double z = pos.getZ() + w[1] + d[1] * 0.25;
        double speed = 0.08 + range * 0.07;
        RandomSource random = level.random;
        int count = 2 + range / 2;
        for (int i = 0; i < count; i++) {
            double v = speed * (0.65 + random.nextDouble() * 0.5);
            double jx = (random.nextDouble() - 0.5) * 0.06;
            double jy = (random.nextDouble() - 0.5) * 0.04;
            double jz = (random.nextDouble() - 0.5) * 0.06;
            level.addParticle(i == 0 ? ParticleTypes.WHITE_ASH : ParticleTypes.CLOUD,
                    x, y, z, d[0] * v + jx, c * v + jy - 0.02, d[1] * v + jz);
        }
        if (random.nextFloat() < 0.45F + range * 0.05F) {
            level.addParticle(ParticleTypes.FALLING_WATER, x, y, z, d[0] * speed * 0.6, c * speed * 0.25, d[1] * speed * 0.6);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Range", range);
        tag.putInt("Yaw", yaw);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        range = tag.contains("Range") ? Mth.clamp(tag.getInt("Range"), 1, RANGE_MAX) : 4;
        yaw = tag.contains("Yaw") ? snapYaw(tag.getInt("Yaw")) : 0;
    }

    /** The mouth sweeps well outside the skid block once the head is panned. */
    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(1.8, 2.2, 1.8);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
