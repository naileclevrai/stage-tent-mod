package com.nailec.stagetents.network;

import com.nailec.stagetents.furniture.MachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: lift step, mast tilt, load on the forks, outriggers and the extension deck. */
public record ConfigureMachinePacket(BlockPos pos, int lift, int tilt, int load, boolean outriggers, boolean deck) {
    private static final double MAX_DISTANCE_SQR = 16 * 16;

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeByte(lift);
        buf.writeByte(tilt);
        buf.writeByte(load);
        buf.writeByte((outriggers ? 1 : 0) | (deck ? 2 : 0));
    }

    public static ConfigureMachinePacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int lift = buf.readUnsignedByte();
        int tilt = buf.readByte();
        int load = buf.readUnsignedByte();
        int flags = buf.readUnsignedByte();
        return new ConfigureMachinePacket(pos, lift, tilt, load, (flags & 1) != 0, (flags & 2) != 0);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null || !player.mayBuild()) return;
        if (!player.level().isLoaded(pos) || player.distanceToSqr(pos.getCenter()) > MAX_DISTANCE_SQR) return;
        if (player.level().getBlockEntity(pos) instanceof MachineBlockEntity be
                && !be.configure(lift, tilt, load, outriggers, deck)) {
            player.displayClientMessage(Component.translatable("message.stagetents.machine.blocked"), true);
        }
    }
}
