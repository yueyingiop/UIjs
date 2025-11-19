package com.core.UIJS.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.core.UIJS.UIJS;

public class RecipeParser {
    /**
     * 解析配方内容字符串，返回解析出的配方列表
     * 
     * @param recipeContent 配方内容字符串
     * @return 解析成功的配方列表
     */
    public static List<RecipeSystem> parseRecipes(String recipeContent) {
        List<RecipeSystem> recipes = new ArrayList<>();
        
        if (recipeContent == null || recipeContent.trim().isEmpty()) {
            UIJS.LOGGER.warn("Recipe content is empty or null");
            return recipes;
        }
        
        UIJS.LOGGER.info("=== RecipeParser: Starting parse ===");
        // UIJS.LOGGER.info("Recipe content: '{}'", recipeContent);
        
        // 按分号分割配方（每个配方以分号结尾）
        String[] recipeStrings = recipeContent.split(";");
        int totalRecipes = 0;
        int successfulRecipes = 0;
        
        for (String recipeStr : recipeStrings) {
            totalRecipes++;
            String trimmedRecipe = recipeStr.trim();
            if (trimmedRecipe.isEmpty()) {
                continue;
            }
            
            // 确保以recipeGroup开头
            if (trimmedRecipe.startsWith("recipeGroup")) {
                RecipeSystem recipe = parseSingleRecipe(trimmedRecipe + ";"); // 重新添加分号
                if (recipe != null) {
                    recipes.add(recipe);
                    successfulRecipes++;
                }
            } else {
                UIJS.LOGGER.warn("Skipping invalid recipe string (doesn't start with recipeGroup): '{}'", trimmedRecipe);
            }
        }
        
        UIJS.LOGGER.info("RecipeParser: Parsed {}/{} recipes successfully", successfulRecipes, totalRecipes);
        return recipes;
    }
    
    //#region 处理recipeGroup链式函数
    /**
     * 解析单个配方字符串
     * 
     * @param recipeString 单个配方字符串
     * @return 解析成功的RecipeSystem对象，解析失败返回null
     */
    private static RecipeSystem parseSingleRecipe(String recipeString) {
        try {
            
            // 提取配方组名
            Pattern groupPattern = Pattern.compile("recipeGroup\\(\\s*\"([^\"]+)\"\\s*\\)");
            Matcher groupMatcher = groupPattern.matcher(recipeString);
            
            if (!groupMatcher.find()) {
                UIJS.LOGGER.error("No recipeGroup found in: {}", recipeString);
                return null;
            }
            
            String group = groupMatcher.group(1);
            
            // 提取tick值
            Pattern tickPattern = Pattern.compile("\\.\\s*tick\\(\\s*(\\d+)\\s*\\)");
            Matcher tickMatcher = tickPattern.matcher(recipeString);
            
            if (!tickMatcher.find()) {
                UIJS.LOGGER.error("No tick value found in: {}", recipeString);
                return null;
            }
            
            int ticks = Integer.parseInt(tickMatcher.group(1));
            
            // 创建配方对象
            RecipeSystem recipe = new RecipeSystem(group, ticks);
            
            // 提取所有输入项
            parseInputs(recipeString, recipe);
            
            // 提取所有输出项
            parseOutputs(recipeString, recipe);
            
            // 验证配方
            if (recipe.getInputs().isEmpty()) {
                UIJS.LOGGER.warn("Recipe has no inputs: {}", recipeString);
            }
            if (recipe.getOutputs().isEmpty()) {
                UIJS.LOGGER.warn("Recipe has no outputs: {}", recipeString);
            }
            
            UIJS.LOGGER.info("Successfully parsed recipe: group={}, inputs={}, outputs={}, ticks={}", 
                group, recipe.getInputs().size(), recipe.getOutputs().size(), ticks);
            
            return recipe;
            
        } catch (Exception e) {
            UIJS.LOGGER.error("Error parsing recipe: {}", recipeString, e);
            return null;
        }
    }
    
