package com.nailec.stagetents.network;

import com.nailec.stagetents.furniture.CableRampBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client to server: channel count chosen on a cable ramp. */
public record ConfigureCableRampPacket(BlockPos pos, int channels) {
    private static final double MAX_DISTANCE_SQR = 12 * 12;

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeByte(channels);
    }

    public static ConfigureCableRampPacket decode(FriendlyByteBuf buf) {
        return new ConfigureCableRampPacket(buf.readBlockPos(), buf.readUnsignedByte());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null || !player.mayBuild()) return;
        if (!player.level().isLoaded(pos) || player.distanceToSqr(pos.getCenter()) > MAX_DISTANCE_SQR) return;
        BlockState state = player.level().getBlockState(pos);
        if (!(state.getBlock() instanceof CableRampBlock)) return;
        CableRampBlock.applyChannels(player.level(), pos, state, Mth.clamp(channels, 1, 5));
    }
}
