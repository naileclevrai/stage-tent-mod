package com.nailec.stagetents.effect;

import com.nailec.stagetents.StageTents;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, StageTents.MOD_ID);

    public static final RegistryObject<MobEffect> DRUNKENNESS = EFFECTS.register("drunkenness", DrunkennessEffect::new);

    private ModEffects() {}
}
