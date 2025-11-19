package com.core.UIJS.recipe;

import java.util.HashMap;
import java.util.Map;

import com.core.UIJS.UIJS;
import com.core.UIJS.storage.SlotDataManager;

import net.minecraft.world.item.ItemStack;

// 方块绑定的配方完成处理器
public class BlockBoundRecipeCompletionHandler {
    private static final BlockBoundRecipeCompletionHandler INSTANCE = new BlockBoundRecipeCompletionHandler();
    
    public static BlockBoundRecipeCompletionHandler getInstance() {
        return INSTANCE;
    }

    private BlockBoundRecipeCompletionHandler() {}

    public void onRecipeCompleted(String blockKey, String group, RecipeSystem recipe) {
        handleRecipeCompletion(blockKey, group, recipe);
    }

    private void handleRecipeCompletion(String blockKey, String group, RecipeSystem recipe) {
        // 处理输出物品
        handleRecipeOutputs(blockKey, group, recipe);
        
        // 消耗输入物品
        consumeInputItems(blockKey, group, recipe);
        
        // 检查是否可以开始新的配方
        checkNextRecipe(blockKey, group);
    }
    
    /**
     * 处理配方输出物品
     */
    private void handleRecipeOutputs(String blockKey, String group, RecipeSystem recipe) {
        Map<Integer, String> outputs = recipe.getOutputs();
        BlockBoundRecipeManager recipeManager = BlockBoundRecipeManager.getInstance(blockKey);
        
        for (Map.Entry<Integer, String> output : outputs.entrySet()) {
            int slotOrder = output.getKey();
            String itemId = output.getValue();
            int amount = recipe.getOutputAmount(slotOrder);
            
            setOutputItemDirectly(blockKey, group, slotOrder, itemId, amount, recipeManager);
        }
    }

    /**
     * 直接设置输出物品到存储
     */
    private void setOutputItemDirectly(String blockKey, String group, int slotOrder, String itemId, int amount, BlockBoundRecipeManager recipeManager) {
        ItemStack outputItem = recipeManager.createItemStack(itemId);
        outputItem.setCount(amount);
        
        if (!outputItem.isEmpty()) {
            // 使用方块特定的数据加载方法
            ItemStack currentItem = SlotDataManager.getBlockSlotData(blockKey, "output", slotOrder, group);
            
            if (currentItem.isEmpty()) {
                // 如果输出槽为空，直接设置新物品
                SlotDataManager.saveBlockSlotData(blockKey, "output", slotOrder, group, outputItem);
                UIJS.LOGGER.debug("Set output item for block {}: {}", blockKey, outputItem);
            } else if (ItemStack.isSameItemSameTags(currentItem, outputItem)) {
                // 如果输出槽已有相同物品，尝试堆叠
                int newCount = currentItem.getCount() + outputItem.getCount();
                if (newCount <= currentItem.getMaxStackSize()) {
                    currentItem.setCount(newCount);
                    SlotDataManager.saveBlockSlotData(blockKey, "output", slotOrder, group, currentItem);
                    UIJS.LOGGER.debug("Stacked output item for block {}: {}", blockKey, currentItem);
                } else {
                    // 如果超过最大堆叠数，保持最大堆叠数
                    currentItem.setCount(currentItem.getMaxStackSize());
                    SlotDataManager.saveBlockSlotData(blockKey, "output", slotOrder, group, currentItem);
                    UIJS.LOGGER.debug("Stacked output item (max) for block {}: {}", blockKey, currentItem);
                }
            }
            // 如果输出槽有不同物品，保持原样
        }
    }
    
    /**
     * 消耗输入物品
     */
    private void consumeInputItems(String blockKey, String group, RecipeSystem recipe) {
        for (Map.Entry<Integer, String> input : recipe.getInputs().entrySet()) {
            int slotOrder = input.getKey();
            int consumeAmount = recipe.getInputAmount(slotOrder);
            
            ItemStack currentItem = SlotDataManager.getBlockSlotData(blockKey, "input", slotOrder, group);
            if (!currentItem.isEmpty()) {
                currentItem.shrink(consumeAmount);
                if (currentItem.getCount() <= 0) {
                    SlotDataManager.saveBlockSlotData(blockKey, "input", slotOrder, group, ItemStack.EMPTY);
                } else {
                    SlotDataManager.saveBlockSlotData(blockKey, "input", slotOrder, group, currentItem);
                }
            }
        }
    }
    
    /**
     * 检查是否可以开始下一个配方
     */
    private void checkNextRecipe(String blockKey, String group) {
        Map<Integer, ItemStack> slotItems = getSlotItemsForBlockGroup(blockKey, group);
        BlockBoundRecipeManager recipeManager = BlockBoundRecipeManager.getInstance(blockKey);
        RecipeSystem matchingRecipe = recipeManager.findMatchingRecipe(group, slotItems);
        
        if (matchingRecipe != null) {
            recipeManager.startBackgroundProcessing(group, matchingRecipe);
        }
    }
    
    /**
     * 获取方块配方组的所有插槽物品
     */
    private Map<Integer, ItemStack> getSlotItemsForBlockGroup(String blockKey, String group) {
        Map<Integer, ItemStack> slotItems = new HashMap<>();
        
        for (int i = 0; i < 10; i++) {
            ItemStack inputItem = SlotDataManager.getBlockSlotData(blockKey, "input", i, group);
            ItemStack outputItem = SlotDataManager.getBlockSlotData(blockKey, "output", i, group);
            
            if (!inputItem.isEmpty()) {
                slotItems.put(i, inputItem);
            }
            if (!outputItem.isEmpty()) {
                slotItems.put(i, outputItem);
            }
        }
        
        return slotItems;
    }
    
}
