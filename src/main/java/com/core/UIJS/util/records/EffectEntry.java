package com.core.UIJS.util.records;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

public record EffectEntry(MobEffect effect, int amplifier, int duration) {
    public MobEffectInstance createEffectInstance() {
        return new net.minecraft.world.effect.MobEffectInstance(effect, duration, amplifier, true, true, true);
    }
}
