package com.core.UIJS.event;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.core.UIJS.UIJS;
import com.core.UIJS.mcml.MCMLNode;
import com.core.UIJS.network.NetworkHandler;
import com.core.UIJS.network.OpenUIPacket;
import com.core.UIJS.ui.UIRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

// UI交互事件处理
@Mod.EventBusSubscriber(modid = UIJS.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class UIInteractionHandler {
    private static final Logger LOGGER = LogManager.getLogger("UIJS-InteractionHandler");
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide()) return;
        Player player = event.getEntity();
        ItemStack itemStack = event.getItemStack();
        
        if (itemStack.isEmpty()) return;
        
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(itemStack.getItem());
        if (itemId == null) return;
        
        // 检查物品是否绑定了UI
        ResourceLocation uiId = UIRegistry.getUIForItem(itemId);
        if (uiId != null) {
            // 打开UI逻辑
            openUI(player, uiId, "item", itemId.toString(), event.getPos());
            event.setCanceled(true);
            LOGGER.debug("Opened UI {} for item {} clicked by player {}", uiId, itemId, player.getName().getString());
        }
    }
    
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        Player player = event.getEntity();
        BlockState blockState = event.getLevel().getBlockState(event.getPos());
        
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(blockState.getBlock());
        if (blockId == null) return;
        
        // 检查方块是否绑定了UI
        ResourceLocation uiId = UIRegistry.getUIForBlock(blockId);
        if (uiId != null) {
            // 打开UI逻辑
            
            openUI(player, uiId, "block", blockId.toString(), event.getPos());
            event.setCanceled(true);
            LOGGER.debug("Opened UI {} for block {} clicked by player {}", uiId, blockId, player.getName().getString());
        }
    }
    
    private static void openUI(Player player, ResourceLocation uiId, String type, String targetId, BlockPos blockPos) {
        if (!player.level().isClientSide) {
            
            // 获取UI数据
            MCMLNode uiNode = UIRegistry.getUI(uiId);
            if (uiNode != null) {
                // 在客户端打开UI
                if (player instanceof ServerPlayer serverPlayer) {
                    openUIOnClient(serverPlayer, uiId, uiNode, blockPos);
                }
            }
        }
    }

    private static void openUIOnClient(ServerPlayer player, ResourceLocation uiId, MCMLNode uiNode, BlockPos blockPos) {
        try {
            // 检查网络通道是否已注册
            if (NetworkHandler.INSTANCE == null) {
                LOGGER.error("Network channel is null!");
                return;
            }

            OpenUIPacket packet;
            if (UIRegistry.isBlockBoundUI(uiId)) {
                packet = new OpenUIPacket(uiId, blockPos, player.level().dimension().location());
            } else {
                packet = new OpenUIPacket(uiId);
            }
            
            NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
        } catch (Exception e) {
            LOGGER.error("Failed to send UI packet to client", e);
        }
    }
}
