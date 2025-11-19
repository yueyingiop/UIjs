package com.core.UIJS.storage;

import java.util.HashMap;
import java.util.Map;

import com.core.UIJS.UIJS;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class BlockBoundSlotDataManager {
    private static final Map<String, Map<String, ItemStack>> blockSlotData = new HashMap<>();
    private static final String PERSISTENT_DATA_KEY = "uijs_block_slots";
    private static boolean hasLoadedFromClient = false;

    /**
     * 保存方块插槽数据
     */
    public static void saveBlockSlot(String blockKey, String slotKey, ItemStack itemStack) {
        String normalizedSlotKey = normalizeSlotKey(slotKey);
        
        blockSlotData.computeIfAbsent(blockKey, k -> new HashMap<>())
                    .put(slotKey, itemStack.copy());

        saveToClientData(blockKey, slotKey, itemStack);
        
        UIJS.LOGGER.debug("Saved block slot: {}/{} - {}", blockKey, normalizedSlotKey, itemStack);
    }
    
    /**
     * 加载方块插槽数据
     */
    public static ItemStack loadBlockSlot(String blockKey, String slotKey) {
        // 确保使用统一的键格式
        String normalizedSlotKey = normalizeSlotKey(slotKey);
        
        // 首先尝试从内存加载
        Map<String, ItemStack> blockSlots = blockSlotData.get(blockKey);
        if (blockSlots != null && blockSlots.containsKey(normalizedSlotKey)) {
            ItemStack stack = blockSlots.get(normalizedSlotKey).copy();
            UIJS.LOGGER.debug("Loaded block slot from memory: {}/{} - {}", blockKey, normalizedSlotKey, stack);
            return stack;
        }
        
        // 如果内存中没有，从持久化存储加载
        ItemStack stack = loadFromClientData(blockKey, normalizedSlotKey);
        if (!stack.isEmpty()) {
            // 更新内存存储
            blockSlotData.computeIfAbsent(blockKey, k -> new HashMap<>())
                        .put(normalizedSlotKey, stack.copy());
            UIJS.LOGGER.debug("Loaded block slot from client data: {}/{} - {}", blockKey, normalizedSlotKey, stack);
        }
        
        return stack;
    }

    /**
     * 标准化插槽键格式
     * 确保客户端存储和服务器同步使用相同的键格式
     */
    private static String normalizeSlotKey(String slotKey) {
        if (slotKey == null || slotKey.isEmpty()) {
            return slotKey;
        }
        
        // 移除可能的前缀
        if (slotKey.startsWith("block_")) {
            slotKey = slotKey.substring(6);
        }
        
        // 修复格式问题：将斜杠替换回下划线
        if (slotKey.contains("/")) {
            // 找到第一个斜杠，将其替换为下划线
            int firstSlash = slotKey.indexOf('/');
            if (firstSlash > 0) {
                String dimensionPart = slotKey.substring(0, firstSlash);
                String coordPart = slotKey.substring(firstSlash + 1);
                slotKey = dimensionPart + "_" + coordPart;
                UIJS.LOGGER.debug("Fixed slot key format: {} -> {}", slotKey, slotKey);
            }
        }
        
        return slotKey;
    }
    
    /**
     * 从服务器数据恢复方块插槽数据
     */
    public static void restoreFromServerData(String blockKey, Map<String, ItemStack> serverData) {
        if (serverData != null && !serverData.isEmpty()) {
            UIJS.LOGGER.info("Restoring block slot data for {}: {} slots", blockKey, serverData.size());
            
            // 清空现有数据
            if (blockSlotData.containsKey(blockKey)) {
                blockSlotData.get(blockKey).clear();
            } else {
                blockSlotData.put(blockKey, new HashMap<>());
            }
            
            // 恢复服务器数据，使用标准化键
            for (Map.Entry<String, ItemStack> entry : serverData.entrySet()) {
                String normalizedKey = normalizeSlotKey(entry.getKey());
                blockSlotData.get(blockKey).put(normalizedKey, entry.getValue().copy());
                // 同时保存到持久化数据
                saveToClientData(blockKey, normalizedKey, entry.getValue());
                UIJS.LOGGER.debug("Restored slot: {}/{} -> {}", blockKey, normalizedKey, entry.getValue());
            }
            
            UIJS.LOGGER.info("Successfully restored block slot data for {}: {} slots", blockKey, serverData.size());
        }
    }

    /**
     * 获取方块插槽数据
     */
    public static Map<String, ItemStack> getBlockSlots(String blockKey) {
        // 确保数据已加载
        ensureDataLoaded(blockKey);
        return new HashMap<>(blockSlotData.getOrDefault(blockKey, new HashMap<>()));
    }

    /**
     * 确保数据已加载
     */
    private static void ensureDataLoaded(String blockKey) {
        if (!blockSlotData.containsKey(blockKey)) {
            // 尝试从客户端数据加载
            loadBlockDataFromClient(blockKey);
        }
    }

    /**
     * 从客户端数据加载特定方块的数据
     */
    private static void loadBlockDataFromClient(String blockKey) {
        try {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                CompoundTag persistentData = player.getPersistentData();
                CompoundTag blockData = persistentData.getCompound(PERSISTENT_DATA_KEY);
                CompoundTag slotData = blockData.getCompound(blockKey);
                
                Map<String, ItemStack> slots = new HashMap<>();
                for (String slotKey : slotData.getAllKeys()) {
                    CompoundTag itemTag = slotData.getCompound(slotKey);
                    ItemStack itemStack = ItemStack.of(itemTag);
                    slots.put(slotKey, itemStack.copy());
                }
                
                if (!slots.isEmpty()) {
                    blockSlotData.put(blockKey, slots);
                    UIJS.LOGGER.debug("Loaded {} slots for block {} from client data", slots.size(), blockKey);
                }
            }
        } catch (Exception e) {
            UIJS.LOGGER.error("Failed to load block data for {} from client: {}", blockKey, e.getMessage());
        }
    }

    /**
     * 清空特定方块的插槽数据
     */
    public static void clearBlockSlots(String blockKey) {
        blockSlotData.remove(blockKey);
        clearClientData(blockKey);
    }
    
    /**
     * 获取所有方块插槽数据的快照
     */
    public static Map<String, Map<String, ItemStack>> getAllBlockSlotDataSnapshot() {
        loadAllFromClientData();
        Map<String, Map<String, ItemStack>> snapshot = new HashMap<>();
        for (Map.Entry<String, Map<String, ItemStack>> entry : blockSlotData.entrySet()) {
            snapshot.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        return snapshot;
    }


    /**
     * 保存到客户端持久化数据
     */
    private static void saveToClientData(String blockKey, String slotKey, ItemStack itemStack) {
        try {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                CompoundTag persistentData = player.getPersistentData();
                CompoundTag blockData = persistentData.getCompound(PERSISTENT_DATA_KEY);
                CompoundTag slotData = blockData.getCompound(blockKey);
                
                if (!itemStack.isEmpty()) {
                    CompoundTag itemTag = new CompoundTag();
                    itemStack.save(itemTag);
                    slotData.put(slotKey, itemTag);
                } else {
                    slotData.remove(slotKey);
                }
                
                blockData.put(blockKey, slotData);
                persistentData.put(PERSISTENT_DATA_KEY, blockData);
                
                UIJS.LOGGER.debug("Saved block slot to client data: {}/{}", blockKey, slotKey);
            }
        } catch (Exception e) {
            UIJS.LOGGER.error("Failed to save block slot data to client: {}", e.getMessage());
        }
    }
    
    /**
     * 从客户端持久化数据加载
     */
    private static ItemStack loadFromClientData(String blockKey, String slotKey) {
        try {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                CompoundTag persistentData = player.getPersistentData();
                CompoundTag blockData = persistentData.getCompound(PERSISTENT_DATA_KEY);
                CompoundTag slotData = blockData.getCompound(blockKey);
                
                if (slotData.contains(slotKey)) {
                    CompoundTag itemTag = slotData.getCompound(slotKey);
                    ItemStack itemStack = ItemStack.of(itemTag);
                    
                    UIJS.LOGGER.debug("Loaded block slot from client data: {}/{}", blockKey, slotKey);
                    return itemStack;
                }
            }
        } catch (Exception e) {
            UIJS.LOGGER.error("Failed to load block slot data from client: {}", e.getMessage());
        }
        
        return ItemStack.EMPTY;
    }
    
    /**
     * 清空客户端持久化数据
     */
    private static void clearClientData(String blockKey) {
        try {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                CompoundTag persistentData = player.getPersistentData();
                CompoundTag blockData = persistentData.getCompound(PERSISTENT_DATA_KEY);
                blockData.remove(blockKey);
                persistentData.put(PERSISTENT_DATA_KEY, blockData);
                UIJS.LOGGER.debug("Cleared client data for block: {}", blockKey);
            }
        } catch (Exception e) {
            UIJS.LOGGER.error("Failed to clear block slot data from client: {}", e.getMessage());
        }
    }
    
    /**
     * 从客户端持久化数据加载所有方块插槽数据（在游戏启动时调用）
     */
    public static void loadAllFromClientData() {
        if (hasLoadedFromClient) {
            return; // 避免重复加载
        }
        
        try {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                CompoundTag persistentData = player.getPersistentData();
                CompoundTag blockData = persistentData.getCompound(PERSISTENT_DATA_KEY);
                
                for (String blockKey : blockData.getAllKeys()) {
                    CompoundTag slotData = blockData.getCompound(blockKey);
                    Map<String, ItemStack> slots = new HashMap<>();
                    
                    for (String slotKey : slotData.getAllKeys()) {
                        CompoundTag itemTag = slotData.getCompound(slotKey);
                        ItemStack itemStack = ItemStack.of(itemTag);
                        slots.put(slotKey, itemStack.copy());
                    }
                    
                    blockSlotData.put(blockKey, slots);
                    UIJS.LOGGER.info("Loaded {} slots for block {} from client data", slots.size(), blockKey);
                }
                
                hasLoadedFromClient = true;
                UIJS.LOGGER.info("Loaded all block slot data from client storage");
            }
        } catch (Exception e) {
            UIJS.LOGGER.error("Failed to load all block slot data from client: {}", e.getMessage());
        }
    }

    /**
     * 重置加载标志（用于重新登录时重新加载）
     */
    public static void resetLoadFlag() {
        hasLoadedFromClient = false;
    }
}
