package com.core.UIJS.ui;

import java.util.ArrayList;
import java.util.List;

import com.core.UIJS.mcml.MCMLNode;
import com.core.UIJS.network.InventorySyncPacket;
import com.core.UIJS.network.NetworkHandler;
import com.core.UIJS.util.ItemInteractionHelper;
import com.core.UIJS.util.UIRenderHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class InventoryWidget extends AbstractWidget {
    private final MCMLNode node;
    private final int guiLeft, guiTop, guiWidth, guiHeight;
    private final String type;
    private final int slotSize;
    private final boolean isItemBound;
    
    private final List<InventorySlotInfo> slots;

    public InventoryWidget(
        int guiLeft, int guiTop, 
        int guiWidth, int guiHeight, 
        String type, 
        int slotSize, 
        boolean isItemBound,
        MCMLNode node
    ) {
        super(0, 0, 0, 0, Component.literal("Inventory"));
        this.guiLeft = guiLeft;
        this.guiTop = guiTop;
        this.guiWidth = guiWidth;
        this.guiHeight = guiHeight;
        this.type = type;
        this.slotSize = slotSize;
        this.isItemBound = isItemBound;
        this.node = node;
        this.slots = new ArrayList<>();
        
        // 设置widget的边界为整个屏幕\
        this.setWidth(Minecraft.getInstance().getWindow().getGuiScaledWidth());
        this.setHeight(Minecraft.getInstance().getWindow().getGuiScaledHeight());
    }

    //#region 渲染

    // 渲染组件
    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        
        Inventory inventory = player.getInventory();
        slots.clear();
        
        switch (type) {
            case "bottom":
                renderBottomInventory(guiGraphics, inventory, mouseX, mouseY);
                break;
            case "around":
                renderAroundInventory(guiGraphics, inventory, mouseX, mouseY);
                break;
            case "both":
            default:
                renderBothInventory(guiGraphics, inventory, mouseX, mouseY);
                break;
        }
        renderCarriedItem(guiGraphics, mouseX, mouseY);
    }

    // 在GUI下方渲染物品栏
    private void renderBottomInventory(GuiGraphics guiGraphics, Inventory inventory, int mouseX, int mouseY) {
        int startX = guiLeft + (guiWidth - 9 * slotSize) / 2;
        int startY = guiTop + guiHeight + 5;
        
        // 在左边添加2x2装备栏
        int equipmentX = startX - 2 * slotSize - 10;
        int equipmentY = startY;
        
        // 装备栏: 头盔(39), 胸甲(38), 护腿(37), 靴子(36)
        renderEquipmentSlot(guiGraphics, inventory, 39, equipmentX, equipmentY, mouseX, mouseY); // 头盔
        renderEquipmentSlot(guiGraphics, inventory, 38, equipmentX + slotSize, equipmentY, mouseX, mouseY); // 胸甲
        renderEquipmentSlot(guiGraphics, inventory, 37, equipmentX, equipmentY + slotSize, mouseX, mouseY); // 护腿
        renderEquipmentSlot(guiGraphics, inventory, 36, equipmentX + slotSize, equipmentY + slotSize, mouseX, mouseY); // 靴子
        
        // 在装备栏下面添加副手插槽
        int offhandY = equipmentY + 2 * slotSize + 5;
        renderEquipmentSlot(guiGraphics, inventory, 40, equipmentX, offhandY, mouseX, mouseY); // 副手
        
        // 渲染主物品栏 (3行 x 9列)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = col + row * 9 + 9; // +9 跳过快捷栏
                int x = startX + col * slotSize;
                int y = startY + row * slotSize;
                
                slots.add(new InventorySlotInfo(slotIndex, x, y, slotSize, slotSize));
                renderInventorySlot(guiGraphics, inventory, slotIndex, x, y, mouseX, mouseY);
            }
        }
        
        // 渲染快捷栏 (1行 x 9列)
        int hotbarY = startY + 3 * slotSize + 5;
        for (int col = 0; col < 9; col++) {
            int slotIndex = col;
            int x = startX + col * slotSize;
            int y = hotbarY;
            
            slots.add(new InventorySlotInfo(slotIndex, x, y, slotSize, slotSize));
            renderInventorySlot(guiGraphics, inventory, slotIndex, x, y, mouseX, mouseY);
        }
    }
    
    // 在GUI周围渲染物品栏
    private void renderAroundInventory(GuiGraphics guiGraphics, Inventory inventory, int mouseX, int mouseY) {
        int centerX = guiLeft + guiWidth / 2;
        int centerY = guiTop + guiHeight / 2;
        
        // 在gui下面渲染快捷栏
        int hotbarStartX = centerX - (9 * slotSize) / 2;
        int hotbarStartY = guiTop + guiHeight + 5;
        for (int col = 0; col < 9; col++) {
            int slotIndex = col;
            int x = hotbarStartX + col * slotSize;
            int y = hotbarStartY;

            slots.add(new InventorySlotInfo(slotIndex, x, y, slotSize, slotSize));
            renderInventorySlot(guiGraphics, inventory, slotIndex, x, y, mouseX, mouseY);
        }
        
        // 在快捷栏左面渲染副手插槽
        int offhandX = hotbarStartX - slotSize - 5;
        int offhandY = hotbarStartY;
        renderEquipmentSlot(guiGraphics, inventory, 40, offhandX, offhandY, mouseX, mouseY);
        
        // 在gui左边纵向渲染第一行主物品栏 (9-17)
        int leftStartX = guiLeft - slotSize - 5;
        int leftStartY = centerY - (9 * slotSize) / 2;
        for (int row = 0; row < 9; row++) {
            int slotIndex = row + 9; // 第一行主物品栏
            int x = leftStartX;
            int y = leftStartY + row * slotSize;

            slots.add(new InventorySlotInfo(slotIndex, x, y, slotSize, slotSize));
            renderInventorySlot(guiGraphics, inventory, slotIndex, x, y, mouseX, mouseY);
        }
        
        // 在gui上面渲染第二行主物品栏 (18-26)
        int topStartX = centerX - (9 * slotSize) / 2;
        int topStartY = guiTop - slotSize - 5;
        for (int col = 0; col < 9; col++) {
            int slotIndex = col + 18; // 第二行主物品栏
            int x = topStartX + col * slotSize;
            int y = topStartY;

            slots.add(new InventorySlotInfo(slotIndex, x, y, slotSize, slotSize));
            renderInventorySlot(guiGraphics, inventory, slotIndex, x, y, mouseX, mouseY);
        }
        
        // 在gui右面纵向渲染第三行主物品栏 (27-35)
        int rightStartX = guiLeft + guiWidth + 5;
        int rightStartY = centerY - (9 * slotSize) / 2;
        for (int row = 0; row < 9; row++) {
            int slotIndex = row + 27; // 第三行主物品栏
            int x = rightStartX;
            int y = rightStartY + row * slotSize;

            slots.add(new InventorySlotInfo(slotIndex, x, y, slotSize, slotSize));
            renderInventorySlot(guiGraphics, inventory, slotIndex, x, y, mouseX, mouseY);
        }
        
        // 在左上角渲染装备栏的其中两个 (头盔39, 胸甲38)
        int topLeftX = guiLeft - slotSize - 5;
        int topLeftY = guiTop - slotSize - 5;
        renderEquipmentSlot(guiGraphics, inventory, 39, topLeftX - slotSize, topLeftY, mouseX, mouseY); // 头盔
        renderEquipmentSlot(guiGraphics, inventory, 38, topLeftX, topLeftY, mouseX, mouseY); // 胸甲
        
        // 在右上角渲染装备栏的另外两个 (护腿37, 靴子36)
        int topRightX = guiLeft + guiWidth + 5;
        int topRightY = guiTop - slotSize - 5;
        renderEquipmentSlot(guiGraphics, inventory, 37, topRightX, topRightY, mouseX, mouseY); // 护腿
        renderEquipmentSlot(guiGraphics, inventory, 36, topRightX + slotSize, topRightY, mouseX, mouseY); // 靴子
    }
    
    // 在GUI两侧渲染物品栏
    private void renderBothInventory(GuiGraphics guiGraphics, Inventory inventory, int mouseX, int mouseY) {
        int centerY = guiTop + guiHeight / 2;

        // 计算四列的起始位置
        int column1X = guiLeft - 2 * slotSize - 10; // 第一数列（最左）
        int column2X = guiLeft - slotSize - 10; // 第二数列
        int column3X = guiLeft + guiWidth + 10;     // 第三数列
        int column4X = guiLeft + guiWidth + slotSize + 10; // 第四数列（最右）
        
        int columnStartY = centerY - (9 * slotSize) / 2;
        
        // 第一数列：快捷栏 (0-8) - 纵向排列
        for (int row = 0; row < 9; row++) {
            int slotIndex = row;
            int x = column1X;
            int y = columnStartY + row * slotSize;

            slots.add(new InventorySlotInfo(slotIndex, x, y, slotSize, slotSize));
            renderInventorySlot(guiGraphics, inventory, slotIndex, x, y, mouseX, mouseY);
        }
        
        // 第二数列：主物品栏第一行 (9-17) - 纵向排列
        for (int row = 0; row < 9; row++) {
            int slotIndex = row + 9;
            int x = column2X;
            int y = columnStartY + row * slotSize;

            slots.add(new InventorySlotInfo(slotIndex, x, y, slotSize, slotSize));
            renderInventorySlot(guiGraphics, inventory, slotIndex, x, y, mouseX, mouseY);
        }
        
        // 第三数列：主物品栏第二行 (18-26) - 纵向排列
        for (int row = 0; row < 9; row++) {
            int slotIndex = row + 18;
            int x = column3X;
            int y = columnStartY + row * slotSize;

            slots.add(new InventorySlotInfo(slotIndex, x, y, slotSize, slotSize));
            renderInventorySlot(guiGraphics, inventory, slotIndex, x, y, mouseX, mouseY);
        }
        
        // 第四数列：主物品栏第三行 (27-35) - 纵向排列
        for (int row = 0; row < 9; row++) {
            int slotIndex = row + 27;
            int x = column4X;
            int y = columnStartY + row * slotSize;

            slots.add(new InventorySlotInfo(slotIndex, x, y, slotSize, slotSize));
            renderInventorySlot(guiGraphics, inventory, slotIndex, x, y, mouseX, mouseY);
        }
        
        // 在底部渲染装备栏和副手
        int bottomStartX = guiLeft + (guiWidth - 6 * slotSize) / 2;
        int bottomStartY = guiTop + guiHeight + 5;
        
        // 装备栏: 头盔(39), 胸甲(38), 护腿(37), 靴子(36)
        renderEquipmentSlot(guiGraphics, inventory, 39, bottomStartX + slotSize, bottomStartY, mouseX, mouseY);
        renderEquipmentSlot(guiGraphics, inventory, 38, bottomStartX + 2 * slotSize, bottomStartY, mouseX, mouseY);
        renderEquipmentSlot(guiGraphics, inventory, 37, bottomStartX + 3 * slotSize, bottomStartY, mouseX, mouseY);
        renderEquipmentSlot(guiGraphics, inventory, 36, bottomStartX + 4 * slotSize, bottomStartY, mouseX, mouseY);
        
        // 副手(40)
        renderEquipmentSlot(guiGraphics, inventory, 40, bottomStartX - 1 * slotSize, bottomStartY, mouseX, mouseY);
    }

    // 渲染单个物品栏插槽
    private void renderInventorySlot(GuiGraphics guiGraphics, Inventory inventory, int slotIndex, int x, int y, int mouseX, int mouseY) {
        String mainBackgroundColorStr = node.getAttributeOrStyle("mainBackgroundColor");
        String mainBorderColorStr = node.getAttributeOrStyle("mainBorderColor");
        String mainHoverCoverColorStr = node.getAttributeOrStyle("mainHoverCoverColor");
        String mainHoverBorderColorStr = node.getAttributeOrStyle("mainHoverBorderColor");
        int mainBackgroundColor = UIRenderHelper.parseColor(mainBackgroundColorStr, 0x80FFFFFF);
        int mainBorderColor = UIRenderHelper.parseColor(mainBorderColorStr, 0xFF8B8B8B);
        int mainHoverCoverColor = UIRenderHelper.parseColor(mainHoverCoverColorStr, 0x80FFFFFF);
        int mainHoverBorderColor = UIRenderHelper.parseColor(mainHoverBorderColorStr, 0xFF08FF18);
        
        // 检查插槽索引是否有效
        if (slotIndex < 0 || slotIndex >= inventory.getContainerSize()) return;
        
        ItemStack itemStack = inventory.getItem(slotIndex);
        
        // 渲染插槽背景
        guiGraphics.fill(
            x, y,
            x + slotSize, y + slotSize,
            mainBackgroundColor
        );
        
        // 渲染边框
        int borderColor = mainBorderColor; // 默认灰色边框
        
        // 如果是当前选中的快捷栏插槽并且是物品绑定模式
        if (slotIndex == inventory.selected && slotIndex < 9 && isItemBound) {
            borderColor = 0xFF0000;
        }

        guiGraphics.renderOutline(
            x, y,
            slotSize, slotSize,
            borderColor
        );
        
        // 渲染物品
        UIRenderHelper.renderItemInSlot(guiGraphics, itemStack, x, y, slotSize, slotSize);
        
        // 检查鼠标悬停
        UIRenderHelper.renderHover(guiGraphics, x, y, slotSize, mouseX, mouseY, mainHoverCoverColor, mainHoverBorderColor);
    }

    // 渲染装备栏插槽
    private void renderEquipmentSlot(GuiGraphics guiGraphics, Inventory inventory, int slotIndex, int x, int y, int mouseX, int mouseY) {
        String equipmentBackgroundColorStr = node.getAttributeOrStyle("equipmentBackgroundColorStr");
        String equipmentBorderColorStr = node.getAttributeOrStyle("equipmentBorderColorStr");
        String equipmentHoverCoverColorStr = node.getAttributeOrStyle("equipmentHoverCoverColorStr");
        String equipmentHoverBorderColorStr = node.getAttributeOrStyle("equipmentHoverBorderColorStr");
        int equipmentBackgroundColor = UIRenderHelper.parseColor(equipmentBackgroundColorStr, 0x80FFD700);
        int equipmentBorderColor = UIRenderHelper.parseColor(equipmentBorderColorStr, 0xFF4169E1);
        int equipmentHoverCoverColor = UIRenderHelper.parseColor(equipmentHoverCoverColorStr, 0x80FFFFFF);
        int equipmentHoverBorderColor = UIRenderHelper.parseColor(equipmentHoverBorderColorStr, 0xFFBE961C);

        
        // 检查插槽索引是否有效
        if (slotIndex < 0 || slotIndex >= inventory.getContainerSize()) return;
        
        ItemStack itemStack = inventory.getItem(slotIndex);
        
        // 渲染插槽背景 - 装备栏使用不同颜色
        guiGraphics.fill(
            x, y,
            x + slotSize, y + slotSize,
            equipmentBackgroundColor
        );
        
        // 渲染边框

        guiGraphics.renderOutline(
            x, y,
            slotSize, slotSize,
            equipmentBorderColor
        );
        
        // 渲染物品
        UIRenderHelper.renderItemInSlot(guiGraphics, itemStack, x, y, slotSize, slotSize);
        
        // 添加到插槽列表以便交互
        slots.add(new InventorySlotInfo(slotIndex, x, y, slotSize, slotSize));
        
        // 检查鼠标悬停
        UIRenderHelper.renderHover(guiGraphics, x, y, slotSize, mouseX, mouseY, equipmentHoverCoverColor, equipmentHoverBorderColor);
    }

    // 渲染手持物品
    private void renderCarriedItem(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        ItemStack carried = getCarriedStack();
        if (!carried.isEmpty()) {
            guiGraphics.renderItem(carried, mouseX - 8, mouseY - 8);
            guiGraphics.renderItemDecorations(Minecraft.getInstance().font, carried, mouseX - 8, mouseY - 8);
        }
    }
    
    //#endregion

    //#region 鼠标事件

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return false;
        
        Inventory inventory = player.getInventory();
        
        // 查找被点击的插槽
        InventorySlotInfo clickedSlot = null;
        for (InventorySlotInfo slot : slots) {
            if (slot.isMouseOver(mouseX, mouseY)) {
                clickedSlot = slot;
                break;
            }
        }
        
        if (clickedSlot != null) {
            // 检查是否应该阻止点击
            if (shouldBlockClick(clickedSlot.slotIndex, inventory)) {
                return false; // 阻止点击
            }

            handleSlotClick(inventory, clickedSlot.slotIndex, button);
            return true;
        }
        
        return false;
    }

    // 检查是否应该阻止点击
    private boolean shouldBlockClick(int slotIndex, Inventory inventory) {
        // 如果当前UI绑定到物品，并且点击的是当前选中的快捷栏插槽
        if (isItemBound && slotIndex == inventory.selected && slotIndex < 9) {
            return true; // 阻止点击
        }
        return false;
    }

    // 鼠标释放
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    // 鼠标拖拽
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    
    // 处理插槽点击
    private void handleSlotClick(Inventory inventory, int slotIndex, int button) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        
        ItemStack carriedStack = getCarriedStack();
        ItemStack slotStack = inventory.getItem(slotIndex);
        
        ItemInteractionHelper.InteractionResult result = null;
        
        // 左键点击
        if (button == 0) {
            result = ItemInteractionHelper.handleLeftClick(carriedStack, slotStack, Minecraft.getInstance().player);
        }
        // 右键点击
        else if (button == 1) {
            result = ItemInteractionHelper.handleRightClick(carriedStack, slotStack, Minecraft.getInstance().player);
        }
        
        if (result != null && result.isChanged()) {
            setCarriedStack(result.getCarriedItem());
            inventory.setItem(slotIndex, result.getSlotItem());
        }
        
        if (result != null && result.isChanged()) {
            // 更新客户端显示
            setCarriedStack(result.getCarriedItem());
            inventory.setItem(slotIndex, result.getSlotItem());
            
            // 同步到服务器
            syncInventoryToServer(slotIndex, result.getSlotItem());
            
            // 更新物品显示
            player.containerMenu.broadcastChanges();
        }
    }

    /**
     * 同步物品栏变更到服务器
     */
    private void syncInventoryToServer(int slotIndex, ItemStack slotItem) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            NetworkHandler.sendToServer(new InventorySyncPacket(
                slotIndex, 
                slotItem, 
                player.getUUID()
            ));
        }
    }

    //#endregion
    
    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        //  narration support
    }
    
    // Getter方法
    public String getType() { return type; }
    public int getSlotSize() { return slotSize; }

    
     // 获取当前手持物品的方法
    private ItemStack getCarriedStack() {
        Player player = Minecraft.getInstance().player;
        return player != null ? player.containerMenu.getCarried() : ItemStack.EMPTY;
    }
    
    // 设置当前手持物品的方法
    private void setCarriedStack(ItemStack stack) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            player.containerMenu.setCarried(stack);
        }
    }
    

    // 内部类用于存储插槽信息
    private static class InventorySlotInfo {
        final int slotIndex;
        final int x;
        final int y;
        final int width;
        final int height;
        
        InventorySlotInfo(int slotIndex, int x, int y, int width, int height) {
            this.slotIndex = slotIndex;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
        
        boolean isMouseOver(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width &&
                   mouseY >= y && mouseY <= y + height;
        }
    }
}
