package com.core.UIJS.event;

import com.core.UIJS.UIJS;
import com.core.UIJS.recipe.RecipeManager;
import com.core.UIJS.storage.BlockBoundSlotDataManager;

import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = UIJS.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEventHandler {
    private static int backgroundTickCounter = 0;
    private static boolean hasLoadedBlockData = false;
    private static int retryCount = 0;
    private static final int MAX_RETRY_COUNT = 20; // 最多重试20次
    
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            // 在每个客户端刻的结束时更新配方进度
            RecipeManager.getInstance().updateRecipeProgress();
            
            // 每5刻更新一次后台配方（减少性能消耗）
            backgroundTickCounter++;
            if (backgroundTickCounter >= 5) {
                RecipeManager.getInstance().updateAllBackgroundRecipes();
                backgroundTickCounter = 0;
            }

            // 在玩家可用时加载方块插槽数据（带重试机制）
            if (!hasLoadedBlockData && Minecraft.getInstance().player != null) {
                if (retryCount < MAX_RETRY_COUNT) {
                    try {
                        BlockBoundSlotDataManager.loadAllFromClientData();
                        hasLoadedBlockData = true;
                        UIJS.LOGGER.info("Successfully loaded block slot data from client storage");
                    } catch (Exception e) {
                        retryCount++;
                        if (retryCount >= MAX_RETRY_COUNT) {
                            UIJS.LOGGER.error("Failed to load block slot data after {} attempts: {}", MAX_RETRY_COUNT, e.getMessage());
                        } else {
                            UIJS.LOGGER.warn("Attempt {} to load block slot data failed, retrying...", retryCount);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        // 重置标志，以便在重新登录时重新加载数据
        hasLoadedBlockData = false;
        retryCount = 0;
        BlockBoundSlotDataManager.resetLoadFlag();
        UIJS.LOGGER.info("Reset block data load flags for re-login");
    }
    
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        // 玩家登出时标记数据需要重新加载
        hasLoadedBlockData = false;
        UIJS.LOGGER.info("Player logged out, block data will be reloaded");
    }
}
