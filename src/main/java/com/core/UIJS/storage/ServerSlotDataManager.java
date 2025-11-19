package com.core.UIJS.storage;

import java.util.HashMap;
import java.util.Map;

import com.core.UIJS.UIJS;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

public class ServerSlotDataManager extends SavedData {
    private static final String DATA_NAME = "uijs_server_slots";
    private final Map<String, CompoundTag> slotData = new HashMap<>();

    public ServerSlotDataManager() {
        super();
    }

    public ServerSlotDataManager(CompoundTag tag) {
        CompoundTag slotsTag = tag.getCompound("slots");
        for (String key : slotsTag.getAllKeys()) {
            slotData.put(key, slotsTag.getCompound(key));
        }
    }

    @Override
    public CompoundTag save(CompoundTag compound) {
        CompoundTag slotsTag = new CompoundTag();
        for (Map.Entry<String, CompoundTag> entry : slotData.entrySet()) {
            slotsTag.put(entry.getKey(), entry.getValue());
        }
        compound.put("slots", slotsTag);
        return compound;
    }

    /**
     * 保存玩家插槽数据
     */
    public void savePlayerSlot(ServerPlayer player, String slotKey, ItemStack itemStack) {
        String playerKey = player.getUUID().toString() + "_" + slotKey;
        
        // 验证物品是否属于玩家
        if (!itemStack.isEmpty() && !isItemOwnedByPlayer(player, itemStack)) {
            System.err.println("Attempt to save unowned item to slot: " + slotKey);
            return;
        }
        
        if (!itemStack.isEmpty()) {
            CompoundTag itemTag = new CompoundTag();
            itemStack.save(itemTag);
            slotData.put(playerKey, itemTag);
        } else {
            slotData.remove(playerKey);
        }
        
        setDirty();
    }

    /**
     * 保存特定方块的玩家插槽数据
     */
    public void savePlayerBlockSlot(ServerPlayer player, String blockKey, String slotKey, ItemStack itemStack) {
        String fullKey = "block_" + blockKey + "_" + slotKey + "_" + player.getUUID().toString();
        
        if (!itemStack.isEmpty()) {
            CompoundTag itemTag = new CompoundTag();
            itemStack.save(itemTag);
            slotData.put(fullKey, itemTag);
        } else {
            slotData.remove(fullKey);
        }
        
        setDirty();
    }

    /**
     * 验证物品是否属于玩家
     */
    private boolean isItemOwnedByPlayer(ServerPlayer player, ItemStack itemStack) {
        // 默认返回true
        return true;
    }


    /**
     * 加载玩家插槽数据
     */
    public ItemStack loadPlayerSlot(ServerPlayer player, String slotKey) {
        String playerKey = player.getUUID().toString() + "_" + slotKey;
        if (slotData.containsKey(playerKey)) {
            CompoundTag itemTag = slotData.get(playerKey);
            return ItemStack.of(itemTag);
        }
        return ItemStack.EMPTY;
    }

    /**
     * 加载特定方块的玩家插槽数据
     */
    public ItemStack loadPlayerBlockSlot(ServerPlayer player, String blockKey, String slotKey) {
        String fullKey = "block_" + blockKey + "_" + slotKey + "_" + player.getUUID().toString();
        if (slotData.containsKey(fullKey)) {
            CompoundTag itemTag = slotData.get(fullKey);
            ItemStack itemStack = ItemStack.of(itemTag);
            return itemStack;
        }
        return ItemStack.EMPTY;
    }

