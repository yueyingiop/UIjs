package com.core.UIJS.util.records;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public record ConditionalAttributeEntry(
    Attribute attribute, 
    String name, 
    double amount, 
    AttributeModifier.Operation operation, 
    int requiredItems
) {
    public AttributeModifierEntry toAttributeModifierEntry() {
        return new AttributeModifierEntry(attribute, name, amount, operation);
    }
}
