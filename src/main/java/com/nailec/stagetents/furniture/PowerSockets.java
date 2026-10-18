package com.nailec.stagetents.furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Sockets on the power blocks, in the same north-facing model space as the meshes.
 * A cable always leaves the connector, a few centimetres proud of the cap or the pins.
 * The blue marker sits just above that point, nudged toward the front (−Z) so it is not inside the lid.
 */
public final class PowerSockets {
    /** Longest lead, from connector to connector. */
    public static final double REACH = 20.0;
    private static final double MARKER_UP = 0.04;
    private static final double MARKER_OUT = 0.02;

    public enum Role {
        /** Generator or light-tower outlet. Feeds one inlet. */
        OUTPUT,
        /** Front socket on a distro or a rack. Feeds one inlet. */
        OUTLET,
        /** Single inlet. Accepts one cable. */
        INPUT;

        public boolean feeds() {
            return this != INPUT;
        }
    }

    /** Model-space connector. {@code pick} is how far off the aim can be and still count as this socket. */
    public record Socket(Role role, double x, double y, double z, double pick) {}

    public record Hit(BlockPos pos, int index, Socket socket, Vec3 anchor) {}

    private static final List<Socket> GENERATOR = generator();
    private static final List<Socket> DISTRO = distro();
    private static final List<Socket> RACK = rack();
    private static final List<Socket> TOWER = tower();

    private PowerSockets() {}

    @Nullable
    public static List<Socket> of(BlockState state) {
        if (state.getBlock() instanceof GeneratorBlock) return GENERATOR;
        if (state.getBlock() instanceof PowerDistroBlock) return DISTRO;
        if (state.getBlock() instanceof PowerRackBlock) return RACK;
        if (state.getBlock() instanceof LightTowerBlock) return TOWER;
        return null;
    }

