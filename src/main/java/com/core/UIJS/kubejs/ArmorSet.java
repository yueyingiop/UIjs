package com.core.UIJS.kubejs;

import java.util.List;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ArmorSet {
    private final List<Item> mainHandItems;
    private final List<Item> offHandItems;
    private final List<Item> helmetItems;
    private final List<Item> chestplateItems;
    private final List<Item> leggingsItems;
    private final List<Item> bootsItems;
    private final List<EffectEntry> effects;
    private final int checkInterval;
    
    public ArmorSet(List<Item> mainHandItems, List<Item> offHandItems, 
                    List<Item> helmetItems, List<Item> chestplateItems, 
                    List<Item> leggingsItems, List<Item> bootsItems,
                    List<EffectEntry> effects, int checkInterval) {
        this.mainHandItems = mainHandItems;
        this.offHandItems = offHandItems;
        this.helmetItems = helmetItems;
        this.chestplateItems = chestplateItems;
        this.leggingsItems = leggingsItems;
        this.bootsItems = bootsItems;
        this.effects = effects;
        this.checkInterval = checkInterval;
    }
    
    public boolean matches(Player player) {
        // 检查主手
        if (!mainHandItems.isEmpty()) {
            ItemStack mainHand = player.getMainHandItem();
            if (mainHand.isEmpty() || !mainHandItems.contains(mainHand.getItem())) {
                return false;
            }
        }
        
        // 检查副手
        if (!offHandItems.isEmpty()) {
            ItemStack offHand = player.getOffhandItem();
            if (offHand.isEmpty() || !offHandItems.contains(offHand.getItem())) {
                return false;
            }
        }
        
        // 检查头盔
        if (!helmetItems.isEmpty()) {
            ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
            if (helmet.isEmpty() || !helmetItems.contains(helmet.getItem())) {
                return false;
            }
        }
        
        // 检查胸甲
        if (!chestplateItems.isEmpty()) {
            ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
            if (chestplate.isEmpty() || !chestplateItems.contains(chestplate.getItem())) {
                return false;
            }
        }
        
        // 检查护腿
        if (!leggingsItems.isEmpty()) {
            ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);
            if (leggings.isEmpty() || !leggingsItems.contains(leggings.getItem())) {
                return false;
            }
        }
        
        // 检查靴子
        if (!bootsItems.isEmpty()) {
            ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
            if (boots.isEmpty() || !bootsItems.contains(boots.getItem())) {
                return false;
            }
        }
        
        return true;
    }
    
    public void applyEffects(Player player) {
        for (EffectEntry effectEntry : effects) {
            // 检查是否已经存在该效果
            MobEffectInstance existingEffect = player.getEffect(effectEntry.effect());
            
            
            // 如果效果不存在，或者持续时间较短，或者等级不同，则重新应用
            if (existingEffect == null || 
                existingEffect.getDuration() < effectEntry.duration() - 100 ||
                existingEffect.getAmplifier() != effectEntry.amplifier()
            ) {
                
                player.removeEffect(effectEntry.effect());
                player.addEffect(effectEntry.createEffectInstance());
                
                if (ArmorSetRegistry.DEBUG) {
                    System.out.println("[UIJS] Applied/Refreshed effect: " + effectEntry.effect().getDisplayName().getString());
                }
            }
        }
    }
    
    public void removeEffects(Player player) {
        for (EffectEntry effectEntry : effects) {
            player.removeEffect(effectEntry.effect());
            if (ArmorSetRegistry.DEBUG) {
                System.out.println("[UIJS] Removed effect: " + effectEntry.effect().getDisplayName().getString());
            }
        }
    }
    
    public int getCheckInterval() {
        return checkInterval;
    }
}
    

