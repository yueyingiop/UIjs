package com.core.UIJS.kubejs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.core.UIJS.util.CuriosHelper;
import com.core.UIJS.util.records.AttributeModifierEntry;
import com.core.UIJS.util.records.ConditionalAttributeEntry;
import com.core.UIJS.util.records.ConditionalEffectEntry;
import com.core.UIJS.util.records.EffectEntry;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

public class ArmorSet {
    private final String displayName;
    private final List<Item> mainHandItems;
    private final List<Item> offHandItems;
    private final List<Item> helmetItems;
    private final List<Item> chestplateItems;
    private final List<Item> leggingsItems;
    private final List<Item> bootsItems;
    private final Map<String, List<Item>> curiosItems;
    private final Map<Integer, List<ConditionalEffectEntry>> conditionalEffects;
    private final Map<Integer, List<ConditionalAttributeEntry>> conditionalAttributes;
    private final List<Component> tooltips;
    private final Consumer<ServerPlayer> tickCallback;
    private final int checkInterval;
    private final boolean isShow;
    
    public ArmorSet(
        String displayName, 
        List<Item> mainHandItems, 
        List<Item> offHandItems, 
        List<Item> helmetItems, 
        List<Item> chestplateItems, 
        List<Item> leggingsItems, 
        List<Item> bootsItems,
        Map<String, List<Item>> curiosItems,
        Map<Integer, List<ConditionalEffectEntry>> conditionalEffects,
        Map<Integer, List<ConditionalAttributeEntry>> conditionalAttributes,
        List<Component> tooltips,
        Consumer<ServerPlayer> tickCallback,
        int checkInterval,
        boolean isShow
    ) {
        this.displayName = displayName;
        this.mainHandItems = mainHandItems;
        this.offHandItems = offHandItems;
        this.helmetItems = helmetItems;
        this.chestplateItems = chestplateItems;
        this.leggingsItems = leggingsItems;
        this.bootsItems = bootsItems;
        this.curiosItems = curiosItems;
        this.conditionalEffects = conditionalEffects;
        this.conditionalAttributes = conditionalAttributes;
        this.tooltips = tooltips;
        this.tickCallback = tickCallback;
        this.checkInterval = checkInterval;
        this.isShow = isShow;
    }
    
    public boolean matches(Player player) {
        return getEquippedCount(player) > 0;
    }
    
    //#region 效果和属性处理
    public void applyEffects(Player player) {
        int equippedCount = getEquippedCount(player);
    
        removeEffects(player);
        
        for (Map.Entry<Integer, List<ConditionalEffectEntry>> entry : conditionalEffects.entrySet()) {
            int requiredItems = entry.getKey();
            if (equippedCount >= requiredItems) {
                for (ConditionalEffectEntry conditionalEffect : entry.getValue()) {
                    EffectEntry effectEntry = conditionalEffect.toEffectEntry();
                    player.addEffect(effectEntry.createEffectInstance());
                }
            }
        }
    }
    
    public void removeEffects(Player player) {
        for (List<ConditionalEffectEntry> effectList : conditionalEffects.values()) {
            for (ConditionalEffectEntry conditionalEffect : effectList) {
                player.removeEffect(conditionalEffect.effect());
            }
        }
    }

    public void applyAttributes(Player player) {
        int equippedCount = getEquippedCount(player);
    
        // 首先移除所有可能的属性修饰符，然后重新应用满足条件的属性
        removeAttributes(player);
        
        for (Map.Entry<Integer, List<ConditionalAttributeEntry>> entry : conditionalAttributes.entrySet()) {
            int requiredItems = entry.getKey();
            if (equippedCount >= requiredItems) {
                for (ConditionalAttributeEntry conditionalAttribute : entry.getValue()) {
                    AttributeModifierEntry modifierEntry = conditionalAttribute.toAttributeModifierEntry();
                    AttributeInstance attribute = player.getAttribute(modifierEntry.attribute());
                    if (attribute != null) {
                        AttributeModifier modifier = modifierEntry.createModifier();
                        attribute.addPermanentModifier(modifier);
                    }
                }
            }
        }
    }

