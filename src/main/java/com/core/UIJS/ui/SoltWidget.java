package com.core.UIJS.ui;

import java.util.HashMap;
import java.util.Map;

import com.core.UIJS.UIJS;
import com.core.UIJS.mcml.MCMLNode;
import com.core.UIJS.network.NetworkHandler;
import com.core.UIJS.network.SlotSyncPacket;
import com.core.UIJS.recipe.BlockBoundRecipeManager;
import com.core.UIJS.recipe.RecipeManager;
import com.core.UIJS.recipe.RecipeSystem;
import com.core.UIJS.storage.BlockBoundSlotDataManager;
import com.core.UIJS.storage.SlotDataManager;
import com.core.UIJS.util.ItemInteractionHelper;
import com.core.UIJS.util.UIRenderHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class SoltWidget extends AbstractWidget {
    private final int order; // 插槽顺序
    private final String bindGroup; // 绑定组
    private final Minecraft minecraft;
    private final MCMLNode node;
    private ItemStack itemStack = ItemStack.EMPTY; // 当前物品堆栈

    private final String blockKey; // 绑定的方块键

    public SoltWidget(int x, int y, int width, int height, int order, String bindGroup, MCMLNode node) {
        this(x, y, width, height, order, bindGroup, node, null);
    }

    public SoltWidget(int x, int y, int width, int height, int order, String bindGroup, MCMLNode node, String blockKey) {
        super(x, y, width, height, Component.literal("Solt"));
        this.order = order;
        this.bindGroup = bindGroup;
        this.node = node;
        this.blockKey = blockKey;
        this.minecraft = Minecraft.getInstance();

        loadItemFromStorage();
    }

    // 渲染组件
    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        String backgroundColorStr = node.getAttributeOrStyle("backgroundColor");
        String borderColorStr = node.getAttributeOrStyle("borderColor");
        String hoverCoverColorStr = node.getAttributeOrStyle("hoverCoverColor");
        String hoverBorderColorStr = node.getAttributeOrStyle("hoverBorderColor");
        int backgroundColor = UIRenderHelper.parseColor(backgroundColorStr, 0x80FFFFFF);
        int borderColor = UIRenderHelper.parseColor(borderColorStr, 0xFF00FF00);
        int hoverCoverColor = UIRenderHelper.parseColor(hoverCoverColorStr, 0x80FFFFFF);
        int hoverBorderColor = UIRenderHelper.parseColor(hoverBorderColorStr, 0xFF08FF18);
        
        String backgroungImage = node.getAttributeOrStyle("backgroundImage");
        if (backgroungImage != null && !backgroungImage.isEmpty()) {
            if (UIRenderHelper.isHttpUrl(backgroungImage)) {
                UIRenderHelper.renderNetworkBackground(
                    guiGraphics, node, backgroungImage, getX(), getY(), width, height, 
                    () -> {renderSolidBackground(guiGraphics, backgroundColor, borderColor);}
                );
            } else {
                UIRenderHelper.renderLocalBackground(
                    guiGraphics, backgroungImage, getX(), getY(), width, height, 
                    () -> {renderSolidBackground(guiGraphics, backgroundColor, borderColor);}
                );
            }
        } else {
            renderSolidBackground(guiGraphics, backgroundColor, borderColor);
        }

        
        // 渲染物品
        UIRenderHelper.renderItemInSlot(guiGraphics, itemStack, getX(), getY(), width, height);
        

        // 渲染顺序编号
        if (order >= 0) {
            int textX = getX() + width - 8;  // 距离右边8px
            int textY = getY() + height - 8; // 距离底部8px
            
            // 确保文本不会超出插槽
            textX = Math.min(textX, getX() + width - 4);
            textY = Math.min(textY, getY() + height - 4);
            
            guiGraphics.drawString(
                minecraft.font,
                String.valueOf(order),
                textX, textY,
                0xFFFFFF,
                false // 不渲染阴影，避免在深色背景上看不清
            );
        }

        // 渲染hover
        String hoverImage = node.getAttributeOrStyle("hoverImage");
        if (hoverImage != null && !hoverImage.isEmpty() && ItemInteractionHelper.isMouseOverSlot(mouseX, mouseY, getX(), getY(), width, height)) {
            if (UIRenderHelper.isHttpUrl(hoverImage)) {
                UIRenderHelper.renderNetworkBackground(
                    guiGraphics, node, hoverImage, getX(), getY(), width, height, 
                    () -> {UIRenderHelper.renderHover(guiGraphics, getX(), getY(), width, height, mouseX, mouseY, hoverCoverColor, hoverBorderColor);}
                );
            } else {
                UIRenderHelper.renderLocalBackground(
                    guiGraphics, hoverImage, getX(), getY(), width, height, 
                    () -> {UIRenderHelper.renderHover(guiGraphics, getX(), getY(), width, height, mouseX, mouseY, hoverCoverColor, hoverBorderColor);}
                );
            }
        } else {
            UIRenderHelper.renderHover(guiGraphics, getX(), getY(), width, height, mouseX, mouseY, hoverCoverColor, hoverBorderColor);
        }
        

        // 渲染配方进度
        renderRecipeProgress(guiGraphics, mouseX, mouseY);
    }

    // 渲染纯色背景
    private void renderSolidBackground(GuiGraphics guiGraphics, int backgroundColor, int borderColor){
        // 渲染插槽背景
        guiGraphics.fill(
            getX(), getY(),
            getX() + width, getY() + height,
            backgroundColor // 半透明白色背景
        );
        
        // 渲染边框
        guiGraphics.renderOutline(
            getX(), getY(),
            width, height,
            borderColor
        );
    }

    // 渲染配方进度
    private void renderRecipeProgress(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!isRecipeSlot()) return;
        
        RecipeManager recipeManager = RecipeManager.getInstance();
        RecipeSystem activeRecipe = recipeManager.getActiveRecipe(getBindGroup());
        
        // 检查配方是否活跃（包括后台处理）
        boolean isProcessing = activeRecipe != null && 
            (
                activeRecipe.isProcessing() || 
                recipeManager.isBackgroundProcessing(getBindGroup())
            );
        if (isProcessing) {
            float progress = activeRecipe.getProgress();
            int progressWidth = (int) (width * progress);
            
            // 渲染进度条背景
            guiGraphics.fill(
                getX(), getY() + height - 3,
                getX() + width, getY() + height,
                0x80000000
            );
            
            // 渲染进度条
            guiGraphics.fill(
                getX(), getY() + height - 3,
                getX() + progressWidth, getY() + height,
                0xFF00FF00
            );
            
            // 如果鼠标悬停，显示进度信息
            if (isMouseOver(mouseX, mouseY)) {
                String progressText = String.format("%.1f%%", progress * 100);
                guiGraphics.renderTooltip(
                    minecraft.font,
                    Component.literal(progressText),
                    mouseX, mouseY
                );
            }
        }
    }

     // 从存储加载物品
    private void loadItemFromStorage() {
        String slotKey = getValidatedSlotKey();
        if (blockKey != null && !blockKey.isEmpty()) {
            // 加载方块特定的插槽数据
            this.itemStack = BlockBoundSlotDataManager.loadBlockSlot(blockKey, slotKey);
            UIJS.LOGGER.debug("Loaded block slot: {}/{} - {}", blockKey, slotKey, itemStack);
        } else {
            // 加载全局插槽数据
            this.itemStack = SlotDataManager.loadSlotData(getSlotKey());
            UIJS.LOGGER.debug("Loaded global slot: {} - {}", slotKey, itemStack);
        }
    }
    
    // 保存物品到存储
    private void saveItemToStorage() {
        String slotKey = getValidatedSlotKey();
        if (blockKey != null && !blockKey.isEmpty()) {
            // 保存方块特定的插槽数据
            BlockBoundSlotDataManager.saveBlockSlot(blockKey, slotKey, itemStack);
            UIJS.LOGGER.debug("Saved block slot: {}/{} - {}", blockKey, slotKey, itemStack);
            syncBlockSlotToServer(slotKey);
        } else {
            // 保存全局插槽数据
            SlotDataManager.saveSlotData(getSlotKey(), itemStack, true);
            UIJS.LOGGER.debug("Saved global slot: {} - {}", slotKey, itemStack);
        }
    }

    private String getValidatedSlotKey() {
        String type = getType();
        String group = getBindGroup();
        
        // 验证并修复键格式
        if (type == null || type.isEmpty()) {
            type = "def";
            UIJS.LOGGER.warn("Slot type is null or empty, using default: def");
        }
        
        if (group == null || group.isEmpty()) {
            group = "default";
            UIJS.LOGGER.warn("Slot bindGroup is null or empty, using default: default");
        }
        
        // 生成标准格式的键
        return SlotDataManager.generateSlotKey(type, order, group);
    }
    
    //#region 鼠标事件
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.active && this.visible && this.isValidClickButton(button)) {
            if (this.clicked(mouseX, mouseY)) {
                // 处理物品放入/取出逻辑
                handleItemInteraction(button);
                return true;
            }
        }
        return false;
    }

    private void handleItemInteraction(int button) {
        Player player = minecraft.player;
        if (player == null) return;

        // 获取玩家当前手持的物品
        ItemStack carriedItem = player.containerMenu.getCarried();

        ItemInteractionHelper.InteractionResult result = null;

        if (isOutputSlot()) {
            handleOutputSlotInteraction(button, carriedItem, player);
            return;
        }
        
        // 左键点击
        if (button == 0) {
            result = ItemInteractionHelper.handleLeftClick(carriedItem, this.itemStack, player);
        }
        // 右键点击
        else if (button == 1) {
            result = ItemInteractionHelper.handleRightClick(carriedItem, this.itemStack, player);
        }

        if (result != null && result.isChanged()) {
            // 先验证变化是否合理
            if (isValidItemChange(carriedItem, this.itemStack, result.getCarriedItem(), result.getSlotItem())) {
                player.containerMenu.setCarried(result.getCarriedItem());
                // 更新物品堆栈
                this.itemStack = result.getSlotItem();
                
                // 然后保存到存储（会自动同步到服务器）
                saveItemToStorage();

                // 检查配方状态
                checkRecipeState();
                
                // 强制更新物品显示
                player.containerMenu.broadcastChanges();
            }
            
        }
    }

    /**
     * 处理输出插槽的交互逻辑 - 只允许取出，不允许放入
     */
    private void handleOutputSlotInteraction(int button, ItemStack carriedItem, Player player) {
        // 如果玩家手持物品，不允许放入输出插槽
        if (!carriedItem.isEmpty()) return;

        // 如果输出插槽为空，无操作
        if (this.itemStack.isEmpty()) return;

        ItemInteractionHelper.InteractionResult result = null;
        
        // 左键点击：取出所有物品
        if (button == 0) {
            result = handleOutputTakeAll();
        }

        if (result != null && result.isChanged()) {
            // 验证变化是否合理
            if (isValidItemChange(carriedItem, this.itemStack, result.getCarriedItem(), result.getSlotItem())) {
                player.containerMenu.setCarried(result.getCarriedItem());
                // 更新物品堆栈
                this.itemStack = result.getSlotItem();
                
                // 保存到存储
                saveItemToStorage();

                checkRecipeState();
                
                // 强制更新物品显示
                player.containerMenu.broadcastChanges();
            }
        }
    }

    /**
     * 输出插槽：取出所有物品
     */
    private ItemInteractionHelper.InteractionResult handleOutputTakeAll() {
        ItemInteractionHelper.InteractionResult result = new ItemInteractionHelper.InteractionResult(
            ItemStack.EMPTY, this.itemStack
        );
        
        result.setCarriedItem(this.itemStack.copy());
        result.setSlotItem(ItemStack.EMPTY);
        result.setChanged(true);

        Minecraft.getInstance().execute(() -> {
            checkRecipeState();
        });
        
        return result;
    }


    /**
     * 验证物品变化是否合理
     */
    private boolean isValidItemChange(ItemStack originalCarried, ItemStack originalSlot, 
                                    ItemStack newCarried, ItemStack newSlot) {
        // 检查物品总数是否一致（防止复制）
        int originalTotal = originalCarried.getCount() + originalSlot.getCount();
        int newTotal = newCarried.getCount() + newSlot.getCount();
        
        if (originalTotal != newTotal) {
            return false;
        }

        if (!newSlot.isEmpty() && newSlot.getCount() > newSlot.getMaxStackSize()) {
            return false;
        }
        
        if (!newCarried.isEmpty() && newCarried.getCount() > newCarried.getMaxStackSize()) {
            return false;
        }
        
        if (!areItemsCompatible(originalCarried, originalSlot, newCarried, newSlot)) {
            return false;
        }
        
        return true;
    }

    /**
     * 检查物品类型兼容性
     */
    private boolean areItemsCompatible(ItemStack originalCarried, ItemStack originalSlot, 
                                    ItemStack newCarried, ItemStack newSlot) {
        // 空堆栈检查
        if (originalCarried.isEmpty() && originalSlot.isEmpty()) {
            return newCarried.isEmpty() && newSlot.isEmpty();
        }
        
        // 检查物品类型变化是否合理
        if (!originalCarried.isEmpty() && !originalSlot.isEmpty()) {
            // 如果原来是不同类型物品，交换后类型应该互换
            if (!ItemStack.isSameItemSameTags(originalCarried, originalSlot)) {
                return (ItemStack.isSameItemSameTags(originalCarried, newSlot) && 
                        ItemStack.isSameItemSameTags(originalSlot, newCarried)) ||
                    (newCarried.isEmpty() && ItemStack.isSameItemSameTags(originalSlot, newSlot)) ||
                    (newSlot.isEmpty() && ItemStack.isSameItemSameTags(originalCarried, newCarried));
            } else {
                // 如果是同类物品合并，类型应该保持不变
                return ItemStack.isSameItemSameTags(originalCarried, newCarried) || 
                    newCarried.isEmpty() ||
                    ItemStack.isSameItemSameTags(originalCarried, newSlot) ||
                    newSlot.isEmpty();
            }
        }
        
        // 放置或取出操作
        if (originalCarried.isEmpty() && !originalSlot.isEmpty()) {
            // 取出操作：新手持物品应该与原始插槽物品相同
            return ItemStack.isSameItemSameTags(originalSlot, newCarried) && newSlot.isEmpty();
        }
        
        if (!originalCarried.isEmpty() && originalSlot.isEmpty()) {
            // 放置操作：新插槽物品应该与原始手持物品相同
            return ItemStack.isSameItemSameTags(originalCarried, newSlot) && newCarried.isEmpty();
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // 确保物品状态正确同步
        Player player = minecraft.player;
        if (player != null) {
            // 强制更新手持物品显示
            player.containerMenu.broadcastChanges();
            saveItemToStorage();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    //#endregion

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        Component narration;
        if (!itemStack.isEmpty()) {
            narration = Component.translatable("narration.solt.filled", 
                order, getType(), itemStack.getDisplayName(), itemStack.getCount());
        } else {
            narration = Component.translatable("narration.solt.empty", order, getType());
        }
        
        narrationElementOutput.add(NarratedElementType.TITLE, narration);
    }

    //#region 配方相关
    // 检查是否为配方插槽（非物品栏插槽）
    public boolean isRecipeSlot() {
        return !"inventory".equals(this.bindGroup);
    }

    // 检查是否为输入插槽
    public boolean isInputSlot() { return "input".equals(getType()); }
    // 检查是否为输出插槽
    public boolean isOutputSlot() { return "output".equals(getType()); }

    // 检查并更新配方状态
    private void checkRecipeState() {
        if (!isRecipeSlot()) return;
        
        Object recipeManager = getCurrentRecipeManager();
        String group = getBindGroup();
        
        Map<Integer, ItemStack> slotItems = getSlotItemsForGroup(group);
        
        RecipeSystem matchingRecipe = findMatchingRecipe(recipeManager, group, slotItems);
        RecipeSystem activeRecipe = getActiveRecipe(recipeManager, group);
        
        if (matchingRecipe != null) {
            if (activeRecipe == null) {
                startRecipeProcessing(recipeManager, group, matchingRecipe);
            } else if (activeRecipe != matchingRecipe) {
                stopRecipeProcessing(recipeManager, group);
                startRecipeProcessing(recipeManager, group, matchingRecipe);
            }
        } else {
            if (activeRecipe != null) {
                stopRecipeProcessing(recipeManager, group);
            }

        }

    }

    // 获取配方组的所有插槽物品
    private Map<Integer, ItemStack> getSlotItemsForGroup(String group) {
        Map<Integer, ItemStack> slotItems = new HashMap<>();
        
        // 尝试通过UIRenderer获取同组插槽
        Screen currentScreen = Minecraft.getInstance().screen;
        if (currentScreen instanceof UIRenderer uiRenderer) {
            Map<Integer, SoltWidget> groupSlots = uiRenderer.getGroupSlots(group);
            
            for (Map.Entry<Integer, SoltWidget> entry : groupSlots.entrySet()) {
                slotItems.put(entry.getKey(), entry.getValue().getItemStack());
            }
        } else {
            // 回退方案：只包含当前插槽
            slotItems.put(this.order, this.itemStack);
        }
        
        return slotItems;
    }

    /**
     * 同步方块插槽数据到服务器
     */
    private void syncBlockSlotToServer(String slotKey) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player != null && blockKey != null) {
            // 查找来源插槽
            int sourceSlot = findSourceInventorySlot(player.containerMenu.getCarried());
            
            // 验证物品所有权
            if (sourceSlot != -1) {
                ItemStack currentItem = player.getInventory().getItem(sourceSlot);
                if (!ItemStack.isSameItemSameTags(currentItem, player.containerMenu.getCarried())) {
                    sourceSlot = -1;
                }
            }

            // 发送方块特定的同步包
            String fullSlotKey = "block_" + blockKey + "_" + slotKey;
            NetworkHandler.sendToServer(new SlotSyncPacket(
                fullSlotKey,
                this.itemStack, 
                player.containerMenu.getCarried(), 
                player.getUUID(), 
                sourceSlot
            ));
            
            UIJS.LOGGER.debug("Synced block slot to server: {} - {}", fullSlotKey, this.itemStack);
        }
    }

    // 添加查找来源插槽的方法
    private int findSourceInventorySlot(ItemStack carriedItem) {
        if (carriedItem.isEmpty()) return -1;
        
        Player player = Minecraft.getInstance().player;
        if (player == null) return -1;
        
        // 查找物品栏中匹配的物品堆栈
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (ItemStack.isSameItemSameTags(stack, carriedItem)) {
                // 如果数量也匹配，优先返回
                if (stack.getCount() == carriedItem.getCount()) {
                    return i;
                }
            }
        }
        
        return -1;
    }
    //#endregion

    //#region Getter & Setter
    public String getType() { return node.getAttribute("type") == null ? "def" : node.getAttribute("type"); }
    public int getOrder() { return order; }
    public String getBindGroup() { return bindGroup; }
    public ItemStack getItemStack() { return itemStack.copy(); }
    public void setItemStack(ItemStack stack) { 
        this.itemStack = stack.copy(); // 存储副本
        saveItemToStorage(); // 自动保存 
    }
    
    // 获取插槽索引（用于配方系统）
    public int getSlotIndex() {
        return this.order;
    }

    // 获取插槽键（仅用于全局插槽）
    private String getSlotKey() {
        return SlotDataManager.generateSlotKey(getType(), order, bindGroup);
    }

    // 获取配方组键
    public String getRecipeGroupKey() {
        return getBindGroup() + "_" + getType();
    }
    //#endregion

    // 静态方法：清空所有插槽数据
    public static void clearAllSlotData() {
        SlotDataManager.clearAllSlotData();
    }
    
    // 静态方法：获取特定插槽的数据
    public static ItemStack getSlotData(String type, int order, String bindGroup) {
        return SlotDataManager.getSlotData(type, order, bindGroup);
    }
    

    //#region 辅助方法
    // 辅助方法：获取当前配方管理器
    private Object getCurrentRecipeManager() {
        Screen currentScreen = Minecraft.getInstance().screen;
        if (currentScreen instanceof UIRenderer uiRenderer) {
            return uiRenderer.getCurrentRecipeManager();
        }
        return RecipeManager.getInstance();
    }
    
    // 辅助方法：查找匹配的配方
    private RecipeSystem findMatchingRecipe(Object recipeManager, String group, Map<Integer, ItemStack> slotItems) {
        if (recipeManager instanceof BlockBoundRecipeManager blockManager) {
            return blockManager.findMatchingRecipe(group, slotItems);
        } else if (recipeManager instanceof RecipeManager globalManager) {
            return globalManager.findMatchingRecipe(group, slotItems);
        }
        return null;
    }
    
    // 辅助方法：获取活跃配方
    private RecipeSystem getActiveRecipe(Object recipeManager, String group) {
        if (recipeManager instanceof BlockBoundRecipeManager blockManager) {
            return blockManager.getActiveRecipe(group);
        } else if (recipeManager instanceof RecipeManager globalManager) {
            return globalManager.getActiveRecipe(group);
        }
        return null;
    }
    
    // 辅助方法：启动配方处理
    private void startRecipeProcessing(Object recipeManager, String group, RecipeSystem recipe) {
        if (recipeManager instanceof BlockBoundRecipeManager blockManager) {
            blockManager.startBackgroundProcessing(group, recipe);
        } else if (recipeManager instanceof RecipeManager globalManager) {
            globalManager.startBackgroundProcessing(group, recipe);
        }
    }
    
    // 辅助方法：停止配方处理
    private void stopRecipeProcessing(Object recipeManager, String group) {
        if (recipeManager instanceof BlockBoundRecipeManager blockManager) {
            blockManager.stopRecipe(group);
        } else if (recipeManager instanceof RecipeManager globalManager) {
            globalManager.stopRecipe(group);
        }
    }

    //#endregion
}
