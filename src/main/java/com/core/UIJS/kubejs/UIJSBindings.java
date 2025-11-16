package com.core.UIJS.kubejs;

import java.nio.file.Path;

import com.core.UIJS.ui.UIRegistry;
import com.core.UIJS.ui.UIRegistry.BindingType;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.latvian.mods.kubejs.KubeJS;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

public class UIJSBindings {
    /**
     * 从文件注册UI
     */
    public boolean registerUI(String uiId, String filePath) {
        try {
            Path mcmlPath = KubeJS.getGameDirectory().resolve(filePath);
            ResourceLocation location = ResourceLocation.parse(uiId);
            return UIRegistry.registerUI(location, mcmlPath);
        } catch (Exception e) {
            KubeJS.LOGGER.error("Failed to register UI {} from file {}", uiId, filePath, e);
            return false;
        }
    }
    
    /**
     * 从字符串内容注册UI
     */
    public boolean registerUIFromString(String uiId, String mcmlContent) {
        try {
            ResourceLocation location = ResourceLocation.parse(uiId);
            return UIRegistry.registerUIFromString(location, mcmlContent);
        } catch (Exception e) {
            KubeJS.LOGGER.error("Failed to register UI {} from string", uiId, e);
            return false;
        }
    }
    
    /**
     * 将UI绑定到物品
     */
    public void bindUIToItem(String itemId, String uiId) {
        try {
            ResourceLocation itemLocation = ResourceLocation.parse(itemId);
            ResourceLocation uiLocation = ResourceLocation.parse(uiId);
            UIRegistry.bindUIToItem(itemLocation, uiLocation);
        } catch (Exception e) {
            KubeJS.LOGGER.error("Failed to bind UI {} to item {}", uiId, itemId, e);
        }
    }

    /**
     * 将UI绑定到方块
     */
    public void bindUIToBlock(String blockId, String uiId) {
        try {
            ResourceLocation blockLocation = ResourceLocation.parse(blockId);
            ResourceLocation uiLocation = ResourceLocation.parse(uiId);
            UIRegistry.bindUIToBlock(blockLocation, uiLocation);
        } catch (Exception e) {
            KubeJS.LOGGER.error("Failed to bind UI {} to block {}", uiId, blockId, e);
        }
    }

    /**
     * 智能绑定 - 自动判断是物品还是方块
     */
    public void bindUI(String targetId, String uiId) {
        try {
            ResourceLocation targetLocation = ResourceLocation.parse(targetId);
            ResourceLocation uiLocation = ResourceLocation.parse(uiId);
            
            BindingType type = UIRegistry.getBindingType(targetLocation);
            
            switch (type) {
                case ITEM:
                    UIRegistry.bindUIToItem(targetLocation, uiLocation);
                    KubeJS.LOGGER.info("Auto-bound UI {} to item {}", uiId, targetId);
                    break;
                case BLOCK_ITEM:
                    // 对于方块物品，同时绑定物品和方块
                    UIRegistry.bindUIToItem(targetLocation, uiLocation);
                    // 获取对应的方块ID
                    Item item = ForgeRegistries.ITEMS.getValue(targetLocation);
                    if (item instanceof BlockItem) {
                        Block block = ((BlockItem) item).getBlock();
                        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);
                        if (blockId != null) {
                            UIRegistry.bindUIToBlock(blockId, uiLocation);
                            KubeJS.LOGGER.info("Auto-bound UI {} to block item {} (block: {})", uiId, targetId, blockId);
                        }
                    }
                    break;
                case BLOCK:
                    UIRegistry.bindUIToBlock(targetLocation, uiLocation);
                    KubeJS.LOGGER.info("Auto-bound UI {} to block {}", uiId, targetId);
                    break;
                default:
                    KubeJS.LOGGER.warn("Unknown target type for {} - binding as item", targetId);
                    UIRegistry.bindUIToItem(targetLocation, uiLocation);
                    break;
            }
        } catch (Exception e) {
            KubeJS.LOGGER.error("Failed to bind UI {} to target {}", uiId, targetId, e);
        }
    }
    
    /**
     * 检查UI是否已注册
     */
    public boolean hasUI(String uiId) {
        try {
            ResourceLocation location = ResourceLocation.parse(uiId);
            return UIRegistry.hasUI(location);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取绑定类型
     */
    public String getBindingType(String targetId) {
        try {
            ResourceLocation location = ResourceLocation.parse(targetId);
            BindingType type = UIRegistry.getBindingType(location);
            return type.name();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
    
    /**
     * 获取UI的JSON表示（用于调试）
     */
    public JsonElement getUIAsJson(String uiId) {
        try {
            ResourceLocation location = ResourceLocation.parse(uiId);
            JsonObject json = UIRegistry.getUIAsJson(location);
            return json != null ? json : new JsonObject();
        } catch (Exception e) {
            KubeJS.LOGGER.error("Failed to get UI as JSON: {}", uiId, e);
            return new JsonObject();
        }
    }
}
