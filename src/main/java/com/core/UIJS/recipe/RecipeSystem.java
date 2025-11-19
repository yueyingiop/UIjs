package com.core.UIJS.recipe;

import java.util.HashMap;
import java.util.Map;

public class RecipeSystem {
    private final String group;
    private final Map<Integer, String> inputs;
    private final Map<Integer, String> outputs;
    private final int processingTicks;
    private int currentTick;
    private boolean isProcessing;

    // 存储物品数量的映射
    private final Map<Integer, Integer> inputAmounts;
    private final Map<Integer, Integer> outputAmounts;

    public RecipeSystem(String group, int processingTicks) {
        this.group = group;
        this.inputs = new HashMap<>();
        this.outputs = new HashMap<>();
        this.inputAmounts = new HashMap<>();
        this.outputAmounts = new HashMap<>();
        this.processingTicks = processingTicks;
        this.currentTick = 0;
        this.isProcessing = false;
    }

    public void addInput(int slotOrder, String itemId) {
        inputs.put(slotOrder, itemId);
        inputAmounts.put(slotOrder, inputAmounts.getOrDefault(slotOrder, 0) + 1);
    }
    
    public void addOutput(int slotOrder, String itemId) {
        outputs.put(slotOrder, itemId);
        outputAmounts.put(slotOrder, outputAmounts.getOrDefault(slotOrder, 0) + 1);
    }

    //#region Getter
    public String getGroup() { return group; }
    public Map<Integer, String> getInputs() { return inputs; }
    public Map<Integer, String> getOutputs() { return outputs; }
    public int getProcessingTicks() { return processingTicks; }
    public int getCurrentTick() { return currentTick; }
    public boolean isProcessing() { return isProcessing; }
    public float getProgress() { 
        return processingTicks > 0 ? (float) currentTick / processingTicks : 0f; 
    }
    public int getInputAmount(int slot) {
        return inputAmounts.getOrDefault(slot, 1);
    }
    public int getOutputAmount(int slot) {
        return outputAmounts.getOrDefault(slot, 1);
    }
    //#endregion

    //#region 处理方法
    public void startProcessing() {
        this.isProcessing = true;
        this.currentTick = 0;
    }
    
    public void stopProcessing() {
        this.isProcessing = false;
        this.currentTick = 0;
    }
    
    public boolean tick() {
        if (isProcessing && currentTick < processingTicks) {
            currentTick++;
            return currentTick >= processingTicks;
        }
        return false;
    }
    //#endregion
}
