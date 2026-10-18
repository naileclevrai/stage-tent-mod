package com.nailec.stagetents.client;

import com.nailec.stagetents.StageTents;
import com.nailec.stagetents.effect.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Slow camera sway from Ivresse. Visual only; the walk speed is the effect's attribute. */
@Mod.EventBusSubscriber(modid = StageTents.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DrunkennessClient {
    private DrunkennessClient() {}

    @SubscribeEvent
    public static void sway(ViewportEvent.ComputeCameraAngles event) {
        if (!(event.getCamera().getEntity() instanceof LivingEntity living)) return;
        MobEffectInstance drunk = living.getEffect(ModEffects.DRUNKENNESS.get());
        if (drunk == null || drunk.getDuration() <= 0) return;

        int amp = Math.min(3, drunk.getAmplifier());
        double t = (living.tickCount + event.getPartialTick()) / 20.0;
        float strength = 1.35F + amp * 1.45F;
        float fade = Math.min(1F, drunk.getDuration() / 50F);

        float yaw = (float) (Math.sin(t * 0.55) * 1.05 + Math.sin(t * 0.23 + 2.0) * 0.55);
        float pitch = (float) (Math.sin(t * 0.41 + 1.1) * 0.62 + Math.cos(t * 0.19) * 0.35);
        float roll = (float) (Math.sin(t * 0.33 + 0.4) * 1.25 + Math.sin(t * 0.14 + 1.6) * 0.70);
        if (amp >= 2) {
            roll += (float) (Math.sin(t * 0.11) * (1.4 + (amp - 2) * 0.8));
            yaw += (float) (Math.cos(t * 0.17) * 0.9);
        }

        event.setYaw(event.getYaw() + yaw * strength * fade);
        event.setPitch(event.getPitch() + pitch * strength * fade);
        event.setRoll(event.getRoll() + roll * strength * fade);
    }
}
