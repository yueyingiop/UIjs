package com.core.UIJS.storage;

import java.util.HashMap;
import java.util.Map;

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
     * 验证物品是否属于玩家
     */
    private boolean isItemOwnedByPlayer(ServerPlayer player, ItemStack itemStack) {
        // 这里可以添加更复杂的验证逻辑
        // 暂时返回true，在更高级的版本中可以验证物品来源
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
}
