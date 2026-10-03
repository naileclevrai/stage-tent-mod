package com.nailec.stagetents.furniture;

import com.nailec.stagetents.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Invisible mount used to sit on chairs and bleachers. Disappears as soon as nobody sits on it. */
public class SeatEntity extends Entity {
    public SeatEntity(EntityType<? extends SeatEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    /**
     * Sits {@code player} on the seat of the block at {@code pos}, offset from the block centre by (ox, oz); false if
     * somebody already sits there.
     */
    public static boolean sit(Level level, BlockPos pos, double ox, double seatHeight, double oz, Player player) {
        if (!level.getEntitiesOfClass(SeatEntity.class, new AABB(pos)).isEmpty()) return false;
        SeatEntity seat = new SeatEntity(ModRegistry.SEAT.get(), level);
        seat.setPos(pos.getX() + 0.5 + ox, pos.getY() + seatHeight, pos.getZ() + 0.5 + oz);
        level.addFreshEntity(seat);
        return player.startRiding(seat);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && (getPassengers().isEmpty() || level().getBlockState(blockPosition()).isAir())) {
            discard();
        }
    }

    @Override
    public double getPassengersRidingOffset() {
        return -0.05;
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        return new Vec3(getX(), blockPosition().getY() + 1.0, getZ());
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
