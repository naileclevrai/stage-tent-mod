package com.nailec.stagetents.network;

import com.nailec.stagetents.furniture.CurtainBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: how a curtain run hangs. Applied to every block of the run. */
public record ConfigureCurtainPacket(BlockPos pos, int mode, int drop, int open) {
    private static final double MAX_DISTANCE_SQR = 12 * 12;

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeByte(mode);
        buf.writeByte(drop);
        buf.writeByte(open);
    }

    public static ConfigureCurtainPacket decode(FriendlyByteBuf buf) {
        return new ConfigureCurtainPacket(buf.readBlockPos(), buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null || !player.mayBuild()) return;
        if (!player.level().isLoaded(pos) || player.distanceToSqr(pos.getCenter()) > MAX_DISTANCE_SQR) return;
        BlockState state = player.level().getBlockState(pos);
        if (state.getBlock() instanceof CurtainBlock) CurtainBlock.apply(player.level(), pos, state, mode, drop, open);
    }
}
