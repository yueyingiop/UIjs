package com.core.UIJS.network;

import java.util.UUID;
import java.util.function.Supplier;

import com.core.UIJS.storage.ServerSlotDataManager;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public class SlotSyncPacket {
    private final String slotKey;
    private final ItemStack slotItem;
    private final ItemStack carriedItem;
    private final UUID playerUUID;
    private final int inventorySlot;

    public SlotSyncPacket(String slotKey, ItemStack slotItem, ItemStack carriedItem, UUID playerUUID, int inventorySlot) {
        this.slotKey = slotKey;
        this.slotItem = slotItem;
        this.carriedItem = carriedItem;
        this.playerUUID = playerUUID;
        this.inventorySlot = inventorySlot;
    }

    // 编码
    public static void encode(SlotSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.slotKey);
        
        CompoundTag slotTag = new CompoundTag();
        packet.slotItem.save(slotTag);
        buffer.writeNbt(slotTag);
        
        CompoundTag carriedTag = new CompoundTag();
        packet.carriedItem.save(carriedTag);
        buffer.writeNbt(carriedTag);
        
        buffer.writeUUID(packet.playerUUID);
        buffer.writeInt(packet.inventorySlot);
    }

    // 解码
    public static SlotSyncPacket decode(FriendlyByteBuf buffer) {
        String slotKey = buffer.readUtf();
        CompoundTag slotTag = buffer.readNbt();
        CompoundTag carriedTag = buffer.readNbt();
        
        ItemStack slotItem = ItemStack.of(slotTag != null ? slotTag : new CompoundTag());
        ItemStack carriedItem = ItemStack.of(carriedTag != null ? carriedTag : new CompoundTag());
        
        UUID playerUUID = buffer.readUUID();
        int inventorySlot = buffer.readInt();
        
        return new SlotSyncPacket(slotKey, slotItem, carriedItem, playerUUID, inventorySlot);
    }

    // 处理
    public static void handle(SlotSyncPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer serverPlayer = context.get().getSender();
            if (serverPlayer != null && serverPlayer.getUUID().equals(packet.playerUUID)) {
                // 服务器端验证和处理
                handleServerSide(serverPlayer, packet);
            }
        });
        context.get().setPacketHandled(true);
    }

    // 服务器处理
    private static void handleServerSide(ServerPlayer player, SlotSyncPacket packet) {
        // 检查是否是方块插槽
        if (packet.slotKey.startsWith("block_")) {
            // 处理方块插槽同步
            handleBlockSlotSync(player, packet);
            return;
        }
        
        // 验证玩家是否真的有这些物品
        if (isValidTransaction(player, packet)) {
            // 更新服务器端的物品栏
            if (packet.inventorySlot >= 0 && packet.inventorySlot < player.getInventory().getContainerSize()) {
                ItemStack currentItem = player.getInventory().getItem(packet.inventorySlot);
                
                // 确保服务器端物品与客户端声称的一致
                if (ItemStack.isSameItemSameTags(currentItem, packet.carriedItem)) {
                    player.getInventory().setItem(packet.inventorySlot, packet.carriedItem);
                } else {
                    // 如果不一致，记录警告并拒绝操作
                    System.err.println("Inventory slot item mismatch for player: " + player.getScoreboardName());
                    return;
                }
            } else if (packet.inventorySlot == -1) {
                // 客户端无法确定来源插槽，需要服务器验证
                if (!packet.carriedItem.isEmpty()) {
                    // 检查玩家是否真的有这个物品（数量验证）
                    if (!playerHasItem(player, packet.carriedItem)) {
                        System.err.println("Player does not have claimed item: " + packet.carriedItem);
                        return;
                    }
                }
            }

            // 更新服务器端的手持物品状态
            player.containerMenu.setCarried(packet.carriedItem);
            
            // 保存插槽数据到服务器存储
            ServerSlotDataManager.save(player, packet.slotKey, packet.slotItem);
            
            // 保存玩家数据
            player.getInventory().setChanged();
        }
    }

    // 验证交换
    private static boolean isValidTransaction(ServerPlayer player, SlotSyncPacket packet) {
        // 验证玩家身份
        if (!player.getUUID().equals(packet.playerUUID)) {
            return false;
        }
        
        // 验证物品栏插槽索引
        if (packet.inventorySlot >= 0) {
            if (packet.inventorySlot >= player.getInventory().getContainerSize()) {
                return false;
            }
            
            ItemStack currentItem = player.getInventory().getItem(packet.inventorySlot);
            // 如果客户端声称有物品在某个插槽，服务器必须验证
            if (!currentItem.isEmpty() && !packet.carriedItem.isEmpty()) {
                if (!ItemStack.isSameItemSameTags(currentItem, packet.carriedItem)) {
                    return false;
                }
            }
        }
        
        return true;
    }

    /**
     * 检查玩家是否拥有指定物品
     */
    private static boolean playerHasItem(ServerPlayer player, ItemStack itemStack) {
        int totalCount = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (ItemStack.isSameItemSameTags(stack, itemStack)) {
                totalCount += stack.getCount();
            }
        }
        return totalCount >= itemStack.getCount();
    }

    /**
     * 处理方块插槽同步
     */
    private static void handleBlockSlotSync(ServerPlayer player, SlotSyncPacket packet) {
        // 解析方块键和插槽键
        String fullKey = packet.slotKey.substring(6); // 移除 "block_" 前缀
        String[] parts = fullKey.split("_", 2);
        if (parts.length < 2) return;
        
        String blockKey = parts[0];
        String slotKey = parts[1];
        
        // 验证玩家身份
        if (!player.getUUID().equals(packet.playerUUID)) {
            return;
        }
        
        // 保存到服务器存储 - 使用专门的方块数据保存方法
        ServerSlotDataManager.saveBlockSlot(player, blockKey, slotKey, packet.slotItem);
        
        // 更新服务器端的手持物品状态
        player.containerMenu.setCarried(packet.carriedItem);
        
        // 保存玩家数据
        player.getInventory().setChanged();
    }
}
