package com.core.UIJS.util.records;

import java.util.UUID;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public record AttributeModifierEntry(Attribute attribute, String name, double amount, AttributeModifier.Operation operation) {
    public AttributeModifier createModifier() {
        // 使用名称生成固定的UUID
        UUID uuid = UUID.nameUUIDFromBytes(name.getBytes());
        return new AttributeModifier(uuid, name, amount, operation);
    }

     public AttributeModifier createPermanentModifier() {
        UUID uuid = UUID.nameUUIDFromBytes(name.getBytes());
        return new AttributeModifier(uuid, name, amount, operation);
    }
}