    /**
     * Main block of a power machine, or the main block that owns this prop-part cell.
     * Empty when {@code pos} is not part of a cabled machine.
     */
    @Nullable
    public static BlockPos originOf(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (of(state) != null) return pos.immutable();
        if (!(state.getBlock() instanceof PropPartBlock)) return null;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos at = pos.offset(dx, dy, dz);
                    BlockState there = level.getBlockState(at);
                    if (!(there.getBlock() instanceof MultiPropBlock multi) || of(there) == null) continue;
                    for (BlockPos part : multi.parts(at, there.getValue(FurnitureBlock.FACING))) {
                        if (part.equals(pos)) return at.immutable();
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    public static PowerLinks cables(Level level, BlockPos pos) {
        return cables(level.getBlockEntity(pos));
    }

    @Nullable
    public static PowerLinks cables(@Nullable BlockEntity be) {
        if (be instanceof PowerNodeBlockEntity node) return node.cables();
        if (be instanceof LightTowerBlockEntity tower) return tower.cables();
        return null;
    }

    /** Creates the node the first time a cable is hung on a block that did not have one yet. */
    @Nullable
    public static PowerLinks ensure(Level level, BlockPos pos) {
        PowerLinks existing = cables(level, pos);
        if (existing != null) return existing;
        if (level.isClientSide) return null;
        BlockState state = level.getBlockState(pos);
        if (of(state) == null || !(state.getBlock() instanceof net.minecraft.world.level.block.EntityBlock entity)) return null;
        BlockEntity created = entity.newBlockEntity(pos, state);
        if (created == null) return null;
        level.setBlockEntity(created);
        return cables(created);
    }

    /** North-facing model point, turned the same way as the block shapes. */
    public static Vec3 world(BlockPos origin, Direction facing, double x, double y, double z) {
        double dx = x - 0.5;
        double dz = z - 0.5;
        double wx;
        double wz;
        switch (facing) {
            case EAST -> {
                wx = 0.5 - dz;
                wz = 0.5 + dx;
            }
            case SOUTH -> {
                wx = 0.5 - dx;
                wz = 0.5 - dz;
            }
            case WEST -> {
                wx = 0.5 + dz;
                wz = 0.5 - dx;
            }
            default -> {
                wx = x;
                wz = z;
            }
        }
        return new Vec3(origin.getX() + wx, origin.getY() + y, origin.getZ() + wz);
    }

    public static Vec3 anchor(BlockPos origin, Direction facing, Socket socket) {
        return world(origin, facing, socket.x, socket.y, socket.z);
    }

    /** Small point drawn just above the connector while the cabling tool is held. */
    public static Vec3 marker(BlockPos origin, Direction facing, Socket socket) {
        return world(origin, facing, socket.x, socket.y + MARKER_UP, socket.z - MARKER_OUT);
    }

    /**
     * Socket under the crosshair.
     * {@code blockAt} is where the vanilla outline hit landed, and {@code blockPoint} is that hit's location.
     * A socket just behind its own face still counts. A socket behind a different block does not.
     */
    @Nullable
    public static Hit pick(Level level, Player player, double reach, @Nullable Vec3 blockPoint, @Nullable BlockPos blockAt) {
        Vec3 eye = player.getEyePosition();
        Vec3 dir = player.getLookAngle();
        if (dir.lengthSqr() < 1.0E-6) return null;
        dir = dir.normalize();
        double blockDistance = blockPoint == null ? Double.MAX_VALUE : eye.distanceTo(blockPoint);
        BlockPos blockMain = blockAt == null ? null : originOf(level, blockAt);
        Hit best = null;
        double bestMiss = Double.MAX_VALUE;
        double bestAlong = Double.MAX_VALUE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int cx = player.blockPosition().getX();
        int cy = player.blockPosition().getY();
        int cz = player.blockPosition().getZ();
        for (int y = -4; y <= 5; y++) {
            for (int x = -8; x <= 8; x++) {
                for (int z = -8; z <= 8; z++) {
                    cursor.set(cx + x, cy + y, cz + z);
                    BlockState state = level.getBlockState(cursor);
                    List<Socket> sockets = of(state);
                    if (sockets == null) continue;
                    Direction facing = state.getValue(FurnitureBlock.FACING);
                    BlockPos origin = cursor.immutable();
                    boolean ownFace = origin.equals(blockMain);
                    for (int i = 0; i < sockets.size(); i++) {
                        Socket socket = sockets.get(i);
                        Vec3 at = anchor(origin, facing, socket);
                        Vec3 dot = marker(origin, facing, socket);
                        double missAt = miss(eye, dir, at);
                        double missDot = miss(eye, dir, dot);
                        double miss = Math.min(missAt, missDot);
                        if (miss > socket.pick()) continue;
                        Vec3 aimed = missAt <= missDot ? at : dot;
                        double along = aimed.subtract(eye).dot(dir);
                        if (along < 0.25 || along > reach) continue;
                        double slack = ownFace ? 0.55 : 0.08;
                        if (eye.distanceTo(at) > blockDistance + slack) continue;
                        if (miss < bestMiss - 1.0E-4 || (Math.abs(miss - bestMiss) <= 1.0E-4 && along < bestAlong)) {
                            best = new Hit(origin, i, socket, at);
                            bestMiss = miss;
                            bestAlong = along;
                        }
                    }
                }
            }
        }
        return best;
    }

    public static void collect(Level level, BlockPos center, int radius, List<BlockPos> out) {
        out.clear();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = -3; y <= 4; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    cursor.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (of(level.getBlockState(cursor)) != null) out.add(cursor.immutable());
                }
            }
        }
    }

    private static double miss(Vec3 eye, Vec3 dir, Vec3 point) {
        Vec3 rel = point.subtract(eye);
        double along = rel.dot(dir);
        if (along < 0) return Double.MAX_VALUE;
        return point.distanceTo(eye.add(dir.scale(along)));
    }

    /** Four CEE sockets on the control bay. Mouth of {@code _socket} is at z ≈ −0.003; the lead starts just proud of it. */
    private static List<Socket> generator() {
        List<Socket> list = new ArrayList<>();
        double z = -0.020;
        list.add(new Socket(Role.OUTPUT, 1.14, 0.66, z, 0.070));
        list.add(new Socket(Role.OUTPUT, 1.30, 0.66, z, 0.070));
        list.add(new Socket(Role.OUTPUT, 1.46, 0.66, z, 0.070));
        list.add(new Socket(Role.OUTPUT, 1.66, 0.68, z, 0.070));
        return List.copyOf(list);
    }

    /**
     * Two large red outlets, twelve small blue ones, and the 5-pin plug on the coiled input lead.
     * Plug pin tip is the transformed end of {@code _distro_plug} at (0.34, 0.78, 0.36).
     */
    private static List<Socket> distro() {
        List<Socket> list = new ArrayList<>();
        list.add(new Socket(Role.INPUT, 0.288, 0.857, 0.249, 0.080));
        double z = 0.185;
        list.add(new Socket(Role.OUTLET, 0.26, 0.47, z, 0.060));
        list.add(new Socket(Role.OUTLET, 0.26, 0.28, z, 0.060));
        for (double y : new double[]{0.50, 0.38, 0.26}) {
            for (int col = 0; col < 4; col++) {
                list.add(new Socket(Role.OUTLET, 0.42 + col * 0.105, y, z, 0.050));
            }
        }
        return List.copyOf(list);
    }

    /** Open 63 A inlet on the left, then the blue and red CEE caps on the front fascia. */
    private static List<Socket> rack() {
        List<Socket> list = new ArrayList<>();
        double mouth = 0.180;
        list.add(new Socket(Role.INPUT, 0.210, 0.345, 0.170, 0.080));
        for (double y : new double[]{0.800, 0.690, 0.590}) {
            list.add(new Socket(Role.OUTLET, 0.210, y, mouth, 0.050));
        }
        for (double y : new double[]{0.790, 0.630}) {
            for (double x : new double[]{0.430, 0.545, 0.660, 0.775}) {
                list.add(new Socket(Role.OUTLET, x, y, mouth, 0.050));
            }
        }
        list.add(new Socket(Role.OUTLET, 0.455, 0.450, mouth, 0.050));
        list.add(new Socket(Role.OUTLET, 0.640, 0.458, mouth, 0.050));
        list.add(new Socket(Role.OUTLET, 0.760, 0.458, mouth, 0.050));
        return List.copyOf(list);
    }

    /** Three CEE sockets on the light tower's own generator. No inlet on the mesh. */
    private static List<Socket> tower() {
        List<Socket> list = new ArrayList<>();
        double z = 0.090;
        list.add(new Socket(Role.OUTPUT, 1.22, 0.55, z, 0.065));
        list.add(new Socket(Role.OUTPUT, 1.36, 0.55, z, 0.065));
        list.add(new Socket(Role.OUTPUT, 1.50, 0.55, z, 0.065));
        return List.copyOf(list);
    }
}
