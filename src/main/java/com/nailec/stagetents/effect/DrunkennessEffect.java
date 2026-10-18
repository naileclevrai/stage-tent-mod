package com.nailec.stagetents.effect;

import com.nailec.stagetents.StageTents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Beer drunkenness. Movement speed is an attribute, so the server is what other players see.
 * Higher amplifiers add a little mining fatigue, then short darkness and a stumble.
 */
@Mod.EventBusSubscriber(modid = StageTents.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DrunkennessEffect extends MobEffect {
    /** One glass lasts three minutes. Another glass puts the timer back to this. */
    public static final int DRINK_TICKS = 20 * 60 * 3;
    public static final int MAX_AMPLIFIER = 3;
    private static final String SPEED_UUID = "7c1e9a42-5b18-4f0d-a6c3-2d8e4b91f0aa";

    public DrunkennessEffect() {
        super(MobEffectCategory.HARMFUL, 0xE6A23C);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, SPEED_UUID, -0.08D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    /** First glass is amplifier 0. Each further glass raises it, capped, and refreshes the duration. */
    public static void onDrink(LivingEntity entity) {
        MobEffect effect = ModEffects.DRUNKENNESS.get();
        MobEffectInstance current = entity.getEffect(effect);
        int amplifier = current == null ? 0 : Math.min(MAX_AMPLIFIER, current.getAmplifier() + 1);
        entity.forceAddEffect(new MobEffectInstance(effect, DRINK_TICKS, amplifier, false, true, true), null);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        Level level = entity.level();
        if (level.isClientSide) {
            haze(entity, amplifier);
            return;
        }
        if (amplifier < 2 || entity.isPassenger()) return;
        int clock = entity.tickCount + entity.getId() * 31;
        if (clock % 180 == 0) pulseDarkness(entity);
        int stumbleEvery = amplifier >= 3 ? 100 : 150;
        if (clock % stumbleEvery == 0 && entity.onGround() && entity.getRandom().nextFloat() < 0.8F) {
            double angle = entity.getRandom().nextDouble() * Math.PI * 2.0;
            entity.knockback(0.14, Math.sin(angle), Math.cos(angle));
        }
    }

    /** A few amber motes. Client levels only; the server copy of the effect does not spawn them. */
    private static void haze(LivingEntity entity, int amplifier) {
        if (entity.tickCount % 8 != 0) return;
        Level level = entity.level();
        int count = amplifier >= 2 ? 2 : 1;
        for (int i = 0; i < count; i++) {
            double x = entity.getX() + (entity.getRandom().nextDouble() - 0.5) * 0.7;
            double y = entity.getY() + entity.getBbHeight() * (0.4 + entity.getRandom().nextDouble() * 0.5);
            double z = entity.getZ() + (entity.getRandom().nextDouble() - 0.5) * 0.7;
            level.addParticle(ParticleTypes.ENTITY_EFFECT, x, y, z, 0.91, 0.58, 0.16);
        }
    }

    /** Hidden vanilla darkness so the screen dips. The icon the player sees stays Ivresse. */
    private static void pulseDarkness(LivingEntity entity) {
        MobEffectInstance dark = entity.getEffect(MobEffects.DARKNESS);
        if (dark != null && dark.getDuration() > 70) return;
        entity.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 70, 0, false, false, false));
    }

    /** Amplifier 1 and up: a modest dig slowdown, stronger as the player gets drunker. */
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        MobEffectInstance drunk = event.getEntity().getEffect(ModEffects.DRUNKENNESS.get());
        if (drunk == null || drunk.getAmplifier() < 1) return;
        float kept = switch (Math.min(MAX_AMPLIFIER, drunk.getAmplifier())) {
            case 1 -> 0.85F;
            case 2 -> 0.72F;
            default -> 0.60F;
        };
        event.setNewSpeed(event.getNewSpeed() * kept);
    }
}
