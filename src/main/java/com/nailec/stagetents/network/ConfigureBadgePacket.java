package com.nailec.stagetents.network;

import com.nailec.stagetents.furniture.AccessBadgeItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: id and kind written onto the badge in the player's hand. */
public record ConfigureBadgePacket(boolean mainHand, String accessId, int kind) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(mainHand);
        buf.writeUtf(accessId == null ? "" : accessId, 32);
        buf.writeByte(kind);
    }

    public static ConfigureBadgePacket decode(FriendlyByteBuf buf) {
        return new ConfigureBadgePacket(buf.readBoolean(), buf.readUtf(32), buf.readByte());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null) return;
        ItemStack stack = player.getItemInHand(mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        if (!(stack.getItem() instanceof AccessBadgeItem)) return;
        AccessBadgeItem.program(stack, accessId, AccessBadgeItem.Kind.byId(kind));
        player.displayClientMessage(Component.translatable("message.stagetents.badge_saved"), true);
    }
}