    /**
     * 获取玩家所有方块插槽数据
     */
    public CompoundTag getPlayerBlockSlotData(ServerPlayer player) {
        CompoundTag blockData = new CompoundTag();
        String playerUUID = player.getUUID().toString();
        
        for (Map.Entry<String, CompoundTag> entry : slotData.entrySet()) {
            String key = entry.getKey();
            // 查找属于该玩家的方块插槽数据
            if (key.startsWith("block_") && key.endsWith("_" + playerUUID)) {
                // 提取blockKey和slotKey
                // 格式: block_{blockKey}_{slotKey}_{playerUUID}
                String middlePart = key.substring(6, key.length() - (playerUUID.length() + 1));
                String[] parts = middlePart.split("_", 2);
                if (parts.length == 2) {
                    String blockKey = parts[0];
                    String slotKey = parts[1];
                    
                    // 确保使用正确的格式：blockKey_slotKey（不使用斜杠）
                    String fullSlotKey = blockKey + "_" + slotKey;
                    blockData.put(fullSlotKey, entry.getValue().copy());
                    
                    UIJS.LOGGER.debug("Preparing block data for sync: {} -> {}", fullSlotKey, ItemStack.of(entry.getValue()));
                }
            }
        }
        
        UIJS.LOGGER.info("Prepared {} block slot entries for player {}", blockData.getAllKeys().size(), player.getScoreboardName());
        return blockData;
    }

    /**
     * 获取所有玩家的插槽数据
     */
    public Map<String, CompoundTag> getAllSlotData() {
        return new HashMap<>(slotData);
    }

    /**
     * 获取服务器数据实例
     */
    public static ServerSlotDataManager get(ServerPlayer player) {
        if (player.getServer() == null) return new ServerSlotDataManager();
        
        DimensionDataStorage storage = player.getServer().overworld().getDataStorage();
        return storage.computeIfAbsent(ServerSlotDataManager::new, ServerSlotDataManager::new, DATA_NAME);
    }

    /**
     * 从服务器数据恢复方块插槽数据
     */
    public static void restoreBlockSlotDataFromServer(CompoundTag blockSlotData) {
        if (blockSlotData != null) {
            for (String fullSlotKey : blockSlotData.getAllKeys()) {
                CompoundTag itemTag = blockSlotData.getCompound(fullSlotKey);
                if (itemTag != null && !itemTag.isEmpty()) {
                    // 解析完整的插槽键：blockKey_slotKey
                    String[] parts = fullSlotKey.split("_", 2);
                    if (parts.length == 2) {
                        String blockKey = parts[0];
                        String slotKey = parts[1];
                        
                        // 解析slotKey中的type, order, bindGroup
                        String[] slotParts = slotKey.split("_");
                        if (slotParts.length >= 3) {
                            // 保存到方块数据管理器
                            ItemStack itemStack = ItemStack.of(itemTag);
                            BlockBoundSlotDataManager.saveBlockSlot(blockKey, slotKey, itemStack);
                        }
                    }
                }
            }
        }
    }

    /**
     * 获取特定方块的所有插槽数据
     */
    public static Map<String, ItemStack> getBlockSlotData(String blockKey) {
        return BlockBoundSlotDataManager.getBlockSlots(blockKey);
    }

    /**
     * 静态方法：保存玩家插槽数据
     */
    public static void save(ServerPlayer player, String slotKey, ItemStack itemStack) {
        ServerSlotDataManager manager = get(player);
        manager.savePlayerSlot(player, slotKey, itemStack);
    }

    /**
     * 静态方法：加载玩家插槽数据
     */
    public static ItemStack load(ServerPlayer player, String slotKey) {
        ServerSlotDataManager manager = get(player);
        return manager.loadPlayerSlot(player, slotKey);
    }

    /**
     * 静态方法：保存特定方块绑定的玩家插槽数据
     */
    public static void saveBlockSlot(ServerPlayer player, String blockKey, String slotKey, ItemStack itemStack) {
        ServerSlotDataManager manager = get(player);
        manager.savePlayerBlockSlot(player, blockKey, slotKey, itemStack);
    }

    /**
     * 静态方法：保存特定方块绑定的玩家插槽数据
     */
    public static ItemStack loadBlockSlot(ServerPlayer player, String blockKey, String slotKey) {
        ServerSlotDataManager manager = get(player);
        return manager.loadPlayerBlockSlot(player, blockKey, slotKey);
    }

    /**
     * 静态方法：获取玩家所有方块插槽数据
     */
    public static CompoundTag getBlockSlotData(ServerPlayer player) {
        ServerSlotDataManager manager = get(player);
        return manager.getPlayerBlockSlotData(player);
    }
}
