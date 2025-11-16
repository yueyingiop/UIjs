package com.core.UIJS.kubejs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import net.minecraft.world.entity.player.Player;

public class ArmorSetRegistry {
    public static final EventGroup EVENT_GROUP = EventGroup.of("ArmorSetEvents");
    public static final EventHandler REGISTER = EVENT_GROUP.server("register", () -> RegisterEvent.class);

    private static final Map<String, ArmorSet> ARMOR_SETS = new HashMap<>();
    private static final Map<UUID, Set<String>> PLAYER_ACTIVE_SETS = new HashMap<>();

    public static void clearCache() {
        ARMOR_SETS.clear();
        PLAYER_ACTIVE_SETS.clear();
    }

    public static void registerSet(String id, ArmorSet set) {
        ARMOR_SETS.put(id, set);
    }

    public static void checkPlayerEquipment(Player player) {
        UUID playerId = player.getUUID();
        Set<String> previouslyActive = PLAYER_ACTIVE_SETS.getOrDefault(playerId, new HashSet<>());
        Set<String> currentlyActive = new HashSet<>();

        
        for (Map.Entry<String, ArmorSet> entry : ARMOR_SETS.entrySet()) {
            String setId = entry.getKey();
            ArmorSet set = entry.getValue();
            
            if (set.matches(player)) {
                currentlyActive.add(setId);
                
                set.applyEffects(player);
                set.applyAttributes(player);
            } else {
                // 移除效果（如果之前激活了但现在没有）
                if (previouslyActive.contains(setId)) {
                    set.removeEffects(player);
                    set.removeAttributes(player);
                }
            }
        }
        
        // 清理不再激活的套装效果
        for (String oldSetId : previouslyActive) {
            if (!currentlyActive.contains(oldSetId)) {
                ArmorSet oldSet = ARMOR_SETS.get(oldSetId);
                oldSet.removeEffects(player);
                oldSet.removeAttributes(player);
            }
        }

        // 更新激活的套装
        PLAYER_ACTIVE_SETS.put(playerId, currentlyActive);
    }

    public static Map<String, ArmorSet> getArmorSets() {
        return ARMOR_SETS;
    }

    public static class RegisterEvent extends dev.latvian.mods.kubejs.event.EventJS {
        public void create(String id, ArmorSetBuilder builder) {
            ArmorSet set = builder.build();
            ArmorSetRegistry.registerSet(id, set);

        }
    }
}
