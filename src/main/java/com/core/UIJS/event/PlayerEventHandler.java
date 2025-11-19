package com.core.UIJS.event;

import java.util.Map;

import com.core.UIJS.UIJS;
import com.core.UIJS.network.LoginSyncPacket;
import com.core.UIJS.network.NetworkHandler;
import com.core.UIJS.storage.ServerSlotDataManager;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = UIJS.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerEventHandler {
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            syncPlayerSlotData(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        // if (event.getEntity() instanceof ServerPlayer serverPlayer) {
        //     syncPlayerSlotData(serverPlayer);
        // }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // 玩家切换维度时，同步插槽数据
            syncPlayerSlotData(serverPlayer);
        }
    }

    /**
     * 同步玩家插槽数据到客户端
     */
    private static void syncPlayerSlotData(ServerPlayer player) {
        try {
            // 获取服务器端保存的玩家插槽数据
            ServerSlotDataManager manager = ServerSlotDataManager.get(player);
            Map<String, CompoundTag> allSlotData = manager.getAllSlotData();
            
            // 过滤出当前玩家的数据
            String playerPrefix = player.getUUID().toString() + "_";
            CompoundTag playerSlotData = new CompoundTag();
            
            for (Map.Entry<String, CompoundTag> entry : allSlotData.entrySet()) {
                String slotKey = entry.getKey();
                if (slotKey.startsWith(playerPrefix)) {
                    // 移除玩家UUID前缀，只保留原始的slotKey
                    String cleanSlotKey = slotKey.substring(playerPrefix.length());
                    playerSlotData.put(cleanSlotKey, entry.getValue());
                }
            }
            
            // 获取方块插槽数据
            CompoundTag blockSlotData = ServerSlotDataManager.getBlockSlotData(player);

            // 发送同步包到客户端
            if (!playerSlotData.isEmpty()) {
                NetworkHandler.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new LoginSyncPacket(playerSlotData, blockSlotData)
                );
            }
            
        } catch (Exception e) {
            System.err.println("Failed to sync slot data for player " + player.getScoreboardName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

}
