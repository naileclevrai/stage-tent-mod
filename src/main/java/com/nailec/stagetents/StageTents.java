package com.nailec.stagetents;

import com.nailec.stagetents.network.ModNetwork;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(StageTents.MOD_ID)
public class StageTents {
    public static final String MOD_ID = "stagetents";

    public StageTents() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModRegistry.register(modBus);
        ModNetwork.register();
    }
}
