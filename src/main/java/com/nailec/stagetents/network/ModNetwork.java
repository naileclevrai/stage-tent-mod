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
        CHANNEL.messageBuilder(ConfigureTurnstilePacket.class, 2, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ConfigureTurnstilePacket::encode)
                .decoder(ConfigureTurnstilePacket::decode)
                .consumerMainThread(ConfigureTurnstilePacket::handle)
                .add();
        CHANNEL.messageBuilder(ConfigureBadgePacket.class, 3, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ConfigureBadgePacket::encode)
                .decoder(ConfigureBadgePacket::decode)
                .consumerMainThread(ConfigureBadgePacket::handle)
                .add();
        CHANNEL.messageBuilder(ConfigureDrapePacket.class, 4, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ConfigureDrapePacket::encode)
                .decoder(ConfigureDrapePacket::decode)
                .consumerMainThread(ConfigureDrapePacket::handle)
                .add();
        CHANNEL.messageBuilder(ConfigureCannonPacket.class, 5, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ConfigureCannonPacket::encode)
                .decoder(ConfigureCannonPacket::decode)
                .consumerMainThread(ConfigureCannonPacket::handle)
                .add();
        CHANNEL.messageBuilder(ConfigureCableRampPacket.class, 6, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ConfigureCableRampPacket::encode)
                .decoder(ConfigureCableRampPacket::decode)
                .consumerMainThread(ConfigureCableRampPacket::handle)
                .add();
        CHANNEL.messageBuilder(ConfigureCurtainPacket.class, 7, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ConfigureCurtainPacket::encode)
                .decoder(ConfigureCurtainPacket::decode)
                .consumerMainThread(ConfigureCurtainPacket::handle)
                .add();
        CHANNEL.messageBuilder(ConfigureLightTowerPacket.class, 8, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ConfigureLightTowerPacket::encode)
                .decoder(ConfigureLightTowerPacket::decode)
                .consumerMainThread(ConfigureLightTowerPacket::handle)
                .add();
    }
}