    /**
     * 解析输入项
     */
    private static void parseInputs(String recipeString, RecipeSystem recipe) {
        // 解析.input(Item.of("物品ID", 数量), 插槽)
        Pattern inputPatternNew = Pattern.compile("\\.\\s*input\\(\\s*Item\\.of\\(\\s*\"([^\"]+)\"\\s*,\\s*(\\d+)\\s*\\)\\s*,\\s*(\\d+)\\s*\\)");
        Matcher inputMatcherNew = inputPatternNew.matcher(recipeString);

        while (inputMatcherNew.find()) {
            String inputItem = inputMatcherNew.group(1);
            int inputAmount = Integer.parseInt(inputMatcherNew.group(2));
            int inputSlot = Integer.parseInt(inputMatcherNew.group(3));
            
            // 添加指定数量的输入
            for (int i = 0; i < inputAmount; i++) {
                recipe.addInput(inputSlot, inputItem);
            }
        }

        // 解析.input("物品ID", 插槽)
        Pattern inputPattern = Pattern.compile("\\.\\s*input\\(\\s*\"([^\"]+)\"\\s*,\\s*(\\d+)\\s*\\)");
        Matcher inputMatcher = inputPattern.matcher(recipeString);
        
        // int inputCount = 0;
        while (inputMatcher.find()) {
            String inputItem = inputMatcher.group(1);
            int inputSlot = Integer.parseInt(inputMatcher.group(2));
            recipe.addInput(inputSlot, inputItem);
            // inputCount++;
            // UIJS.LOGGER.debug("  Input {}: {} -> slot {}", inputCount, inputItem, inputSlot);
        }
    }
    
    /**
     * 解析输出项
     */
    private static void parseOutputs(String recipeString, RecipeSystem recipe) {
        // 解析.input(Item.of("物品ID", 数量), 插槽)
        Pattern outputPatternNew = Pattern.compile("\\.\\s*output\\(\\s*Item\\.of\\(\\s*\"([^\"]+)\"\\s*,\\s*(\\d+)\\s*\\)\\s*,\\s*(\\d+)\\s*\\)");
        Matcher outputMatcherNew = outputPatternNew.matcher(recipeString);

        while (outputMatcherNew.find()) {
            String outputItem = outputMatcherNew.group(1);
            int outputAmount = Integer.parseInt(outputMatcherNew.group(2));
            int outputSlot = Integer.parseInt(outputMatcherNew.group(3));
            
            // 添加指定数量的输出
            for (int i = 0; i < outputAmount; i++) {
                recipe.addOutput(outputSlot, outputItem);
            }
        }


        // 解析 .output("物品ID", 插槽)
        Pattern outputPattern = Pattern.compile("\\.\\s*output\\(\\s*\"([^\"]+)\"\\s*,\\s*(\\d+)\\s*\\)");
        Matcher outputMatcher = outputPattern.matcher(recipeString);
        
        // int outputCount = 0;
        while (outputMatcher.find()) {
            String outputItem = outputMatcher.group(1);
            int outputSlot = Integer.parseInt(outputMatcher.group(2));
            recipe.addOutput(outputSlot, outputItem);
            // outputCount++;
            // UIJS.LOGGER.debug("  Output {}: {} -> slot {}", outputCount, outputItem, outputSlot);
        }
    }
    //#endregion
    
    /**
     * 验证配方内容格式（用于recipeGroup链式函数调试）
     */
    public static void validateRecipeContent(String recipeContent) {
        if (recipeContent == null || recipeContent.trim().isEmpty()) {
            UIJS.LOGGER.warn("Recipe content is empty");
            return;
        }
        
        UIJS.LOGGER.info("=== Recipe Content Validation ===");
        UIJS.LOGGER.info("Content: {}", recipeContent);
        
        // 检查基本结构
        boolean hasRecipeGroup = recipeContent.contains("recipeGroup");
        boolean hasTick = recipeContent.contains(".tick(");
        boolean hasInput = recipeContent.contains(".input(");
        boolean hasOutput = recipeContent.contains(".output(");
        
        UIJS.LOGGER.info("Has recipeGroup: {}", hasRecipeGroup);
        UIJS.LOGGER.info("Has tick: {}", hasTick);
        UIJS.LOGGER.info("Has input: {}", hasInput);
        UIJS.LOGGER.info("Has output: {}", hasOutput);
        
        if (!hasRecipeGroup) {
            UIJS.LOGGER.error("Missing recipeGroup in content");
        }
        if (!hasTick) {
            UIJS.LOGGER.error("Missing tick in content");
        }
        if (!hasInput) {
            UIJS.LOGGER.warn("No inputs found in content");
        }
        if (!hasOutput) {
            UIJS.LOGGER.warn("No outputs found in content");
        }
        
        // 尝试解析
        List<RecipeSystem> recipes = parseRecipes(recipeContent);
        UIJS.LOGGER.info("Validation: Found {} valid recipes", recipes.size());
        UIJS.LOGGER.info("=== End Validation ===");
    }
}
