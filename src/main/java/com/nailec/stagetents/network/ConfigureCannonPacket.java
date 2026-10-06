package com.nailec.stagetents.network;

import com.nailec.stagetents.furniture.WaterCannonBlock;
import com.nailec.stagetents.furniture.WaterCannonBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: tilt, pan and plume length chosen in the water cannon screen. */
public record ConfigureCannonPacket(BlockPos pos, int pitch, int range, int yaw) {
    private static final double MAX_DISTANCE_SQR = 12 * 12;

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeByte(pitch);
        buf.writeByte(range);
        buf.writeByte(WaterCannonBlockEntity.yawIndex(yaw));
    }

    public static ConfigureCannonPacket decode(FriendlyByteBuf buf) {
        return new ConfigureCannonPacket(buf.readBlockPos(), buf.readUnsignedByte(), buf.readUnsignedByte(),
                WaterCannonBlockEntity.yawFromIndex(buf.readUnsignedByte()));
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null || !player.mayBuild()) return;
        if (!player.level().isLoaded(pos) || player.distanceToSqr(pos.getCenter()) > MAX_DISTANCE_SQR) return;
        BlockState state = player.level().getBlockState(pos);
        if (!(state.getBlock() instanceof WaterCannonBlock)) return;
        WaterCannonBlock.applyPitch(player.level(), pos, state, pitch);
        if (player.level().getBlockEntity(pos) instanceof WaterCannonBlockEntity cannon) cannon.configure(yaw, range);
    }
}
