package com.core.UIJS.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ItemInteractionHelper {
    
    /**
     * 处理左键点击物品交互
     */
    public static InteractionResult handleLeftClick(ItemStack carriedItem, ItemStack slotItem, Player player) {
        InteractionResult result = new InteractionResult(carriedItem, slotItem);
        
        if (shouldPlaceItem(carriedItem, slotItem)) {
            handlePlaceAllItems(result, carriedItem);
        } else if (shouldTakeItem(carriedItem, slotItem)) {
            handleTakeAllItems(result, slotItem);
        } else if (shouldSwapOrMergeItems(carriedItem, slotItem)) {
            handleSwapOrMergeItems(result, carriedItem, slotItem);
        }
        
        return result;
    }
    
    /**
     * 处理右键点击物品交互
     */
    public static InteractionResult handleRightClick(ItemStack carriedItem, ItemStack slotItem, Player player) {
        InteractionResult result = new InteractionResult(carriedItem, slotItem);
        
        if (shouldPlaceItem(carriedItem, slotItem)) {
            handlePlaceSingleItem(result, carriedItem);
        } else if (shouldTakeItem(carriedItem, slotItem)) {
            handleTakeHalfItems(result, slotItem);
        } else if (shouldAddOneItem(carriedItem, slotItem)) {
            handleAddOneItem(result, carriedItem, slotItem);
        } else if (shouldSwapOrMergeItems(carriedItem, slotItem)) {
            handleSwapItems(result, carriedItem, slotItem);
            result.setChanged(true);
        }
        
        return result;
    }
    
    //#region 左键点击辅助方法
    /**
     * 处理放置所有物品逻辑
     */
    private static void handlePlaceAllItems(InteractionResult result, ItemStack carriedItem) {
        result.setSlotItem(carriedItem.copy());
        result.setCarriedItem(ItemStack.EMPTY);
        result.setChanged(true);
    }

    /**
     * 处理取出所有物品逻辑
     */
    private static void handleTakeAllItems(InteractionResult result, ItemStack slotItem) {
        result.setCarriedItem(slotItem.copy());
        result.setSlotItem(ItemStack.EMPTY);
        result.setChanged(true);
    }

    /**
     * 处理交换或合并物品逻辑
     */
    private static void handleSwapOrMergeItems(InteractionResult result, ItemStack carriedItem, ItemStack slotItem) {
        if (canMergeItems(carriedItem, slotItem)) {
            handleMergeItems(result, carriedItem, slotItem);
        } else {
            handleSwapItems(result, carriedItem, slotItem);
        }
        result.setChanged(true);
    }

    /**
     * 处理合并物品逻辑
     */
    private static void handleMergeItems(InteractionResult result, ItemStack carriedItem, ItemStack slotItem) {
        int maxStackSize = slotItem.getMaxStackSize();
        int currentSlotCount = slotItem.getCount();
        int currentCarriedCount = carriedItem.getCount();
        int totalCount = currentSlotCount + currentCarriedCount;
        
        if (totalCount <= maxStackSize) {
            // 可以完全合并
            ItemStack newSlot = slotItem.copy();
            newSlot.setCount(totalCount);
            result.setSlotItem(newSlot);
            result.setCarriedItem(ItemStack.EMPTY);
        } else {
            // 部分合并
            int canAdd = maxStackSize - currentSlotCount;
            ItemStack newSlot = slotItem.copy();
            newSlot.setCount(maxStackSize);
            
            ItemStack newCarried = carriedItem.copy();
            newCarried.setCount(currentCarriedCount - canAdd);
            
            result.setSlotItem(newSlot);
            result.setCarriedItem(newCarried);
        }

    }
    
    /**
     * 处理交换物品逻辑
     */
    private static void handleSwapItems(InteractionResult result, ItemStack carriedItem, ItemStack slotItem) {
        ItemStack newCarried = slotItem.copy();
        ItemStack newSlot = carriedItem.copy();
        
        result.setCarriedItem(newCarried);
        result.setSlotItem(newSlot);
    }
    
    //#endregion

    //#region 右键点击辅助方法
    /**
     * 处理放置单个物品逻辑
     */
    private static void handlePlaceSingleItem(InteractionResult result, ItemStack carriedItem) {
        ItemStack singleItem = carriedItem.copy();
        singleItem.setCount(1);
        result.setSlotItem(singleItem);
        
        ItemStack newCarried = carriedItem.copy();
        newCarried.shrink(1);
        if (newCarried.isEmpty()) {
            result.setCarriedItem(ItemStack.EMPTY);
        } else {
            result.setCarriedItem(newCarried);
        }
        result.setChanged(true);
    }

    /**
     * 处理取出一半物品逻辑
     */
    private static void handleTakeHalfItems(InteractionResult result, ItemStack slotItem) {
        int halfCount = (int) Math.ceil(slotItem.getCount() / 2.0);
        ItemStack halfStack = slotItem.copy();
        halfStack.setCount(halfCount);
        result.setCarriedItem(halfStack);
        
        ItemStack newSlot = slotItem.copy();
        newSlot.shrink(halfCount);
        if (newSlot.isEmpty()) {
            result.setSlotItem(ItemStack.EMPTY);
        } else {
            result.setSlotItem(newSlot);
        }
        result.setChanged(true);
    }

    /**
     * 处理添加一个物品逻辑
     */
    private static void handleAddOneItem(InteractionResult result, ItemStack carriedItem, ItemStack slotItem) {
        if (slotItem.getCount() < slotItem.getMaxStackSize() && carriedItem.getCount() > 0) {
            ItemStack newSlot = slotItem.copy();
            newSlot.grow(1);
            
            ItemStack newCarried = carriedItem.copy();
            newCarried.shrink(1);
            
            if (newCarried.isEmpty()) {
                result.setCarriedItem(ItemStack.EMPTY);
            } else {
                result.setCarriedItem(newCarried);
            }
            result.setSlotItem(newSlot);
            result.setChanged(true);
        }
    }
    
    //#endregion

    //#region 通用条件检查方法
    
    /**
     * 检查是否应该放置物品
     */
    private static boolean shouldPlaceItem(ItemStack carriedItem, ItemStack slotItem) {
        return !carriedItem.isEmpty() && slotItem.isEmpty();
    }
    
    /**
     * 检查是否应该取出物品
     */
    private static boolean shouldTakeItem(ItemStack carriedItem, ItemStack slotItem) {
        return carriedItem.isEmpty() && !slotItem.isEmpty();
    }
    
    /**
     * 检查是否应该交换或合并物品
     */
    private static boolean shouldSwapOrMergeItems(ItemStack carriedItem, ItemStack slotItem) {
        return !carriedItem.isEmpty() && !slotItem.isEmpty();
    }
    
    /**
     * 检查物品是否可以合并
     */
    private static boolean canMergeItems(ItemStack carriedItem, ItemStack slotItem) {
        return ItemStack.isSameItemSameTags(carriedItem, slotItem);
    }
    
    /**
     * 检查是否应该添加一个物品
     */
    private static boolean shouldAddOneItem(ItemStack carriedItem, ItemStack slotItem) {
        return !carriedItem.isEmpty() && 
               !slotItem.isEmpty() && 
               ItemStack.isSameItemSameTags(carriedItem, slotItem) &&
               slotItem.getCount() < slotItem.getMaxStackSize();
    }
    
    //#endregion


    /**
     * 检查鼠标是否在插槽上
     */
    public static boolean isMouseOverSlot(
        double mouseX, double mouseY, 
        int slotX, int slotY, 
        int slotWidth, int slotHeight
    ) {
        return mouseX >= slotX && mouseX <= slotX + slotWidth &&
               mouseY >= slotY && mouseY <= slotY + slotHeight;
    }

    public static boolean isMouseOverSlot(
        double mouseX, double mouseY, 
        int slotX, int slotY, 
        int soltSize
    ) {
        return isMouseOverSlot(mouseX, mouseY, slotX, slotY, soltSize, soltSize);
    }

    /**
     * 交互结果类
     */
    public static class InteractionResult {
        private ItemStack carriedItem;
        private ItemStack slotItem;
        private boolean changed;
        
        public InteractionResult(ItemStack carriedItem, ItemStack slotItem) {
            this.carriedItem = carriedItem.copy();
            this.slotItem = slotItem.copy();
            this.changed = false;

        }
        
        //#region Getters and Setters
        public ItemStack getCarriedItem() { return carriedItem; }
        public void setCarriedItem(ItemStack carriedItem) { 
            this.carriedItem = carriedItem != null ? carriedItem.copy() : ItemStack.EMPTY; 
        }
        
        public ItemStack getSlotItem() { return slotItem; }
        public void setSlotItem(ItemStack slotItem) { 
            this.slotItem = slotItem != null ? slotItem.copy() : ItemStack.EMPTY; 
        }
        
        public boolean isChanged() { return changed; }
        public void setChanged(boolean changed) { this.changed = changed; }
        //#endregion
    }
}
