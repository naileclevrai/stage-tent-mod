package com.nailec.stagetents.network;

import com.nailec.stagetents.block.TentBlockEntity;
import com.nailec.stagetents.tent.TentParams;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: apply the settings chosen in the tent screen. */
public record UpdateTentPacket(BlockPos pos, CompoundTag params) {
    private static final double MAX_DISTANCE_SQR = 96 * 96; // the wrench opens settings from anywhere inside a big tent

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeNbt(params);
    }

    public static UpdateTentPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        CompoundTag tag = buf.readNbt();
        return new UpdateTentPacket(pos, tag == null ? new CompoundTag() : tag);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null || !player.mayBuild()) return;
        if (!player.level().isLoaded(pos) || player.distanceToSqr(pos.getCenter()) > MAX_DISTANCE_SQR) return;
        if (player.level().getBlockEntity(pos) instanceof TentBlockEntity be) {
            TentParams p = TentParams.load(params, be.type());
            if (params.getBoolean("RescanPoles")) be.rescanStretchPoles(p);
            else be.applyParams(p);
        }
    }
}
