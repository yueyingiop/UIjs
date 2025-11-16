package com.core.UIJS.util.records;

import net.minecraft.world.effect.MobEffect;

public record ConditionalEffectEntry(
    MobEffect effect, 
    int amplifier, 
    int duration, 
    int requiredItems
) {
    public EffectEntry toEffectEntry() {
        return new EffectEntry(effect, amplifier, duration);
    }
}
