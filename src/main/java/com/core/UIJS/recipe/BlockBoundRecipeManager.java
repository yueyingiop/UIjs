package com.core.UIJS.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.core.UIJS.UIJS;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class BlockBoundRecipeManager {
    private static final Map<String, BlockBoundRecipeManager> INSTANCES = new ConcurrentHashMap<>();
    private final String blockKey;
    private final Map<String, List<RecipeSystem>> recipesByGroup = new ConcurrentHashMap<>();
    private final Map<String, RecipeSystem> activeRecipes = new ConcurrentHashMap<>();
    private final Map<String, Integer> recipeProgress = new ConcurrentHashMap<>();
    private final Map<String, Boolean> backgroundProcessing = new ConcurrentHashMap<>();

    private BlockBoundRecipeManager(String blockKey) {
        this.blockKey = blockKey;
    }

    // 获取标识
    public static BlockBoundRecipeManager getInstance(BlockPos pos, ResourceLocation dimension) {
        String key = createBlockKey(pos, dimension);
        return INSTANCES.computeIfAbsent(key, BlockBoundRecipeManager::new);
    }

    public static BlockBoundRecipeManager getInstance(String blockKey) {
        return INSTANCES.computeIfAbsent(blockKey, BlockBoundRecipeManager::new);
    }

    // 删除表示
    public static void removeInstance(String blockKey) {
        INSTANCES.remove(blockKey);
    }
    
    // 创建标识
    public static String createBlockKey(BlockPos pos, ResourceLocation dimension) {
        return dimension + "_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ();
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
        backgroundProcessing.put(group, true);
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
            
            if (backgroundProcessing.getOrDefault(group, false) && recipe.tick()) {
                onRecipeCompleted(group, recipe);
            }
        }
    }

    // 配方完成
    private void onRecipeCompleted(String groupKey, RecipeSystem recipe) {
        // 处理配方完成 - 使用特定方块的处理器
        BlockBoundRecipeCompletionHandler.getInstance().onRecipeCompleted(blockKey, groupKey, recipe);
        stopRecipe(groupKey);
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
                return false;
            }
            
            if (!isItemMatch(slotItem, requiredItem)) {
                return false;
            }
            
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
                if (!ItemStack.isSameItemSameTags(slotItem, outputItem)) {
                    return false;
                }
                if (slotItem.getCount() + outputAmount > slotItem.getMaxStackSize()) {
                    return false;
                }
            }
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

    // 后台处理方法
    public void startBackgroundProcessing(String group, RecipeSystem recipe) {
        activeRecipes.put(group, recipe);
        recipeProgress.put(group, 0);
        backgroundProcessing.put(group, true);
        recipe.startProcessing();
    }

    public void checkAndStartBackgroundRecipes(String group, Map<Integer, ItemStack> slotItems) {
        if (activeRecipes.containsKey(group)) {
            return;
        }
        
        RecipeSystem matchingRecipe = findMatchingRecipe(group, slotItems);
        if (matchingRecipe != null) {
            startBackgroundProcessing(group, matchingRecipe);
        }
    }

    public List<String> getBackgroundProcessingGroups() {
        List<String> groups = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : backgroundProcessing.entrySet()) {
            if (entry.getValue()) {
                groups.add(entry.getKey());
            }
        }
        return groups;
    }

    public void updateAllBackgroundRecipes() {
        for (String group : getBackgroundProcessingGroups()) {
            RecipeSystem recipe = activeRecipes.get(group);
            if (recipe != null && backgroundProcessing.getOrDefault(group, false)) {
                if (recipe.tick()) {
                    onRecipeCompleted(group, recipe);
                }
            }
        }
    }

    public boolean isBackgroundProcessing(String group) {
        return backgroundProcessing.getOrDefault(group, false);
    }
    
    public void stopBackgroundProcessing(String group) {
        backgroundProcessing.put(group, false);
    }

    // 获取方块键
    public String getBlockKey() {
        return blockKey;
    }
}
