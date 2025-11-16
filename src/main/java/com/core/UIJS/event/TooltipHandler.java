package com.core.UIJS.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.core.UIJS.UIJS;
import com.core.UIJS.kubejs.ArmorSet;
import com.core.UIJS.kubejs.ArmorSetRegistry;
import com.core.UIJS.util.records.ConditionalAttributeEntry;
import com.core.UIJS.util.records.ConditionalEffectEntry;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = UIJS.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TooltipHandler {
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (event.getEntity() == null) return; // 确保有玩家实体
        
        Item currentItem = event.getItemStack().getItem();
        List<Component> tooltips = event.getToolTip();
        
        // 查找所有包含当前物品的套装
        List<ArmorSet> relevantSets = findRelevantArmorSets(currentItem);
        
        if (!relevantSets.isEmpty()) {
            for (ArmorSet set : relevantSets) {
                if (!set.isShow()) continue;

                // 添加分隔线
                tooltips.add(Component.literal("-------------------").withStyle(ChatFormatting.GRAY));

                // 添加套装描述
                List<Component> description = set.getDescription(event.getEntity());
                if (description != null && !description.isEmpty()) {
                    tooltips.addAll(description);
                }

                addEffectInfo(tooltips, set);
                addAttributeInfo(tooltips, set);

                tooltips.add(Component.literal("-------------------").withStyle(ChatFormatting.GRAY));
            }

            for (ArmorSet set : relevantSets) {
                tooltips.addAll(set.getTooltips());
            }
        }
    }

    private static List<ArmorSet> findRelevantArmorSets(Item item) {
        List<ArmorSet> relevantSets = new ArrayList<>();
        
        for (Map.Entry<String, ArmorSet> entry : ArmorSetRegistry.getArmorSets().entrySet()) {
            ArmorSet set = entry.getValue();
            
            // 检查物品是否属于套装的任何部位
            if (set.getMainHandItems().contains(item) ||
                set.getOffHandItems().contains(item) ||
                set.getHelmetItems().contains(item) ||
                set.getChestplateItems().contains(item) ||
                set.getLeggingsItems().contains(item) ||
                set.getBootsItems().contains(item) ||
                isItemInCurios(set, item)) {
                
                relevantSets.add(set);
            }
        }
        
        return relevantSets;
    }

    // 检查物品是否在套装的饰品列表中
    private static boolean isItemInCurios(ArmorSet set, Item item) {
        Map<String, List<Item>> curiosItems = set.getCuriosItems();
        if (curiosItems != null && !curiosItems.isEmpty()) {
            for (List<Item> items : curiosItems.values()) {
                if (items.contains(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void addEffectInfo(List<Component> tooltips, ArmorSet set) {
        Map<Integer, List<ConditionalEffectEntry>> allEffects = set.getAllConditionalEffects();
        if (allEffects.isEmpty()) {
            return;
        }
        
        // 添加效果标题
        tooltips.add(Component.literal("效果:").withStyle(ChatFormatting.BLUE));
        
        // 添加每个效果
        for (Map.Entry<Integer, List<ConditionalEffectEntry>> entry : allEffects.entrySet()) {
            int requiredItems = entry.getKey();
            for (ConditionalEffectEntry conditionalEffect : entry.getValue()) {
                String effectName = conditionalEffect.effect().getDisplayName().getString();
                int amplifier = conditionalEffect.amplifier();
                
                // 构建效果文本，例如："    跳跃提升:II级"
                String levelText = getRomanNumeral(amplifier + 1);
                String effectText = "    " + effectName + ":" + levelText + "级";
                
                // 添加所需装备数量信息
                String requirementText = " (" + requiredItems + "件)";
                
                tooltips.add(Component.literal(effectText + requirementText).withStyle(ChatFormatting.GRAY));
            }
        }
    }

    private static void addAttributeInfo(List<Component> tooltips, ArmorSet set) {
        Map<Integer, List<ConditionalAttributeEntry>> allAttributes = set.getAllConditionalAttributes();
        if (allAttributes.isEmpty()) {
            return;
        }
        
        // 添加属性标题
        tooltips.add(Component.literal("属性").withStyle(ChatFormatting.GREEN));
        
        // 添加每个属性
        for (Map.Entry<Integer, List<ConditionalAttributeEntry>> entry : allAttributes.entrySet()) {
            int requiredItems = entry.getKey();
            for (ConditionalAttributeEntry conditionalAttribute : entry.getValue()) {
                String attributeTranslationKey = conditionalAttribute.attribute().getDescriptionId();
                Component attributeDisplayName = Component.translatable(attributeTranslationKey);
            
                double amount = conditionalAttribute.amount();
                String operation = conditionalAttribute.operation().name();
                
                // 构建属性文本
                MutableComponent attributeLine = Component.literal("    ")
                    .append(attributeDisplayName)
                    .append(Component.literal(":"));
                
                String valueText;
                
                switch (operation) {
                    case "ADDITION":
                        valueText = String.format("%+.2f", amount);
                        break;
                    case "MULTIPLY_BASE":
                    case "MULTIPLY_TOTAL":
                        valueText = String.format("%+.2f%%", amount * 100);
                        break;
                    default:
                        valueText = String.format("%+.2f", amount);
                        break;
                }
                
                attributeLine.append(Component.literal(valueText));
            
                // 添加所需装备数量信息
                String requirementText = " (" + requiredItems + "件)";
                attributeLine.append(Component.literal(requirementText));
                
                tooltips.add(attributeLine.withStyle(ChatFormatting.GRAY));
            }
        }
    }

    private static String getRomanNumeral(int number) {
        switch (number) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            case 5: return "V";
            case 6: return "VI";
            case 7: return "VII";
            case 8: return "VIII";
            case 9: return "IX";
            case 10: return "X";
            default: return String.valueOf(number);
        }
    }
}
