package com.nailec.stagetents.network;

import com.nailec.stagetents.furniture.ConnectedFurnitureBlock;
import com.nailec.stagetents.furniture.FurnitureBlock;
import com.nailec.stagetents.furniture.GrandstandBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: build a grandstand from the bleacher at {@code pos}. */
public record BuildGrandstandPacket(BlockPos pos, int rows, int width, int aisle, int color) {
    private static final double MAX_DISTANCE_SQR = 16 * 16;

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(rows);
        buf.writeVarInt(width);
        buf.writeVarInt(aisle);
        buf.writeVarInt(color);
    }

    public static BuildGrandstandPacket decode(FriendlyByteBuf buf) {
        return new BuildGrandstandPacket(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null || !player.mayBuild()) return;
        if (!player.level().isLoaded(pos) || player.distanceToSqr(pos.getCenter()) > MAX_DISTANCE_SQR) return;
        BlockState state = player.level().getBlockState(pos);
        if (!(state.getBlock() instanceof ConnectedFurnitureBlock c) || !c.group.equals("grandstand")) return;
        int n = GrandstandBuilder.build(player.level(), pos, state.getValue(FurnitureBlock.FACING), rows, width, aisle, DyeColor.byId(color));
        player.displayClientMessage(Component.translatable("message.stagetents.grandstand_built", n), true);
    }
}