    public void removeAttributes(Player player) {
        // 移除所有可能的属性修饰符
        for (List<ConditionalAttributeEntry> attributeList : conditionalAttributes.values()) {
            for (ConditionalAttributeEntry conditionalAttribute : attributeList) {
                AttributeModifierEntry modifierEntry = conditionalAttribute.toAttributeModifierEntry();
                AttributeInstance attribute = player.getAttribute(modifierEntry.attribute());
                if (attribute != null) {
                    attribute.removeModifier(modifierEntry.createModifier().getId());
                }
            }
        }
    }

    public void onTick(ServerPlayer player){
        int equippedCount = getEquippedCount(player);
        int totalSlots = countEquipmentSlots();
        if (equippedCount != totalSlots) return;
        if (tickCallback == null) return;
        tickCallback.accept(player);
    }
    //#endregion

    public List<Component> getDescription(Player player) {
        List<Component> descriptionLines = new ArrayList<>();
        MutableComponent component = Component.literal(displayName).withStyle(ChatFormatting.YELLOW);
        descriptionLines.add(component);

        int totalSlots = countEquipmentSlots();
        int currentSlot = 1;
        
        // 添加主手物品
        if (!mainHandItems.isEmpty()) {
            boolean hasItem = !player.getMainHandItem().isEmpty() && 
                            mainHandItems.contains(player.getMainHandItem().getItem());
            descriptionLines.add(createItemLine("(" + currentSlot + "/" + totalSlots + ") ", mainHandItems, hasItem));
            currentSlot++;
        }
        
        // 添加副手物品
        if (!offHandItems.isEmpty()) {
            boolean hasItem = !player.getOffhandItem().isEmpty() && 
                            offHandItems.contains(player.getOffhandItem().getItem());
            descriptionLines.add(createItemLine("(" + currentSlot + "/" + totalSlots + ") ", offHandItems, hasItem));
            currentSlot++;
        }
        
        // 添加头盔
        if (!helmetItems.isEmpty()) {
            boolean hasItem = !player.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && 
                            helmetItems.contains(player.getItemBySlot(EquipmentSlot.HEAD).getItem());
            descriptionLines.add(createItemLine("(" + currentSlot + "/" + totalSlots + ") ", helmetItems, hasItem));
            currentSlot++;
        }
        
        // 添加胸甲
        if (!chestplateItems.isEmpty()) {
            boolean hasItem = !player.getItemBySlot(EquipmentSlot.CHEST).isEmpty() && 
                            chestplateItems.contains(player.getItemBySlot(EquipmentSlot.CHEST).getItem());
            descriptionLines.add(createItemLine("(" + currentSlot + "/" + totalSlots + ") ", chestplateItems, hasItem));
            currentSlot++;
        }
        
        // 添加护腿
        if (!leggingsItems.isEmpty()) {
            boolean hasItem = !player.getItemBySlot(EquipmentSlot.LEGS).isEmpty() && 
                            leggingsItems.contains(player.getItemBySlot(EquipmentSlot.LEGS).getItem());
            descriptionLines.add(createItemLine("(" + currentSlot + "/" + totalSlots + ") ", leggingsItems, hasItem));
            currentSlot++;
        }
        
        // 添加靴子
        if (!bootsItems.isEmpty()) {
            boolean hasItem = !player.getItemBySlot(EquipmentSlot.FEET).isEmpty() && 
                            bootsItems.contains(player.getItemBySlot(EquipmentSlot.FEET).getItem());
            descriptionLines.add(createItemLine("(" + currentSlot + "/" + totalSlots + ") ", bootsItems, hasItem));
            currentSlot++;
        }

        // 添加 Curios 饰品
        if (curiosItems != null && !curiosItems.isEmpty()) {
            for (Map.Entry<String, List<Item>> entry : curiosItems.entrySet()) {
                descriptionLines.addAll(createCuriosItemLines(player, curiosItems, currentSlot));
                currentSlot++;
            }
        }
        
        return descriptionLines;
    }

