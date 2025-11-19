package com.core.UIJS.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.core.UIJS.UIJS;
import com.core.UIJS.network.NetworkHandler;
import com.core.UIJS.network.SlotSyncPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class SlotDataManager {
    private static final Map<String, ItemStack> slotData = new HashMap<>();
    private static final String PERSISTENT_DATA_KEY = "uijs_slots";
    private static final Map<String, UUID> slotOwners = new HashMap<>();
    
    //#region ITEM
    /**
     * 保存插槽数据到内存和持久化存储
     */
    public static void saveSlotData(String slotKey, ItemStack itemStack, boolean syncToServer) {
        if (!itemStack.isEmpty()) {
            slotData.put(slotKey, itemStack.copy());
            // 记录物品所有者
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                slotOwners.put(slotKey, player.getUUID());
            }
        } else {
            slotData.remove(slotKey);
            slotOwners.remove(slotKey);
        }
        
        saveToClientData(slotKey, itemStack);
        
        // 同步到服务器
        if (syncToServer) {
            syncSlotToServer(slotKey, itemStack);
        }
    }

    /**
     * 保存插槽数据（默认同步到服务器）
     */
    public static void saveSlotData(String slotKey, ItemStack itemStack) {
        saveSlotData(slotKey, itemStack, true);
    }

    /**
     * 从服务器接收插槽数据更新
     */
    public static void receiveSlotDataFromServer(String slotKey, ItemStack itemStack, UUID playerUUID) {
        // 验证数据来源
        if (isValidSlotUpdate(slotKey, playerUUID)) {
            slotData.put(slotKey, itemStack.copy());
            slotOwners.put(slotKey, playerUUID);
            saveToClientData(slotKey, itemStack);
        }
    }

    /**
     * 验证插槽更新是否有效
     */
    private static boolean isValidSlotUpdate(String slotKey, UUID playerUUID) {
        // 如果是自己的数据，总是接受
        Player player = Minecraft.getInstance().player;
        if (player != null && player.getUUID().equals(playerUUID)) {
            return true;
        }
        
        // 检查插槽所有者
        UUID owner = slotOwners.get(slotKey);
        return owner == null || owner.equals(playerUUID);
    }

    /**
     * 查找物品来源插槽
     */
    private static int findSourceInventorySlot(ItemStack carriedItem) {
        if (carriedItem.isEmpty()) return -1;
        
        Player player = Minecraft.getInstance().player;
        if (player == null) return -1;
        
        // 查找物品栏中匹配的物品堆栈
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (ItemStack.isSameItemSameTags(stack, carriedItem)) {
                // 如果数量也匹配，优先返回
                if (stack.getCount() == carriedItem.getCount()) {
                    return i;
                }
            }
        }
        
        return -1;
    }

    /**
     * 同步插槽数据到服务器
     */
    private static void syncSlotToServer(String slotKey, ItemStack itemStack) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            // 查找来源插槽
            int sourceSlot = findSourceInventorySlot(player.containerMenu.getCarried());
            
            // 验证物品所有权
            if (sourceSlot != -1) {
                ItemStack currentItem = player.getInventory().getItem(sourceSlot);
                if (!ItemStack.isSameItemSameTags(currentItem, player.containerMenu.getCarried())) {
                    // 物品不匹配，重置为-1
                    sourceSlot = -1;
                }
            }

            NetworkHandler.sendToServer(new SlotSyncPacket(
                slotKey, 
                itemStack, 
                player.containerMenu.getCarried(), 
                player.getUUID(), 
                sourceSlot
            ));
        }
    }

    /**
     * 从内存存储加载插槽数据
     */
    public static ItemStack loadSlotData(String slotKey) {
        if (slotData.containsKey(slotKey)) {
            return slotData.get(slotKey).copy();
        }
        
        // 如果内存中没有，尝试从持久化存储加载
        return loadFromClientData(slotKey);
    }

    /**
     * 保存到客户端持久化数据
     */
    private static void saveToClientData(String slotKey, ItemStack itemStack) {
        try {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                CompoundTag persistentData = player.getPersistentData();
                CompoundTag uiData = persistentData.getCompound(PERSISTENT_DATA_KEY);
                
                if (!itemStack.isEmpty()) {
                    CompoundTag itemTag = new CompoundTag();
                    itemStack.save(itemTag);
                    uiData.put(slotKey, itemTag);
                } else {
                    uiData.remove(slotKey);
                }
                
                persistentData.put(PERSISTENT_DATA_KEY, uiData);
            }
        } catch (Exception e) {
            System.err.println("Failed to save slot data: " + e.getMessage());
        }
    }

    /**
     * 从客户端持久化数据加载
     */
    private static ItemStack loadFromClientData(String slotKey) {
        try {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                CompoundTag persistentData = player.getPersistentData();
                CompoundTag uiData = persistentData.getCompound(PERSISTENT_DATA_KEY);
                
                if (uiData.contains(slotKey)) {
                    CompoundTag itemTag = uiData.getCompound(slotKey);
                    ItemStack itemStack = ItemStack.of(itemTag);
                    
                    // 同时更新内存存储
                    slotData.put(slotKey, itemStack.copy());
                    return itemStack;
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load slot data: " + e.getMessage());
        }
        
        return ItemStack.EMPTY;
    }

    /**
     * 清空所有插槽数据
     */
    public static void clearAllSlotData() {
        slotData.clear();
        
        // 同时清空持久化数据
        try {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                CompoundTag persistentData = player.getPersistentData();
                persistentData.remove(PERSISTENT_DATA_KEY);
            }
        } catch (Exception e) {
            System.err.println("Failed to clear slot data: " + e.getMessage());
        }
    }
    //#endregion

    //#region BLOCK
    /**
     * 保存特定方块的插槽数据
     */
    public static void saveBlockSlotData(String blockKey, String type, int order, String bindGroup, ItemStack itemStack, boolean syncToServer) {
        String slotKey = generateSlotKey(type, order, bindGroup);

        // 使用专门的方块数据管理器
        BlockBoundSlotDataManager.saveBlockSlot(blockKey, slotKey, itemStack);
        
        // 同步到服务器
        if (syncToServer) {
            syncBlockSlotToServer(blockKey, type, order, bindGroup, itemStack);
        }
    }

    public static void saveBlockSlotData(String blockKey, String type, int order, String bindGroup, ItemStack itemStack) {
        saveBlockSlotData(blockKey, type, order, bindGroup, itemStack, true);
    }

    /**
     * 加载特定方块的插槽数据
     */
    public static ItemStack loadBlockSlotData(String blockKey, String type, int order, String bindGroup) {
        String slotKey = generateSlotKey(type, order, bindGroup);
        return BlockBoundSlotDataManager.loadBlockSlot(blockKey, slotKey);
    }

    /**
     * 同步方块插槽数据到服务器
     */
    private static void syncBlockSlotToServer(String blockKey, String type, int order, String bindGroup, ItemStack itemStack) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player != null) {
            String slotKey = generateSlotKey(type, order, bindGroup);
            
            // 查找来源插槽
            int sourceSlot = findSourceInventorySlot(player.containerMenu.getCarried());
            
            // 验证物品所有权
            if (sourceSlot != -1) {
                ItemStack currentItem = player.getInventory().getItem(sourceSlot);
                if (!ItemStack.isSameItemSameTags(currentItem, player.containerMenu.getCarried())) {
                    sourceSlot = -1;
                }
            }

            // 发送方块特定的同步包
            NetworkHandler.sendToServer(new SlotSyncPacket(
                "block_" + blockKey + "_" + slotKey, // 关键：包含方块key
                itemStack, 
                player.containerMenu.getCarried(), 
                player.getUUID(), 
                sourceSlot
            ));
        }
    }

    /**
     * 从服务器数据恢复方块插槽数据
     */
    public static void restoreBlockSlotDataFromServer(CompoundTag blockSlotData) {
        if (blockSlotData != null && !blockSlotData.isEmpty()) {
            UIJS.LOGGER.info("Restoring block slot data from server: {} entries", blockSlotData.getAllKeys().size());
            
            // 按方块键分组数据
            Map<String, Map<String, ItemStack>> blockDataMap = new HashMap<>();
            
            for (String fullSlotKey : blockSlotData.getAllKeys()) {
                CompoundTag itemTag = blockSlotData.getCompound(fullSlotKey);
                if (itemTag != null && !itemTag.isEmpty()) {
                    // 解析完整的插槽键：blockKey_slotKey
                    // 注意：服务器发送的格式可能是：minecraft:overworld/9_-47_14_input_0_p2
                    // 我们需要正确解析出方块键和插槽键
                    String[] parts = parseBlockSlotKey(fullSlotKey);
                    if (parts != null && parts.length == 2) {
                        String blockKey = parts[0];
                        String slotKey = parts[1];
                        
                        ItemStack itemStack = ItemStack.of(itemTag);
                        blockDataMap.computeIfAbsent(blockKey, k -> new HashMap<>())
                                .put(slotKey, itemStack);
                        
                        UIJS.LOGGER.debug("Restoring slot: {} -> {} (parsed as: {}/{})", 
                            fullSlotKey, itemStack, blockKey, slotKey);
                    } else {
                        UIJS.LOGGER.warn("Failed to parse block slot key: {}", fullSlotKey);
                    }
                }
            }
            
            // 恢复每个方块的数据
            for (Map.Entry<String, Map<String, ItemStack>> entry : blockDataMap.entrySet()) {
                BlockBoundSlotDataManager.restoreFromServerData(entry.getKey(), entry.getValue());
            }
            
            UIJS.LOGGER.info("Successfully restored block slot data from server: {} blocks", blockDataMap.size());
        } else {
            UIJS.LOGGER.warn("No block slot data received from server or data is empty");
        }
    }

    private static String[] parseBlockSlotKey(String fullSlotKey) {
        if (fullSlotKey == null || fullSlotKey.isEmpty()) {
            return null;
        }
        
        // 移除block_前缀
        if (fullSlotKey.startsWith("block_")) {
            fullSlotKey = fullSlotKey.substring(6);
        }
        
        // 处理斜杠格式
        if (fullSlotKey.contains("/")) {
            int slashIndex = fullSlotKey.indexOf('/');
            String blockKey = fullSlotKey.substring(0, slashIndex);
            String slotKey = fullSlotKey.substring(slashIndex + 1);
            
            // 修复方块键格式：将坐标部分合并
            String[] coordParts = slotKey.split("_", 3);
            if (coordParts.length >= 3) {
                // 重新构建方块键
                blockKey = blockKey + "_" + coordParts[0] + "_" + coordParts[1] + "_" + coordParts[2];
                // 剩余的作为插槽键
                if (coordParts.length > 3) {
                    slotKey = slotKey.substring(coordParts[0].length() + coordParts[1].length() + coordParts[2].length() + 3);
                } else {
                    slotKey = "";
                }
            }
            
            return new String[]{blockKey, slotKey};
        } else {
            // 处理下划线格式：minecraft:overworld_9_-47_14_input_0_p2
            String[] allParts = fullSlotKey.split("_");
            if (allParts.length >= 4) {
                // 前4部分：minecraft:overworld, 9, -47, 14 组成方块键
                StringBuilder blockKeyBuilder = new StringBuilder(allParts[0]);
                for (int i = 1; i < 4; i++) {
                    blockKeyBuilder.append("_").append(allParts[i]);
                }
                String blockKey = blockKeyBuilder.toString();
                
                // 剩余部分作为插槽键
                StringBuilder slotKeyBuilder = new StringBuilder();
                for (int i = 4; i < allParts.length; i++) {
                    if (slotKeyBuilder.length() > 0) {
                        slotKeyBuilder.append("_");
                    }
                    slotKeyBuilder.append(allParts[i]);
                }
                String slotKey = slotKeyBuilder.toString();
                
                return new String[]{blockKey, slotKey};
            }
        }
        
        return null;
    }
    //#endregion

    /**
     * 获取特定插槽的数据
     */
    public static ItemStack getSlotData(String slotKey) {
        return loadSlotData(slotKey);
    }

    /**
     * 获取特定插槽的数据（通过参数）
     */
    public static ItemStack getSlotData(String type, int order, String bindGroup) {
        String slotKey = generateSlotKey(type, order, bindGroup);
        return getSlotData(slotKey);
    }

    /**
     * 获取方块特定的插槽数据
     */
    public static ItemStack getBlockSlotData(String blockKey, String type, int order, String bindGroup) {
        return loadBlockSlotData(blockKey, type, order, bindGroup);
    }

    /**
     * 生成插槽唯一键
     */
    public static String generateSlotKey(String type, int order, String bindGroup) {
        return type + "_" + order + "_" + bindGroup;
    }

    /**
     * 生成特定方块的插槽键
     */
    public static String generateBlockSlotKey(String blockKey, String type, int order, String bindGroup) {
        return blockKey + "_" + type + "_" + order + "_" + bindGroup;
    }

    /**
     * 检查插槽数据是否存在
     */
    public static boolean hasSlotData(String slotKey) {
        return slotData.containsKey(slotKey) || 
                (
                    Minecraft.getInstance().player != null && 
                    Minecraft.getInstance().player.getPersistentData()
                        .getCompound(PERSISTENT_DATA_KEY).contains(slotKey)
                );
    }

    /**
     * 获取所有插槽数据的快照
     */
    public static Map<String, ItemStack> getAllSlotDataSnapshot() {
        return new HashMap<>(slotData);
    }

    /**
     * 从快照恢复所有插槽数据
     */
    public static void restoreFromSnapshot(Map<String, ItemStack> snapshot) {
        slotData.clear();
        slotData.putAll(snapshot);
        
        // 持久化所有数据
        for (Map.Entry<String, ItemStack> entry : snapshot.entrySet()) {
            saveToClientData(entry.getKey(), entry.getValue());
        }
    }
}
