package com.nailec.stagetents;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, StageTents.MOD_ID);

    /** Looping diesel idle played while a generator is running. */
    public static final RegistryObject<SoundEvent> GENERATOR = SOUNDS.register("generator",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(StageTents.MOD_ID, "generator")));

    private ModSounds() {}
}
