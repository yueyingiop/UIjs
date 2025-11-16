package com.core.UIJS.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
     * 生成插槽唯一键
     */
    public static String generateSlotKey(String type, int order, String bindGroup) {
        return type + "_" + order + "_" + bindGroup;
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
