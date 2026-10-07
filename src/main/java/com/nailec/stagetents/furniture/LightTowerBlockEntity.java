package com.nailec.stagetents.furniture;

import com.nailec.stagetents.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/** Keeps the lamp-bank pan and tilt, and the invisible lights the floods leave behind while the set is running. */
public class LightTowerBlockEntity extends BlockEntity {
    public static final int YAW_MIN = -180;
    public static final int YAW_MAX = 180;
    public static final int YAW_STEP = 15;
    public static final int TILT_MAX = 60;
    public static final int TILT_STEP = 10;

    private int yaw;
    private int tilt = 20;
    private long[] lights = new long[0];

    public LightTowerBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.LIGHT_TOWER_BE.get(), pos, state);
    }

    public int yaw() {
        return yaw;
    }

    /** Degrees the floods tilt down from the horizontal. */
    public int tilt() {
        return tilt;
    }

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

    public static int snapTilt(int degrees) {
        int snapped = Math.round(degrees / (float) TILT_STEP) * TILT_STEP;
        return Mth.clamp(snapped, 0, TILT_MAX);
    }

    public void configure(int yaw, int tilt) {
        this.yaw = snapYaw(yaw);
        this.tilt = snapTilt(tilt);
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            refreshLights();
        }
    }

    public void clearLights() {
        if (level == null || level.isClientSide) return;
        for (long saved : lights) {
            BlockPos at = BlockPos.of(saved);
            if (level.isLoaded(at) && level.getBlockState(at).is(Blocks.LIGHT)) {
                level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        lights = new long[0];
        setChanged();
    }

    /** Drops the old light blocks and puts new ones where the floods are aimed. Only replaces air. */
    public void refreshLights() {
        if (level == null || level.isClientSide) return;
        clearLights();
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof LightTowerBlock) || !state.getValue(LightTowerBlock.RUNNING)) return;
        List<Long> kept = new ArrayList<>();
        BlockState glow = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15);
        for (BlockPos at : LightTowerBlock.lightPositions(worldPosition, state, yaw, tilt)) {
            if (!level.isLoaded(at) || level.isOutsideBuildHeight(at)) continue;
            BlockState there = level.getBlockState(at);
            if (!there.isAir()) continue;
            level.setBlock(at, glow, Block.UPDATE_ALL);
            kept.add(at.asLong());
        }
        lights = kept.stream().mapToLong(Long::longValue).toArray();
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LightTowerBlockEntity be) {
        if (level.getGameTime() % 20L != 0) return;
        if (!(state.getBlock() instanceof LightTowerBlock) || !state.getValue(LightTowerBlock.RUNNING)) {
            if (be.lights.length > 0) be.clearLights();
            return;
        }
        if (be.lights.length == 0) {
            be.refreshLights();
            return;
        }
        for (long saved : be.lights) {
            BlockPos at = BlockPos.of(saved);
            if (level.isLoaded(at) && !level.getBlockState(at).is(Blocks.LIGHT)) {
                be.refreshLights();
                return;
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Yaw", yaw);
        tag.putInt("Tilt", tilt);
        tag.putLongArray("Lights", lights);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        yaw = tag.contains("Yaw") ? snapYaw(tag.getInt("Yaw")) : 0;
        tilt = tag.contains("Tilt") ? snapTilt(tag.getInt("Tilt")) : 20;
        lights = tag.getLongArray("Lights");
    }

    @Override
    public AABB getRenderBoundingBox() {
        int height = getBlockState().getBlock() instanceof LightTowerBlock
                ? getBlockState().getValue(LightTowerBlock.HEIGHT) : LightTowerBlock.MAX_HEIGHT;
        return new AABB(worldPosition.getX() - 0.4, worldPosition.getY(), worldPosition.getZ() - 0.4,
                worldPosition.getX() + 2.3, worldPosition.getY() + height + 1.4, worldPosition.getZ() + 1.4);
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
