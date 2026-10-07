package com.nailec.stagetents.network;

import com.nailec.stagetents.furniture.ControlTowerBlock;
import com.nailec.stagetents.furniture.ControlTowerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: bay counts, lifts, roof and side sheets of a control tower. */
public record ConfigureControlTowerPacket(BlockPos pos, int baysW, int baysD, int levels, boolean roof, boolean skirt) {
    private static final double MAX_DISTANCE_SQR = 24 * 24;

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeByte(baysW);
        buf.writeByte(baysD);
        buf.writeByte(levels);
        buf.writeBoolean(roof);
        buf.writeBoolean(skirt);
    }

    public static ConfigureControlTowerPacket decode(FriendlyByteBuf buf) {
        return new ConfigureControlTowerPacket(buf.readBlockPos(), buf.readUnsignedByte(), buf.readUnsignedByte(),
                buf.readUnsignedByte(), buf.readBoolean(), buf.readBoolean());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null || !player.mayBuild()) return;
        if (!player.level().isLoaded(pos) || player.distanceToSqr(pos.getCenter()) > MAX_DISTANCE_SQR) return;
        BlockState state = player.level().getBlockState(pos);
        if (!(state.getBlock() instanceof ControlTowerBlock)) return;
        if (player.level().getBlockEntity(pos) instanceof ControlTowerBlockEntity tower) {
            tower.configure(baysW, baysD, levels, roof, skirt);
        }
    }
}
