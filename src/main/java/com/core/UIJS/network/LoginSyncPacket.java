package com.core.UIJS.network;

import java.util.function.Supplier;

import com.core.UIJS.storage.SlotDataManager;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public class LoginSyncPacket {
    private final CompoundTag slotData;
    private final CompoundTag blockSlotData;

    public LoginSyncPacket(CompoundTag slotData, CompoundTag blockSlotData) {
        this.slotData = slotData;
        this.blockSlotData = blockSlotData;
    }

    public static void encode(LoginSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeNbt(packet.slotData);
        buffer.writeNbt(packet.blockSlotData);
    }

    public static LoginSyncPacket decode(FriendlyByteBuf buffer) {
        return new LoginSyncPacket(buffer.readNbt(), buffer.readNbt());
    }

    public static void handle(LoginSyncPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            // 客户端处理：恢复插槽数据
            CompoundTag data = packet.slotData;
            CompoundTag blockData = packet.blockSlotData;
            
            if (data != null) {
                // 清除客户端现有数据
                SlotDataManager.clearAllSlotData();
                
                // 恢复所有插槽数据
                for (String slotKey : data.getAllKeys()) {
                    CompoundTag itemTag = data.getCompound(slotKey);
                    if (itemTag != null && !itemTag.isEmpty()) {
                        SlotDataManager.saveSlotData(slotKey, ItemStack.of(itemTag), false);
                    }
                }
            }
            
            // 恢复方块插槽数据 - 使用新的恢复方法
            if (blockData != null) {
                SlotDataManager.restoreBlockSlotDataFromServer(blockData);
            }
        });
        context.get().setPacketHandled(true);
    }
}