    private int countEquipmentSlots() {
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

    //#region 辅助函数

    public int getEquippedCount(Player player) {
        int count = 0;
        
        if (!mainHandItems.isEmpty() && !player.getMainHandItem().isEmpty() && 
            mainHandItems.contains(player.getMainHandItem().getItem())) {
            count++;
        }
        if (!offHandItems.isEmpty() && !player.getOffhandItem().isEmpty() && 
            offHandItems.contains(player.getOffhandItem().getItem())) {
            count++;
        }
        if (!helmetItems.isEmpty() && !player.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && 
            helmetItems.contains(player.getItemBySlot(EquipmentSlot.HEAD).getItem())) {
            count++;
        }
        if (!chestplateItems.isEmpty() && !player.getItemBySlot(EquipmentSlot.CHEST).isEmpty() && 
            chestplateItems.contains(player.getItemBySlot(EquipmentSlot.CHEST).getItem())) {
            count++;
        }
        if (!leggingsItems.isEmpty() && !player.getItemBySlot(EquipmentSlot.LEGS).isEmpty() && 
            leggingsItems.contains(player.getItemBySlot(EquipmentSlot.LEGS).getItem())) {
            count++;
        }
        if (!bootsItems.isEmpty() && !player.getItemBySlot(EquipmentSlot.FEET).isEmpty() && 
            bootsItems.contains(player.getItemBySlot(EquipmentSlot.FEET).getItem())) {
            count++;
        }

        if (curiosItems != null && !curiosItems.isEmpty()) {
            for (Map.Entry<String, List<Item>> entry : curiosItems.entrySet()) {
                String slot = entry.getKey();
                List<Item> items = entry.getValue();
                count+=hasCuriosItemCount(player, slot, items);
            }
        }
        
        return count;
    }

    private int hasCuriosItemCount(Player player, String curiosSlot, List<Item> items) {
        int count = 0;
        for (Item item : items) {
            String itemId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item).toString();
            boolean hasItem = CuriosHelper.getCuriosItem(player, curiosSlot, itemId);
            if (hasItem) {
                count++;
            }
        }
        return count;
    }


    private Component createItemLine(String prefix, List<Item> items, boolean hasItem) {
        MutableComponent line = Component.literal(prefix);
        
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            // 使用物品的显示名称而不是技术名称
            MutableComponent itemName = Component.literal(item.getDescription().getString());
            
            if (hasItem) {
                itemName.withStyle(ChatFormatting.GREEN);
            } else {
                itemName.withStyle(ChatFormatting.GRAY);
            }
            
            line.append(itemName);
            
            if (i < items.size() - 1) {
                line.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
            }
        }
        
        return line;
        
    }

    private List<Component> createCuriosItemLines(Player player, Map<String, List<Item>> itemMap, int startIndex) { 
        int currentSlot = startIndex;
        int totalSlots = countEquipmentSlots();

        List<Component> lines = new ArrayList<>();
        for (Map.Entry<String, List<Item>> entry : itemMap.entrySet()) {
            String slot = entry.getKey();
            List<Item> items = entry.getValue();
            
            for (int i = 0; i < items.size(); i++){
                String prefix = "(" + currentSlot + "/" + totalSlots + ") " + slot + ": ";
                MutableComponent line = Component.literal(prefix);
                Item item = items.get(i);
                String itemId = ForgeRegistries.ITEMS.getKey(item).toString();
                MutableComponent itemName = Component.literal(item.getDescription().getString());

                if (CuriosHelper.getCuriosItem(player, slot, itemId)) {
                    itemName.withStyle(ChatFormatting.GREEN);
                } else {
                    itemName.withStyle(ChatFormatting.GRAY);
                }

                lines.add(line.append(itemName));
                currentSlot++;
            }

        }
        return lines;
    }
    //#endregion

    //#region Getters
    public int getCheckInterval() {
        return checkInterval;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<Item> getMainHandItems() {
        return mainHandItems;
    }

    public List<Item> getOffHandItems() {
        return offHandItems;
    }

    public List<Item> getHelmetItems() {
        return helmetItems;
    }

    public List<Item> getChestplateItems() {
        return chestplateItems;
    }

    public List<Item> getLeggingsItems() {
        return leggingsItems;
    }

    public List<Item> getBootsItems() {
        return bootsItems;
    }

    public Map<String, List<Item>> getCuriosItems() {
        return curiosItems;
    }

    public Map<Integer, List<ConditionalEffectEntry>> getAllConditionalEffects() {
        return conditionalEffects;
    }
    
    // 获取所有条件属性（用于工具提示）
    public Map<Integer, List<ConditionalAttributeEntry>> getAllConditionalAttributes() {
        return conditionalAttributes;
    }

    public List<Component> getTooltips() { 
        return tooltips;
    }

    public Consumer<ServerPlayer> getTickCallback() { 
        return tickCallback;
    }

    public boolean isShow() {
        return isShow;
    }
    //#endregion
}
    

