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

    public static final boolean DEBUG = true;

    public static void clearCache() {
        if (DEBUG) {
            System.out.println("[UIJS] Clearing armor set cache");
        }
        ARMOR_SETS.clear();
        PLAYER_ACTIVE_SETS.clear();
    }

    public static void registerSet(String id, ArmorSet set) {
        ARMOR_SETS.put(id, set);
        if (DEBUG) {
            System.out.println("[UIJS] Registered armor set: " + id);
            System.out.println("[UIJS] Total registered sets: " + ARMOR_SETS.size());
        }
    }

    public static void checkPlayerEquipment(Player player) {
        if (DEBUG) {
            System.out.println("[UIJS] Checking equipment for player: " + player.getName().getString());
        }
        UUID playerId = player.getUUID();
        Set<String> previouslyActive = PLAYER_ACTIVE_SETS.getOrDefault(playerId, new HashSet<>());
        Set<String> currentlyActive = new HashSet<>();

        if (DEBUG) {
            System.out.println("[UIJS] Previously active sets: " + previouslyActive);
        }
        
        for (Map.Entry<String, ArmorSet> entry : ARMOR_SETS.entrySet()) {
            String setId = entry.getKey();
            ArmorSet set = entry.getValue();

            if (DEBUG) {
                System.out.println("[UIJS] Set '" + setId + "' matches: " + set.matches(player));
            }
            
            if (set.matches(player)) {
                currentlyActive.add(setId);
                
                // 应用效果（如果之前没有激活）
                if (!previouslyActive.contains(setId)) {
                    currentlyActive.add(setId);
                    if (DEBUG) {
                        System.out.println("[UIJS] Applying effects for set: " + setId);
                    }
                    set.applyEffects(player);
                }
            } else {
                // 移除效果（如果之前激活了但现在没有）
                if (previouslyActive.contains(setId)) {
                    if (DEBUG) {
                        System.out.println("[UIJS] Removing effects for set: " + setId);
                    }
                    set.removeEffects(player);
                }
            }
        }
        
        // 清理不再激活的套装效果
        for (String oldSetId : previouslyActive) {
            if (!currentlyActive.contains(oldSetId)) {
                ARMOR_SETS.get(oldSetId).removeEffects(player);
            }
        }

        // 更新激活的套装
        PLAYER_ACTIVE_SETS.put(playerId, currentlyActive);
        if (DEBUG) {
            System.out.println("[UIJS] Currently active sets: " + currentlyActive);
        }
    }

    public static class RegisterEvent extends dev.latvian.mods.kubejs.event.EventJS {
        public void create(String id, ArmorSetBuilder builder) {
            if (DEBUG) {
                System.out.println("[UIJS] Creating armor set: " + id);
            }
            ArmorSet set = builder.build();
            ArmorSetRegistry.registerSet(id, set);

        }
    }
}
