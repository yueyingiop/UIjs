package com.core.UIJS.kubejs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.core.UIJS.util.records.ConditionalAttributeEntry;
import com.core.UIJS.util.records.ConditionalEffectEntry;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

public class ArmorSetBuilder {
    private String displayName = "";
    private List<Item> mainHandItems = new ArrayList<>();
    private List<Item> offHandItems = new ArrayList<>();
    private List<Item> helmetItems = new ArrayList<>();
    private List<Item> chestplateItems = new ArrayList<>();
    private List<Item> leggingsItems = new ArrayList<>();
    private List<Item> bootsItems = new ArrayList<>();
    private Map<String, List<Item>> curiosItems = new HashMap<>();
    private Map<Integer, List<ConditionalEffectEntry>> conditionalEffects = new HashMap<>();
    private Map<Integer, List<ConditionalAttributeEntry>> conditionalAttributes = new HashMap<>();

    private List<Component> description = new ArrayList<>();

    private Consumer<ServerPlayer> tickCallback = null;

    private int checkInterval = 100; // 默认5秒检查一次
    private boolean isShow = true;

    //#region 添加方法
    public ArmorSetBuilder displayName(String name) {
        this.displayName = name;
        return this;
    }
    
    public ArmorSetBuilder mainHand(String... itemIds) {
        addItems(mainHandItems, itemIds);
        return this;
    }
    
    public ArmorSetBuilder offHand(String... itemIds) {
        addItems(offHandItems, itemIds);
        return this;
    }
    
    public ArmorSetBuilder helmet(String... itemIds) {
        addItems(helmetItems, itemIds);
        return this;
    }
    
    public ArmorSetBuilder chestplate(String... itemIds) {
        addItems(chestplateItems, itemIds);
        return this;
    }
    
    public ArmorSetBuilder leggings(String... itemIds) {
        addItems(leggingsItems, itemIds);
        return this;
    }
    
    public ArmorSetBuilder boots(String... itemIds) {
        addItems(bootsItems, itemIds);
        return this;
    }

    public ArmorSetBuilder curios(String curiosSlot, String... itemIds) {
        List<Item> items = curiosItems.computeIfAbsent(curiosSlot, k -> new ArrayList<>());
        for (String itemId : itemIds) {
            Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(itemId));
            if (item != null) {
                items.add(item);
            }
        }
        return this;
    }
    
    public ArmorSetBuilder effect(String effectId, int amplifier, int duration, int requiredItems) {
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.parse(effectId));
        if (effect != null) {
            ConditionalEffectEntry conditionalEffect = new ConditionalEffectEntry(effect, amplifier, duration, requiredItems);
            conditionalEffects.computeIfAbsent(requiredItems, k -> new ArrayList<>()).add(conditionalEffect);
        }
        return this;
    }

    public ArmorSetBuilder effect(String effectId, int amplifier, int duration) {
        int totalItems = countTotalItems();
        return effect(effectId, amplifier, duration, totalItems);
    }

    public ArmorSetBuilder attribute(String attributeId, String name, double amount, String operation, int requiredItems) {
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(ResourceLocation.parse(attributeId));
        if (attribute != null) {
            AttributeModifier.Operation op = parseOperation(operation);
            ConditionalAttributeEntry conditionalAttribute = new ConditionalAttributeEntry(attribute, name, amount, op, requiredItems);
            conditionalAttributes.computeIfAbsent(requiredItems, k -> new ArrayList<>()).add(conditionalAttribute);
        }
        return this;
    }

    public ArmorSetBuilder attribute(String attributeId, String name, double amount, String operation) {
        int totalItems = countTotalItems();
        return attribute(attributeId, name, amount, operation, totalItems);
    }

    public ArmorSetBuilder addTooltips(Component... description) {
        this.description.addAll(List.of(description));
        return this;
    }

    public ArmorSetBuilder tickCallback(Consumer<ServerPlayer> tickCallback) {
        this.tickCallback = tickCallback;
        return this;
    }
    
    public ArmorSetBuilder checkInterval(int ticks) {
        this.checkInterval = ticks;
        return this;
    }

    public ArmorSetBuilder isShow(boolean isShow) {
        this.isShow = isShow;
        return this;
    }
    
    private void addItems(List<Item> list, String... itemIds) {
        for (String itemId : itemIds) {
            Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(itemId));
            if (item != null) {
                list.add(item);
            }
        }
    }
    //#endregion

    private AttributeModifier.Operation parseOperation(String operation) {
        return switch (operation.toLowerCase()) {
            case "multiply_base" -> AttributeModifier.Operation.MULTIPLY_BASE;
            case "multiply_total" -> AttributeModifier.Operation.MULTIPLY_TOTAL;
            default -> AttributeModifier.Operation.ADDITION;
        };
    }

    private int countTotalItems() {
        int count = 0;
        if (!mainHandItems.isEmpty()) count++;
        if (!offHandItems.isEmpty()) count++;
        if (!helmetItems.isEmpty()) count++;
        if (!chestplateItems.isEmpty()) count++;
        if (!leggingsItems.isEmpty()) count++;
        if (!bootsItems.isEmpty()) count++;
        count += curiosItems.values().stream().mapToInt(List::size).sum();
        return count;
    }
    
    public ArmorSet build() {
        return new ArmorSet(
            displayName, 
            mainHandItems, 
            offHandItems, 
            helmetItems, 
            chestplateItems, 
            leggingsItems, 
            bootsItems,
            curiosItems,
            conditionalEffects, 
            conditionalAttributes, 
            description,
            tickCallback,
            checkInterval,
            isShow
        );
    }
}
