package com.core.UIJS.kubejs;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

public class ArmorSetBuilder {
    private List<Item> mainHandItems = new ArrayList<>();
        private List<Item> offHandItems = new ArrayList<>();
        private List<Item> helmetItems = new ArrayList<>();
        private List<Item> chestplateItems = new ArrayList<>();
        private List<Item> leggingsItems = new ArrayList<>();
        private List<Item> bootsItems = new ArrayList<>();
        private List<EffectEntry> effects = new ArrayList<>();
        private int checkInterval = 100; // 默认5秒检查一次
        
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
        
        public ArmorSetBuilder effect(String effectId, int amplifier, int duration) {
            MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.parse(effectId));
            if (effect != null) {
                effects.add(new EffectEntry(effect, amplifier, duration));
            }
            return this;
        }
        
        public ArmorSetBuilder checkInterval(int ticks) {
            this.checkInterval = ticks;
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
        
        public ArmorSet build() {
            return new ArmorSet(
                mainHandItems, offHandItems, 
                helmetItems, chestplateItems, leggingsItems, bootsItems,
                effects, checkInterval
            );
        }
}
