package com.nailec.stagetents.furniture;

import com.nailec.stagetents.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Settings and motion of a machine. The lift runs towards its target at the machine's speed on both sides, and
 * carries whatever stands on the platform or the forks: mobs and items on the server, the local player on the
 * client, the same way a piston moves what it pushes.
 */
public class MachineBlockEntity extends BlockEntity {
    private int lift;
    private int tilt;
    private int load;
    private boolean outriggers;
    private boolean deck;

    private double rise;
    private double prevRise;
    private float outPos;
    private float prevOut;
    private float deckPos;
    private float prevDeck;
    private boolean seeded;
    private long[] parts = new long[0];

    private VoxelShape shape;
    private long shapeKey = Long.MIN_VALUE;

    public MachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.MACHINE_BE.get(), pos, state);
    }

    public MachineKind kind() {
        return getBlockState().getBlock() instanceof MachineBlock m ? m.kind : MachineKind.FORKLIFT;
    }

    public int lift() {
        return lift;
    }

    public int tilt() {
        return tilt;
    }

    public int load() {
        return load;
    }

    public boolean outriggers() {
        return outriggers;
    }

    public boolean deck() {
        return deck;
    }

    public double rise(float partial) {
        return Mth.lerp(partial, prevRise, rise);
    }

    public float outPos(float partial) {
        return Mth.lerp(partial, prevOut, outPos);
    }

    public float deckPos(float partial) {
        return Mth.lerp(partial, prevDeck, deckPos);
    }

    public boolean moving() {
        return Math.abs(rise - target()) > 1.0e-4 || Math.abs(outPos - (outriggers ? 1 : 0)) > 1.0e-3
                || Math.abs(deckPos - (deck ? 1 : 0)) > 1.0e-3;
    }

    private double target() {
        return lift * kind().step;
    }

    private Direction facing() {
        return getBlockState().getValue(FurnitureBlock.FACING);
    }

    /** Shape relative to this block, following the current lift. */
    public VoxelShape shape() {
        int q = (int) Math.round(rise * 64);
        long key = q | ((long) load << 20) | (deck ? 1L << 24 : 0) | ((long) facing().get2DDataValue() << 26);
        if (shape == null || key != shapeKey) {
            shape = MachineBlock.shapeOf(kind(), facing(), q / 64.0, load, deck);
            shapeKey = key;
        }
        return shape;
    }

    // ------------------------------------------------------------------ settings

    /** Applies new settings if the machine has room for them. Returns false if a block is in the way. */
    public boolean configure(int lift, int tilt, int load, boolean outriggers, boolean deck) {
        MachineKind kind = kind();
        lift = Mth.clamp(lift, 0, kind.liftSteps);
        tilt = kind.hasTilt() ? Mth.clamp(tilt, MachineKind.TILT_MIN, MachineKind.TILT_MAX) : 0;
        load = kind.hasLoad() ? Mth.clamp(load, 0, MachineKind.LOADS.length - 1) : 0;
        outriggers = kind.hasOutriggers() && outriggers;
        deck = kind.hasDeck() && deck;
        if (level == null || level.isClientSide) return false;
        double target = lift * kind.step;
        Set<BlockPos> need = MachineBlock.cells(kind, facing(), Math.max(target, rise), load, deck || this.deck);
        if (!MachineBlock.fits(level, worldPosition, need)) return false;
        this.lift = lift;
        this.tilt = tilt;
        this.load = load;
        this.outriggers = outriggers;
        this.deck = deck;
        refreshParts(Math.max(target, rise));
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        return true;
    }

    /** Puts parts in the cells reached at {@code reach} and the target, and takes the others away. */
    public void refreshParts(double reach) {
        if (level == null || level.isClientSide) return;
        Set<BlockPos> need = new HashSet<>(MachineBlock.cells(kind(), facing(), reach, load, deck));
        need.addAll(MachineBlock.cells(kind(), facing(), target(), load, deck));
        List<BlockPos> placed = MachineBlock.placeParts(level, worldPosition, need);
        Set<Long> keep = new HashSet<>();
        for (BlockPos p : placed) keep.add(p.asLong());
        for (long old : parts) {
            if (keep.contains(old)) continue;
            BlockPos at = BlockPos.of(old);
            if (worldPosition.equals(MachinePartBlock.mainOf(level, at))) {
                level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        parts = keep.stream().mapToLong(Long::longValue).toArray();
        setChanged();
    }

    public void clearParts() {
        if (level == null || level.isClientSide) return;
        for (long old : parts) {
            BlockPos at = BlockPos.of(old);
            if (worldPosition.equals(MachinePartBlock.mainOf(level, at))) {
                level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        parts = new long[0];
    }

    // ------------------------------------------------------------------ motion

    public static void tick(Level level, BlockPos pos, BlockState state, MachineBlockEntity be) {
        be.prevRise = be.rise;
        be.prevOut = be.outPos;
        be.prevDeck = be.deckPos;
        MachineKind kind = be.kind();
        be.outPos = approach(be.outPos, be.outriggers ? 1 : 0, 0.04F);
        be.deckPos = approach(be.deckPos, be.deck ? 1 : 0, 0.03F);
        double target = be.target();
        if (Math.abs(be.rise - target) < 1.0e-6) return;
        double old = be.rise;
        double next = old < target ? Math.min(target, old + kind.speed) : Math.max(target, old - kind.speed);
        be.carry(level, old, next);
        be.rise = next;
        if (!level.isClientSide && next == target) {
            be.refreshParts(target);
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
        if (!level.isClientSide) be.setChanged();
    }

    private static float approach(float v, float target, float step) {
        return v < target ? Math.min(target, v + step) : Math.max(target, v - step);
    }

    /** Moves what stands on the platform or the forks along with them. */
    private void carry(Level level, double from, double to) {
        double[] f = kind().floor(from, load, deck);
        Direction facing = facing();
        double[] a = MachineBlock.modelToWorld(facing, f[0], f[1]);
        double[] b = MachineBlock.modelToWorld(facing, f[2], f[3]);
        double top = f[4];
        AABB area = new AABB(worldPosition.getX() + Math.min(a[0], b[0]), worldPosition.getY() + top - 0.2,
                worldPosition.getZ() + Math.min(a[1], b[1]), worldPosition.getX() + Math.max(a[0], b[0]),
                worldPosition.getY() + top + 0.6, worldPosition.getZ() + Math.max(a[1], b[1]));
        double dy = to - from;
        List<Entity> riders = new ArrayList<>(level.getEntities((Entity) null, area, e -> !e.isPassenger()));
        for (Entity e : riders) {
            boolean player = e instanceof Player;
            if (level.isClientSide ? !(player && ((Player) e).isLocalPlayer()) : player) continue;
            double feet = e.getY() - worldPosition.getY();
            if (feet < top - 0.15 || feet > top + 0.45) continue;
            e.setPos(e.getX(), Math.max(e.getY() + dy, worldPosition.getY() + top + dy), e.getZ());
            e.fallDistance = 0;
            if (dy > 0) e.setDeltaMovement(e.getDeltaMovement().x, Math.max(0, e.getDeltaMovement().y), e.getDeltaMovement().z);
        }
    }

    // ------------------------------------------------------------------ save

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Lift", lift);
        tag.putInt("Tilt", tilt);
        tag.putInt("Load", load);
        tag.putBoolean("Outriggers", outriggers);
        tag.putBoolean("Deck", deck);
        tag.putDouble("Rise", rise);
        tag.putLongArray("Parts", parts);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        lift = tag.getInt("Lift");
        tilt = tag.getInt("Tilt");
        load = tag.getInt("Load");
        outriggers = tag.getBoolean("Outriggers");
        deck = tag.getBoolean("Deck");
        parts = tag.getLongArray("Parts");
        boolean client = level != null && level.isClientSide;
        // The client runs the lift itself: only the first sync, or a big jump, moves it.
        if (!client || !seeded || Math.abs(tag.getDouble("Rise") - rise) > 1.5) {
            rise = prevRise = tag.getDouble("Rise");
            outPos = prevOut = outriggers ? 1 : 0;
            deckPos = prevDeck = deck ? 1 : 0;
            seeded = true;
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(4.5, 0, 4.5).expandTowards(0, 14, 0);
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
