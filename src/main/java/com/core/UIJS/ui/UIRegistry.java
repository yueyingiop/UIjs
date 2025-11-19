package com.core.UIJS.ui;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.core.UIJS.mcml.MCMLNode;
import com.core.UIJS.mcml.MCMLParser;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

// UI注册
public class UIRegistry {
    private static final Logger LOGGER = LogManager.getLogger("UIJS-UIRegistry");
    private static final Map<ResourceLocation, MCMLNode> REGISTERED_UIS = new HashMap<>(); // 存储UI节点
    public static final Map<ResourceLocation, ResourceLocation> ITEM_UI_BINDINGS = new HashMap<>(); // 物品与uiId的绑定关系
    public static final Map<ResourceLocation, ResourceLocation> BLOCK_UI_BINDINGS = new HashMap<>(); // 方块与uiId的绑定关系

    // 通过mcml注册UI
    public static boolean registerUI(ResourceLocation uiId, Path mcmlFile) {
        try {
            MCMLParser parser = new MCMLParser(); // 创建解析器
            MCMLNode uiNode = parser.parseFile(mcmlFile);// 解析MCML文件
            if (uiNode != null) {
                REGISTERED_UIS.put(uiId, uiNode);
                LOGGER.info("Registered UI: {} from file: {}", uiId, mcmlFile);
                return true;
            } else {
                LOGGER.error("Failed to parse MCML file: {}", mcmlFile);
                return false;
            }
        } catch (Exception e) {
            LOGGER.error("Error registering UI {} from file {}", uiId, mcmlFile, e);
            return false;
        }
    }
    
    // 通过string注册UI
    public static boolean registerUIFromString(ResourceLocation uiId, String mcmlContent) {
        try {
            MCMLParser parser = new MCMLParser(); // 创建解析器
            MCMLNode uiNode = parser.parse(mcmlContent);
            if (uiNode != null) {
                REGISTERED_UIS.put(uiId, uiNode);
                LOGGER.info("Registered UI: {} from string content", uiId);
                return true;
            }
            return false;
        } catch (Exception e) {
            LOGGER.error("Error registering UI {} from string", uiId, e);
            return false;
        }
    }
    
    // 绑定UI到物品
    public static void bindUIToItem(ResourceLocation itemId, ResourceLocation uiId) {
        ITEM_UI_BINDINGS.put(itemId, uiId);
        LOGGER.info("Bound UI {} to item {}", uiId, itemId);
    }

    // 绑定UI到方块
    public static void bindUIToBlock(ResourceLocation blockId, ResourceLocation uiId) {
        BLOCK_UI_BINDINGS.put(blockId, uiId);
        LOGGER.info("Bound UI {} to block {}", uiId, blockId);
    }
    
    // 获取UI节点
    public static MCMLNode getUI(ResourceLocation uiId) {
        return REGISTERED_UIS.get(uiId);
    }
    
    // 获取UI绑定的物品ID
    public static ResourceLocation getUIForItem(ResourceLocation itemId) {
        return ITEM_UI_BINDINGS.get(itemId);
    }

    // 获取UI绑定的方块ID
    public static ResourceLocation getUIForBlock(ResourceLocation blockId) {
        return BLOCK_UI_BINDINGS.get(blockId);
    }
    
    // 检查UI是否存在
    public static boolean hasUI(ResourceLocation uiId) {
        return REGISTERED_UIS.containsKey(uiId);
    }

    // 检查物品是否绑定了UI
    public static boolean isItemBound(ResourceLocation itemId) {
        return ITEM_UI_BINDINGS.containsKey(itemId);
    }
    
    // 检查方块是否绑定了UI
    public static boolean isBlockBound(ResourceLocation blockId) {
        return BLOCK_UI_BINDINGS.containsKey(blockId);
    }
    
    // 获取UI的Json对象
    public static JsonObject getUIAsJson(ResourceLocation uiId) {
        MCMLNode node = getUI(uiId);
        return node != null ? node.toJson() : null;
    }

    // 检查一个ResourceLocation对应的是物品还是方块
    public static BindingType getBindingType(ResourceLocation resourceId) {
        // 首先检查是否是注册的物品
        if (ForgeRegistries.ITEMS.containsKey(resourceId)) {
            Item item = ForgeRegistries.ITEMS.getValue(resourceId);
            if (item != null) {
                // 检查这个物品是否对应一个方块（BlockItem）
                if (item instanceof BlockItem) {
                    return BindingType.BLOCK_ITEM;
                } else {
                    return BindingType.ITEM;
                }
            }
        }
        
        // 检查是否是注册的方块
        if (ForgeRegistries.BLOCKS.containsKey(resourceId)) {
            return BindingType.BLOCK;
        }
        
        return BindingType.UNKNOWN;
    }

    // 检查一个UI是否绑定了方块
    public static boolean isBlockBoundUI(ResourceLocation uiId) {
        return BLOCK_UI_BINDINGS.containsValue(uiId);
    }
    
    // 清空注册表
    public static void clearRegistry() {
        REGISTERED_UIS.clear();
        ITEM_UI_BINDINGS.clear();
        BLOCK_UI_BINDINGS.clear();
    }

    public enum BindingType {
        ITEM,           // 普通物品
        BLOCK_ITEM,     // 方块物品（可以放置为方块）
        BLOCK,          // 方块
        UNKNOWN         // 未知类型
    }
}
