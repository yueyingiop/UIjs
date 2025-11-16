package com.core.UIJS.event;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.core.UIJS.UIJS;
import com.core.UIJS.kubejs.ArmorSet;
import com.core.UIJS.kubejs.ArmorSetRegistry;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = UIJS.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerTickHandler {
    private static final Map<UUID, Integer> playerTickCounters = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent event) {
        if (event.phase == Phase.END && !event.player.level().isClientSide()) {
            UUID playerId = event.player.getUUID();
            int tickCounter = playerTickCounters.getOrDefault(playerId, 0) + 1;
            playerTickCounters.put(playerId, tickCounter);
            
            // 检查是否需要检查装备
            if (tickCounter >= 20) {
                ArmorSetRegistry.checkPlayerEquipment(event.player);
                playerTickCounters.put(playerId, 0);
            }

            // 执行所有 armor set 的 tick 回调
            for (Map.Entry<String, ArmorSet> entry : ArmorSetRegistry.getArmorSets().entrySet()){
                ArmorSet set = entry.getValue();
                if (event.player instanceof ServerPlayer serverPlayer) {
                    set.onTick(serverPlayer);
                }
            }
        }
    }
}
