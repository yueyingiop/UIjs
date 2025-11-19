package com.core.UIJS.recipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.core.UIJS.UIJS;
import com.core.UIJS.ui.SoltWidget;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class RecipeManager {
    private static final RecipeManager INSTANCE = new RecipeManager();
    private final Map<String, List<RecipeSystem>> recipesByGroup = new ConcurrentHashMap<>();
    private final Map<String, RecipeSystem> activeRecipes = new ConcurrentHashMap<>();
    private final Map<String, Integer> recipeProgress = new ConcurrentHashMap<>();
    private final Map<String, Boolean> backgroundProcessing = new ConcurrentHashMap<>(); // 后台处理

    public static RecipeManager getInstance() {
        return INSTANCE;
    }

    // 注册配方
    public void registerRecipe(RecipeSystem recipe) {
        recipesByGroup.computeIfAbsent(
            recipe.getGroup(), 
            k -> new ArrayList<>()
        ).add(recipe);
    }

    // 注销配方
    public void unregisterRecipe(RecipeSystem recipe) {
        List<RecipeSystem> recipes = recipesByGroup.get(recipe.getGroup());
        if (recipes != null) {
            recipes.remove(recipe);
            if (recipes.isEmpty()) {
                recipesByGroup.remove(recipe.getGroup());
            }
        }
    }

    // 根据组获取配方
    public List<RecipeSystem> getRecipesByGroup(String group) {
        return recipesByGroup.getOrDefault(group, new ArrayList<>());
    }

    // 启动配方
    public void startRecipe(String group, RecipeSystem recipe) {
        activeRecipes.put(group, recipe);
        recipeProgress.put(group, 0);
        backgroundProcessing.put(group, true); // 启用后台处理
        recipe.startProcessing();
    }

    // 停止配方
    public void stopRecipe(String group) {
        RecipeSystem recipe = activeRecipes.remove(group);
        if (recipe != null) {
            recipe.stopProcessing();
        }
        recipeProgress.remove(group);
        backgroundProcessing.remove(group);
    }

    // 获取正在处理的配方
    public RecipeSystem getActiveRecipe(String group) {
        return activeRecipes.get(group);
    }

    // 更新配方进度
    public void updateRecipeProgress() {
        for (Map.Entry<String, RecipeSystem> entry : activeRecipes.entrySet()) {
            String group = entry.getKey();
            RecipeSystem recipe = entry.getValue();
            
            // 只有在启用后台处理时才更新进度
            if (backgroundProcessing.getOrDefault(group, false) && recipe.tick()) {
                // 配方完成
                onRecipeCompleted(group, recipe);
            }
        }
    }

    // 配方完成
    private void onRecipeCompleted(String groupKey, RecipeSystem recipe) {
        for (RecipeCompletionListener listener : completionListeners) {
            listener.onRecipeCompleted(groupKey, recipe);
        }
        stopRecipe(groupKey);

        // 通知配方完成,检索下一个配方
        notifyRecipeCompletion(groupKey);
    }

    //通知配方完成，用于触发重新检查
    private void notifyRecipeCompletion(String group) {
        for (RecipeCompletionListener listener : completionListeners) {
            if (listener instanceof RecipeCompletionHandler) {
                ((RecipeCompletionHandler) listener).onRecipeFinished(group);
            }
        }
    }

    // 检查是否可以启动配方
    public boolean canStartRecipe(String group, Map<Integer, ItemStack> slotItems) {
        List<RecipeSystem> recipes = getRecipesByGroup(group);
        for (RecipeSystem recipe : recipes) {
            if (matchesRecipe(recipe, slotItems)) {
                return true;
            }
        }
        return false;
    }
    
    // 寻找匹配的配方
    public RecipeSystem findMatchingRecipe(String group, Map<Integer, ItemStack> slotItems) {
        List<RecipeSystem> recipes = getRecipesByGroup(group);
        for (RecipeSystem recipe : recipes) {
            if (matchesRecipe(recipe, slotItems)) {
                return recipe;
            }
        }
        return null;
    }
    
    // 检查配方是否匹配
    private boolean matchesRecipe(RecipeSystem recipe, Map<Integer, ItemStack> slotItems) {
        // 检查所有输入条件
        for (Map.Entry<Integer, String> input : recipe.getInputs().entrySet()) {
            ItemStack slotItem = slotItems.get(input.getKey());
            String requiredItem = input.getValue();
            int requiredAmount = recipe.getInputAmount(input.getKey());
            
            if (slotItem == null || slotItem.isEmpty()) {
                return false; // 插槽为空
            }
            
            // 检查物品是否匹配
            if (!isItemMatch(slotItem, requiredItem)) {
                return false; // 物品不匹配
            }
            
            // 检查物品数量是否足够
            if (slotItem.getCount() < requiredAmount) {
                return false;
            }
        }
        
        // 检查输出插槽条件
        for (Map.Entry<Integer, String> output : recipe.getOutputs().entrySet()) {
            ItemStack slotItem = slotItems.get(output.getKey());
            ItemStack outputItem = createItemStack(output.getValue());
            int outputAmount = recipe.getOutputAmount(output.getKey());
            
            if (slotItem != null && !slotItem.isEmpty()) {
                // 输出插槽不为空，检查是否可以堆叠
                if (!ItemStack.isSameItemSameTags(slotItem, outputItem)) {
                    return false; // 输出插槽有不同物品
                }
                if (slotItem.getCount() + outputAmount > slotItem.getMaxStackSize()) {
                    return false; // 输出插槽空间不足
                }
            }
            // 如果输出插槽为空，总是允许
        }
        
        return true;
    }
    
    // 检查物品是否匹配
    private boolean isItemMatch(ItemStack itemStack, String itemId) {
        try {
            ResourceLocation itemLocation = ResourceLocation.tryParse(itemId);
            if (itemLocation != null) {
                return ForgeRegistries.ITEMS.getValue(itemLocation) == itemStack.getItem();
            }
        } catch (Exception e) {
            UIJS.LOGGER.error("Error parsing item ID: {}", itemId, e);
        }
        return false;
    }
    
    // 创建物品堆栈
    public ItemStack createItemStack(String itemId) {
        try {
            ResourceLocation itemLocation = ResourceLocation.tryParse(itemId);
            if (itemLocation != null && ForgeRegistries.ITEMS.containsKey(itemLocation)) {
                return new ItemStack(ForgeRegistries.ITEMS.getValue(itemLocation));
            }
        } catch (Exception e) {
            // 如果解析失败，返回空堆栈
        }
        return ItemStack.EMPTY;
    }

    //#region 后台处理

    // 启动后台配方处理
    public void startBackgroundProcessing(String group, RecipeSystem recipe) {
        activeRecipes.put(group, recipe);
        recipeProgress.put(group, 0);
        backgroundProcessing.put(group, true);
        recipe.startProcessing();
    }

    // 检查并启动符合条件的后台配方
    public void checkAndStartBackgroundRecipes(String group, Map<Integer, ItemStack> slotItems) {
        // 如果该组已有活跃配方，不重复启动
        if (activeRecipes.containsKey(group)) {
            return;
        }
        
        RecipeSystem matchingRecipe = findMatchingRecipe(group, slotItems);
        if (matchingRecipe != null) {
            startBackgroundProcessing(group, matchingRecipe);
        }
    }

    // 获取所有正在后台处理的配方组
    public List<String> getBackgroundProcessingGroups() {
        List<String> groups = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : backgroundProcessing.entrySet()) {
            if (entry.getValue()) {
                groups.add(entry.getKey());
            }
        }
        return groups;
    }

    // 强制更新所有后台处理的配方进度
    public void updateAllBackgroundRecipes() {
        for (String group : getBackgroundProcessingGroups()) {
            RecipeSystem recipe = activeRecipes.get(group);
            if (recipe != null && backgroundProcessing.getOrDefault(group, false)) {
                if (recipe.tick()) {
                    // 配方完成
                    onRecipeCompleted(group, recipe);
                }
            }
        }
    }

    // 检查是否需要后台处理
    public boolean isBackgroundProcessing(String group) {
        return backgroundProcessing.getOrDefault(group, false);
    }
    
    // 允许手动停止后台处理
    public void stopBackgroundProcessing(String group) {
        backgroundProcessing.put(group, false);
    }

    //#endregion

    // 获取所有插槽物品的方法
    public Map<Integer, ItemStack> getSlotItemsForGroup(String group, Map<Integer, SoltWidget> slotWidgets) {
        Map<Integer, ItemStack> slotItems = new HashMap<>();
        if (slotWidgets != null) {
            for (Map.Entry<Integer, SoltWidget> entry : slotWidgets.entrySet()) {
                slotItems.put(entry.getKey(), entry.getValue().getItemStack());
            }
        }
        return slotItems;
    }


    // 配方完成监听器
    public interface RecipeCompletionListener {
        void onRecipeCompleted(String group, RecipeSystem recipe);
    }

    private final List<RecipeCompletionListener> completionListeners = new ArrayList<>();
    
    public void addCompletionListener(RecipeCompletionListener listener) {
        completionListeners.add(listener);
    }
    
    public void removeCompletionListener(RecipeCompletionListener listener) {
        completionListeners.remove(listener);
    }

    // 用于处理配方完成
    public interface RecipeCompletionHandler {
        void onRecipeFinished(String group);
    }
}
