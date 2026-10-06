package com.nailec.stagetents.network;

import com.nailec.stagetents.furniture.AccessBadgeItem;
import com.nailec.stagetents.furniture.TurnstileBlock;
import com.nailec.stagetents.furniture.TurnstileBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: settings chosen in the turnstile screen. */
public record ConfigureTurnstilePacket(BlockPos pos, int mode, boolean oneWay, String accessId, int holdSeconds,
                                       boolean closeOnPass, boolean clicks, boolean pulseOut, boolean resetCount) {
    private static final double MAX_DISTANCE_SQR = 12 * 12;

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeByte(mode);
        buf.writeBoolean(oneWay);
        buf.writeUtf(accessId == null ? "" : accessId, 32);
        buf.writeByte(holdSeconds);
        buf.writeBoolean(closeOnPass);
        buf.writeBoolean(clicks);
        buf.writeBoolean(pulseOut);
        buf.writeBoolean(resetCount);
    }

    public static ConfigureTurnstilePacket decode(FriendlyByteBuf buf) {
        return new ConfigureTurnstilePacket(buf.readBlockPos(), buf.readByte(), buf.readBoolean(), buf.readUtf(32),
                buf.readByte(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null || !player.mayBuild()) return;
        if (!player.level().isLoaded(pos) || player.distanceToSqr(pos.getCenter()) > MAX_DISTANCE_SQR) return;
        if (!(player.level().getBlockEntity(pos) instanceof TurnstileBlockEntity be)) return;
        be.configure(TurnstileBlock.Mode.byId(mode), oneWay, AccessBadgeItem.sanitize(accessId), holdSeconds,
                closeOnPass, clicks, pulseOut, resetCount);
        player.displayClientMessage(Component.translatable("message.stagetents.turnstile_saved"), true);
    }
}
