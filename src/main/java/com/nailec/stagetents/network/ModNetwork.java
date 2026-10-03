package com.nailec.stagetents.network;

import com.nailec.stagetents.StageTents;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(StageTents.MOD_ID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private ModNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(UpdateTentPacket.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UpdateTentPacket::encode)
                .decoder(UpdateTentPacket::decode)
                .consumerMainThread(UpdateTentPacket::handle)
                .add();
        CHANNEL.messageBuilder(BuildGrandstandPacket.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder(BuildGrandstandPacket::encode)
                .decoder(BuildGrandstandPacket::decode)
                .consumerMainThread(BuildGrandstandPacket::handle)
                .add();
    }
}
