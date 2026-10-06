package com.nailec.stagetents.network;

import com.nailec.stagetents.furniture.FriseBlock;
import com.nailec.stagetents.furniture.PendrillonBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: height or drop chosen in the drape screen. Applied to the whole run. */
public record ConfigureDrapePacket(BlockPos pos, int value) {
    private static final double MAX_DISTANCE_SQR = 12 * 12;

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeByte(value);
    }

    public static ConfigureDrapePacket decode(FriendlyByteBuf buf) {
        return new ConfigureDrapePacket(buf.readBlockPos(), buf.readUnsignedByte());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null || !player.mayBuild()) return;
        if (!player.level().isLoaded(pos) || player.distanceToSqr(pos.getCenter()) > MAX_DISTANCE_SQR) return;
        BlockState state = player.level().getBlockState(pos);
        if (state.getBlock() instanceof PendrillonBlock) PendrillonBlock.applyHeight(player.level(), pos, state, value, player);
        else if (state.getBlock() instanceof FriseBlock) FriseBlock.applyDrop(player.level(), pos, state, value);
    }
}
