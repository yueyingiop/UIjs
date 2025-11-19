package com.core.UIJS.recipe;

import java.util.HashMap;
import java.util.Map;

import com.core.UIJS.storage.SlotDataManager;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

public class GlobalRecipeCompletionHandler implements RecipeManager.RecipeCompletionListener {
    private static final GlobalRecipeCompletionHandler INSTANCE = new GlobalRecipeCompletionHandler();
    public static GlobalRecipeCompletionHandler getInstance() {
        return INSTANCE;
    }

    private GlobalRecipeCompletionHandler() {
        // 注册为全局监听器
        RecipeManager.getInstance().addCompletionListener(this);
    }

    @Override
    public void onRecipeCompleted(String group, RecipeSystem recipe) {
        Minecraft.getInstance().execute(() -> {
            handleRecipeCompletion(group, recipe);
        });
    }

    private void handleRecipeCompletion(String group, RecipeSystem recipe) {
        
        // 处理输出物品 - 无论UI是否打开都执行
        handleRecipeOutputs(group, recipe);
        
        // 消耗输入物品
        consumeInputItems(group, recipe);
        
        // 检查是否可以开始新的配方
        checkNextRecipe(group);
    }
    
    /**
     * 处理配方输出物品
     */
    private void handleRecipeOutputs(String group, RecipeSystem recipe) {
        Map<Integer, String> outputs = recipe.getOutputs();
        RecipeManager recipeManager = RecipeManager.getInstance();
        
        for (Map.Entry<Integer, String> output : outputs.entrySet()) {
            int slotOrder = output.getKey();
            String itemId = output.getValue();
            int amount = recipe.getOutputAmount(slotOrder);
            
            // 直接通过 SlotDataManager 设置输出物品
            setOutputItemDirectly(group, slotOrder, itemId, amount, recipeManager);
        }
    }

    /**
     * 直接设置输出物品到存储
     */
    private void setOutputItemDirectly(String group, int slotOrder, String itemId, int amount, RecipeManager recipeManager) {
        String slotKey = "output_" + slotOrder + "_" + group;
        ItemStack outputItem = recipeManager.createItemStack(itemId);
        outputItem.setCount(amount);
        
        if (!outputItem.isEmpty()) {
            // 获取当前插槽物品
            ItemStack currentItem = SlotDataManager.getSlotData(slotKey);
            
            if (currentItem.isEmpty()) {
                // 如果输出槽为空，直接设置新物品
                SlotDataManager.saveSlotData(slotKey, outputItem);
            } else if (ItemStack.isSameItemSameTags(currentItem, outputItem)) {
                // 如果输出槽已有相同物品，尝试堆叠
                int newCount = currentItem.getCount() + outputItem.getCount();
                if (newCount <= currentItem.getMaxStackSize()) {
                    currentItem.setCount(newCount);
                    SlotDataManager.saveSlotData(slotKey, currentItem);
                } else {
                    // 如果超过最大堆叠数，保持最大堆叠数
                    currentItem.setCount(currentItem.getMaxStackSize());
                    SlotDataManager.saveSlotData(slotKey, currentItem);
                }
            }
            // 如果输出槽有不同物品，保持原样
        }
    }
    
    /**
     * 消耗输入物品
     */
    private void consumeInputItems(String group, RecipeSystem recipe) {
        for (Map.Entry<Integer, String> input : recipe.getInputs().entrySet()) {
            int slotOrder = input.getKey();
            int consumeAmount = recipe.getInputAmount(slotOrder);
            String slotKey = "input_" + slotOrder + "_" + group;
            
            ItemStack currentItem = SlotDataManager.getSlotData(slotKey);
            if (!currentItem.isEmpty()) {
                currentItem.shrink(consumeAmount);
                if (currentItem.getCount() <= 0) {
                    SlotDataManager.saveSlotData(slotKey, ItemStack.EMPTY);
                } else {
                    SlotDataManager.saveSlotData(slotKey, currentItem);
                }
            }
        }
    }
    
    /**
     * 检查是否可以开始下一个配方
     */
    private void checkNextRecipe(String group) {
        // 获取当前组的所有插槽物品
        Map<Integer, ItemStack> slotItems = getSlotItemsForGroup(group);
        
        // 检查是否有匹配的配方
        RecipeManager recipeManager = RecipeManager.getInstance();
        RecipeSystem matchingRecipe = recipeManager.findMatchingRecipe(group, slotItems);
        
        if (matchingRecipe != null) {
            // 自动开始新的后台处理
            recipeManager.startBackgroundProcessing(group, matchingRecipe);
        }
    }
    
    /**
     * 获取配方组的所有插槽物品
     */
    private Map<Integer, ItemStack> getSlotItemsForGroup(String group) {
        Map<Integer, ItemStack> slotItems = new HashMap<>();
        
        // 假设最多10个插槽（可以根据实际情况调整）
        for (int i = 0; i < 10; i++) {
            String inputKey = "input_" + i + "_" + group;
            String outputKey = "output_" + i + "_" + group;
            
            ItemStack inputItem = SlotDataManager.getSlotData(inputKey);
            ItemStack outputItem = SlotDataManager.getSlotData(outputKey);
            
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
