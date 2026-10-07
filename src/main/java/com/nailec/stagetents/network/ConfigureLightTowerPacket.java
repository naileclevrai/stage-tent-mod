package com.nailec.stagetents.network;

import com.nailec.stagetents.furniture.LightTowerBlock;
import com.nailec.stagetents.furniture.LightTowerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: mast height, lamp tilt and which way the bank turns. */
public record ConfigureLightTowerPacket(BlockPos pos, int height, int tilt, int yaw) {
    private static final double MAX_DISTANCE_SQR = 12 * 12;

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeByte(height);
        buf.writeByte(tilt);
        buf.writeByte(LightTowerBlockEntity.yawIndex(yaw));
    }

    public static ConfigureLightTowerPacket decode(FriendlyByteBuf buf) {
        return new ConfigureLightTowerPacket(buf.readBlockPos(), buf.readUnsignedByte(), buf.readUnsignedByte(),
                LightTowerBlockEntity.yawFromIndex(buf.readUnsignedByte()));
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null || !player.mayBuild()) return;
        if (!player.level().isLoaded(pos) || player.distanceToSqr(pos.getCenter()) > MAX_DISTANCE_SQR) return;
        BlockState state = player.level().getBlockState(pos);
        if (!(state.getBlock() instanceof LightTowerBlock)) return;
        LightTowerBlock.applyHeight(player.level(), pos, state, height);
        state = player.level().getBlockState(pos);
        if (player.level().getBlockEntity(pos) instanceof LightTowerBlockEntity tower) tower.configure(yaw, tilt);
    }
}
