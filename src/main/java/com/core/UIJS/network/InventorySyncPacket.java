package com.core.UIJS.network;

import java.util.UUID;
import java.util.function.Supplier;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public class InventorySyncPacket {
    private final int slotIndex;
    private final ItemStack slotItem;
    private final UUID playerUUID;

    public InventorySyncPacket(int slotIndex, ItemStack slotItem, UUID playerUUID) {
        this.slotIndex = slotIndex;
        this.slotItem = slotItem;
        this.playerUUID = playerUUID;
    }

    public static void encode(InventorySyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.slotIndex);
        
        CompoundTag slotTag = new CompoundTag();
        packet.slotItem.save(slotTag);
        buffer.writeNbt(slotTag);
        
        buffer.writeUUID(packet.playerUUID);
    }

    public static InventorySyncPacket decode(FriendlyByteBuf buffer) {
        int slotIndex = buffer.readInt();
        CompoundTag slotTag = buffer.readNbt();
        
        ItemStack slotItem = ItemStack.of(slotTag != null ? slotTag : new CompoundTag());
        UUID playerUUID = buffer.readUUID();
        
        return new InventorySyncPacket(slotIndex, slotItem, playerUUID);
    }

    public static void handle(InventorySyncPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer serverPlayer = context.get().getSender();
            if (serverPlayer != null && serverPlayer.getUUID().equals(packet.playerUUID)) {
                // 服务器端验证和处理
                handleServerSide(serverPlayer, packet);
            }
        });
        context.get().setPacketHandled(true);
    }

    private static void handleServerSide(ServerPlayer player, InventorySyncPacket packet) {
        // 验证玩家身份和插槽索引
        if (isValidTransaction(player, packet)) {
            // 更新服务器端的物品栏
            if (packet.slotIndex >= 0 && packet.slotIndex < player.getInventory().getContainerSize()) {
                player.getInventory().setItem(packet.slotIndex, packet.slotItem);
                player.getInventory().setChanged();
            }
        }
    }

    private static boolean isValidTransaction(ServerPlayer player, InventorySyncPacket packet) {
        // 验证玩家身份
        if (!player.getUUID().equals(packet.playerUUID)) {
            return false;
        }
        
        // 验证插槽索引
        if (packet.slotIndex < 0 || packet.slotIndex >= player.getInventory().getContainerSize()) {
            return false;
        }
        
        return true;
    }
}
