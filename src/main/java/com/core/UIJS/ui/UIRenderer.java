package com.core.UIJS.ui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.core.UIJS.Config;
import com.core.UIJS.UIJS;
import com.core.UIJS.mcml.MCMLNode;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.ImageWidget;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

// UI渲染器
public class UIRenderer extends Screen {
    private final ResourceLocation uiId;
    private final MCMLNode uiRoot;
    private final List<AbstractWidget> widgets;
    private int originalWidth;
    private int originalHeight;
    private int left;
    private int top;

    // 网络背景图片相关字段
    private ResourceLocation networkBackgroundTexture = null;
    private boolean isNetworkBackgroundLoading = false;
    private long lastBackgroundCheckTime = 0;

    
    public UIRenderer(ResourceLocation uiId, MCMLNode uiRoot) {
        super(Component.literal("MCML UI"));
        this.uiId = uiId;
        this.uiRoot = uiRoot;
        this.widgets = new ArrayList<>();
    }
    
    public static void openUI(ResourceLocation uiId) {
        MCMLNode uiNode = UIRegistry.getUI(uiId);
        if (uiNode != null) {
            Minecraft.getInstance().setScreen(new UIRenderer(uiId, uiNode));
        } else {
            UIJS.LOGGER.error("Failed to open UI: {}, UI node not found", uiId);
        }
    }

    //#region 初始化方法
    @Override
    protected void init() {
        super.init();
        this.widgets.clear();

        this.originalWidth = this.width;
        this.originalHeight = this.height;
        // 重置窗口大小
        this.width = parsePosition(uiRoot.getAttributeOrStyle("width"), this.width, this.width);
        this.height = parsePosition(uiRoot.getAttributeOrStyle("height"), this.height, this.height);

        int[] defaultOffset = parseTranslate(uiRoot, this.width, this.height, "-50%", "-50%");

        this.left = this.originalWidth / 2 + defaultOffset[0];
        this.top = this.originalHeight / 2 + defaultOffset[1];

        // 初始化UI组件
        parseAndCreateWidgets(uiRoot, this.left, this.top);

        // 将所有widget添加到屏幕
        for (AbstractWidget widget : widgets) {
            this.addRenderableWidget(widget);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // 渲染背景
        String backgroundImage = uiRoot.getAttributeOrStyle("backgroundImage");
        if (backgroundImage != null && !backgroundImage.isEmpty()) {
            if (isHttpUrl(backgroundImage)) {
                // HTTP 网络图片
                renderNetworkBackground(guiGraphics, backgroundImage);
            } else {
                // 本地图片
                renderLocalBackground(guiGraphics, backgroundImage);
            }
        } else {
            // 纯色背景
            renderSolidBackground(guiGraphics);
        }

        
        // 渲染UI组件（通过父类自动渲染已注册的widgets）
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        // 渲染自定义节点
        renderCustomNodes(guiGraphics, uiRoot, this.left, this.top);
    }

    @Override
    public boolean isPauseScreen() {
        return false; // 游戏不会在打开此UI时暂停
    }
    //#endregion


    //#region 组件创建方法
    // 解析MCML节点并创建对应的UI组件
    private void parseAndCreateWidgets(MCMLNode node, int parentX, int parentY) {
        // 获取样式和属性中的位置信息
        String leftStr = node.getComputedStyles().get("left");
        if (leftStr == null) leftStr = node.getAttributeOrStyle("left");
        
        String topStr = node.getComputedStyles().get("top");
        if (topStr == null) topStr = node.getAttributeOrStyle("top");
        
        String widthStr = node.getComputedStyles().get("width");
        if (widthStr == null) widthStr = node.getAttributeOrStyle("width");
        
        String heightStr = node.getComputedStyles().get("height");
        if (heightStr == null) heightStr = node.getAttributeOrStyle("height");
        
        // 计算位置和尺寸
        int x = parentX + parsePosition(leftStr, this.width, 0);
        int y = parentY + parsePosition(topStr, this.height, 0);
        System.out.println(node.toJson());
        // 根据节点类型创建对应的UI组件
        switch (node.getTagName()) {
            case "input":
                createInputWidget(node);
                break;
            case "button":
                createButtonWidget(node);
                break;
            case "image":
                createImageWidget(node);
                break;
            case "checkbox":
                createCheckboxWidget(node);
                break;
            case "cycleButton":
                createCycleButtonWidget(node);
                break;
            case "solt":
                createSoltWidget(node);
                break;
            case "inventory":
                createInventoryWidget(node);
                break;
        }
        
        // 递归处理子节点
        for (MCMLNode child : node.getChildren()) {
            parseAndCreateWidgets(child, x, y);
        }
    }

    // 添加输入框组件
    private void createInputWidget(MCMLNode node) {
        String placeholder = node.getAttributeOrStyle("placeholder");
        String defaultValue = node.getTextContent();
        if (defaultValue.isEmpty())  defaultValue = node.getAttributeOrStyle("value");
        String tooltip = node.getAttributeOrStyle("tooltip");
        String colorStr = node.getAttributeOrStyle("color");
        int color = parseColor(colorStr, 0xFFFFFF);
        int inputWidth = parsePosition(node.getAttributeOrStyle("width"), this.width, 0);
        int inputHeight = parsePosition(node.getAttributeOrStyle("height"), this.height, 0);

        int[] translate = parseTranslate(node, inputWidth, inputHeight, "0", "0");

        int inputX = this.left + parsePosition(node.getAttributeOrStyle("left"), this.width, 0) + translate[0];
        int inputY = this.top + parsePosition(node.getAttributeOrStyle("top"), this.height, 0) + translate[1];

        String type = node.getAttributeOrStyle("type");
        if ("multiLine".equals(type)) {
            MultiLineEditBox multiLineEditBox = new MultiLineEditBox(
                this.font, 
                inputX, inputY, 
                inputWidth, inputHeight, 
                Component.literal(placeholder).setStyle(Style.EMPTY.withColor(color)),
                Component.literal("").setStyle(Style.EMPTY.withColor(color))
            );
            if (defaultValue != null && !defaultValue.isEmpty()) {
                multiLineEditBox.setValue(defaultValue);;
            }
            if (tooltip != null && !tooltip.isEmpty()) {
                multiLineEditBox.setTooltip(Tooltip.create(Component.literal(tooltip)));
            }
            widgets.add(multiLineEditBox);
        } else {
            EditBox editBox = new EditBox(
                this.font, 
                inputX, inputY, 
                inputWidth, inputHeight, 
                Component.literal(defaultValue).setStyle(Style.EMPTY.withColor(color))
            );
            if (defaultValue != null && !defaultValue.isEmpty()) {
                editBox.setValue(defaultValue);
                editBox.setTextColor(color);
            }
            if (placeholder != null && !placeholder.isEmpty()) {
                editBox.setHint(Component.literal(placeholder));
            }
            if (tooltip != null && !tooltip.isEmpty()) {
                editBox.setTooltip(Tooltip.create(Component.literal(tooltip)));
            }
            widgets.add(editBox);
        }
    }

    // 添加按钮组件
    private void createButtonWidget(MCMLNode node) {
        String text = node.getTextContent();
        if (text.isEmpty()) text = node.getAttributeOrStyle("text");
        String backgroundImage = node.getAttributeOrStyle("backgroundImage");
        String tooltip = node.getAttributeOrStyle("tooltip");
        String onClick = node.getAttributeOrStyle("onclick");
        String colorStr = node.getAttributeOrStyle("color");
        int color = parseColor(colorStr, 0xFFFFFF);
        int buttonWidth = parsePosition(node.getAttributeOrStyle("width"), this.width, 0);
        int buttonHeight = parsePosition(node.getAttributeOrStyle("height"), this.height, 0);
        
        int[] translate = parseTranslate(node, buttonWidth, buttonHeight, "0", "0");

        int buttonX = this.left + parsePosition(node.getAttributeOrStyle("left"), this.width, 0)+translate[0];
        int buttonY = this.top + parsePosition(node.getAttributeOrStyle("top"), this.height, 0)+translate[1];
        
        if (backgroundImage != null && !backgroundImage.isEmpty()) {
            try {
                
                String imageType = node.getAttributeOrStyle("imageType");
                int yDiffTex = 0; // 默认 full 模式
                int textureWidth = buttonWidth;
                int textureHeight = buttonHeight;

                if ("halfFull".equals(imageType)) {
                    // halfFull 模式：悬停时显示图片下半部分
                    yDiffTex = buttonHeight; // 悬停时Y坐标偏移一个按钮高度
                    textureHeight = buttonHeight * 2; // 图片高度是按钮高度的两倍
                }

                if (isHttpUrl(backgroundImage)) {
                    // 将变量声明为final，以便在内部类中使用
                    final int finalYDiffTex = yDiffTex;
                    final int finalTextureWidth = textureWidth;
                    final int finalTextureHeight = textureHeight;
                    ResourceLocation placeholder = ResourceLocation.fromNamespaceAndPath(UIJS.MODID, "textures/img/loading.gif");
                    ImageButton placeholderWidget = new ImageButton(
                        buttonX, buttonY, 
                        buttonWidth, buttonHeight, 
                        0,0, yDiffTex,
                        placeholder, textureWidth, textureHeight,
                        btn -> {
                            handleButtonClick(onClick);
                        }, 
                        Component.literal("")
                    );
                    widgets.add(placeholderWidget);
                    final AbstractWidget placeholderRef = placeholderWidget;

                    // 网络图片 - 异步加载
                    NetworkImageLoader.loadImage(backgroundImage)
                    .thenAccept(location -> {
                        Minecraft.getInstance().execute(() -> {
                            if (location != null) {
                                widgets.remove(placeholderRef);
                                ImageButton imageWidget = new ImageButton(
                                    buttonX, buttonY, 
                                    buttonWidth, buttonHeight, 
                                    0,0, finalYDiffTex,
                                    location, finalTextureWidth, finalTextureHeight,
                                    btn -> {
                                        handleButtonClick(onClick);
                                    }, 
                                    Component.literal("")
                                );
                                widgets.add(imageWidget);
                                // 重新添加所有组件到屏幕以确保新组件被渲染
                                refreshWidgets();
                            } else {
                                UIJS.LOGGER.error("Failed to load network image: {}", backgroundImage);
                            }
                        });
                    }).exceptionally(throwable -> {
                        Minecraft.getInstance().execute(() -> {
                            UIJS.LOGGER.error("Error loading network image: {}", backgroundImage, throwable);
                        });
                        return null;
                    });
                } else {
                    ResourceLocation texture = ResourceLocation.parse(backgroundImage);
                    ImageButton imageButton = new ImageButton(
                        buttonX, buttonY, 
                        buttonWidth, buttonHeight, 
                        0, 0, yDiffTex, 
                        texture, textureWidth, textureHeight,
                        btn -> {
                            handleButtonClick(onClick);
                        }, 
                        Component.literal("")
                    );
                    if (tooltip != null && !tooltip.isEmpty()) imageButton.setTooltip(Tooltip.create(Component.literal(tooltip)));
                    widgets.add(imageButton);
                }
                
            } catch (Exception e) {
                UIJS.LOGGER.error("Failed to create button background image: {}", backgroundImage, e);
            }
        } else {
            Button button = Button.builder(
                Component.literal(text).setStyle(Style.EMPTY.withColor(color)), 
                btn -> {
                    handleButtonClick(onClick);
                }
            ).bounds(buttonX, buttonY, buttonWidth, buttonHeight).build();

            if (tooltip != null && !tooltip.isEmpty()) button.setTooltip(Tooltip.create(Component.literal(tooltip)));
            widgets.add(button);
        }
    }

    // 添加图片组件
    private void createImageWidget(MCMLNode node) {
        String imagePath = node.getAttributeOrStyle("src");
        int imageWidth = parsePosition(node.getAttributeOrStyle("width"), this.width, 0);
        int imageHeight = parsePosition(node.getAttributeOrStyle("height"), this.height, 0);
        
        int[] translate = parseTranslate(node, imageWidth, imageHeight, "0", "0");

        int imageX = this.left + parsePosition(node.getAttributeOrStyle("left"), this.width, 0)+translate[0];
        int imageY = this.top + parsePosition(node.getAttributeOrStyle("top"), this.height, 0)+translate[1];

        if (imagePath != null && !imagePath.isEmpty()) {
            try {
                if (isHttpUrl(imagePath)) {

                    // 先添加一个占位符图片
                    ResourceLocation placeholder = ResourceLocation.fromNamespaceAndPath(UIJS.MODID, "textures/img/loading.gif");
                    ImageWidget placeholderWidget = new ImageWidget(imageX, imageY, imageWidth, imageHeight, placeholder);
                    widgets.add(placeholderWidget);
                    
                    // 记录占位符的引用，以便后续移除
                    final AbstractWidget placeholderRef = placeholderWidget;

                    // 网络图片 - 异步加载
                    NetworkImageLoader.loadImage(imagePath)
                    .thenAccept(location -> {
                        Minecraft.getInstance().execute(() -> {
                            if (location != null) {
                                widgets.remove(placeholderRef);
                                ImageWidget imageWidget = new ImageWidget(imageX, imageY, imageWidth, imageHeight, location);
                                widgets.add(imageWidget);
                                // 重新添加所有组件到屏幕以确保新组件被渲染
                                refreshWidgets();
                            } else {
                                UIJS.LOGGER.error("Failed to load network image: {}", imagePath);
                            }
                        });
                    }).exceptionally(throwable -> {
                        Minecraft.getInstance().execute(() -> {
                            UIJS.LOGGER.error("Error loading network image: {}", imagePath, throwable);
                        });
                        return null;
                    });
                } else {
                    ResourceLocation texture = ResourceLocation.parse(imagePath);
                    ImageWidget imageWidget = new ImageWidget(imageX, imageY, imageWidth, imageHeight, texture);
                    widgets.add(imageWidget);
                }
                
                
                
            } catch (Exception e) {
                UIJS.LOGGER.error("Failed to create image widget: {}", imagePath, e);
            }
        }
    }
    
    // 添加复选框组件
    private void createCheckboxWidget(MCMLNode node) {
        String text = node.getAttributeOrStyle("text");
        String colorStr = node.getAttributeOrStyle("color");
        int color = parseColor(colorStr, 0xFFFFFF);

        int checkboxWidth = 20;
        int checkboxHeight = 20;
        int[] translate = parseTranslate(node, checkboxWidth, checkboxHeight, "0", "0");
        int checkboxX = this.left + parsePosition(node.getAttributeOrStyle("left"), this.width, 0)+translate[0];
        int checkboxY = this.top + parsePosition(node.getAttributeOrStyle("top"), this.height, 0)+translate[1];
        
        boolean checked = node.getAttributeOrStyle("checked") != null && node.getAttributeOrStyle("checked").equals("true");
        boolean showText = node.getAttributeOrStyle("showText") != null && node.getAttributeOrStyle("showText").equals("true");

        Checkbox checkbox = new Checkbox(
            checkboxX, checkboxY, 
            20, 20, 
            Component.literal(text).setStyle(Style.EMPTY.withColor(color)), 
            checked,
            showText
        );
        widgets.add(checkbox);
    }
    
    // 添加循环按钮组件
    private void createCycleButtonWidget(MCMLNode node) {
        String text = node.getTextContent();
        CycleButtonConfig config = new CycleButtonConfig(text);
        String defaultValue = node.getAttributeOrStyle("defaultValue");
        String name = node.getAttributeOrStyle("name");
        if (name == null) name = "";
        String showName = node.getAttributeOrStyle("showName");
        String tooltip = node.getAttributeOrStyle("tooltip");
        String onClick = node.getAttributeOrStyle("onclick");
        String colorStr = node.getAttributeOrStyle("color");
        int color = parseColor(colorStr, 0xFFFFFF);
        int buttonWidth = parsePosition(node.getAttributeOrStyle("width"), this.width, 0);
        int buttonHeight = parsePosition(node.getAttributeOrStyle("height"), this.height, 0);
        
        int[] translate = parseTranslate(node, buttonWidth, buttonHeight, "0", "0");

        int buttonX = this.left + parsePosition(node.getAttributeOrStyle("left"), this.width, 0)+translate[0];
        int buttonY = this.top + parsePosition(node.getAttributeOrStyle("top"), this.height, 0)+translate[1];

        CycleButton.Builder<String> ButtonBuilder = CycleButton.<String>builder(theme -> {
            return Component.literal(theme).setStyle(Style.EMPTY.withColor(color));
        })
        .withValues(
            config.getDefaultList()
        );

        if (defaultValue != null && !defaultValue.isEmpty()) {
            ButtonBuilder.withInitialValue(defaultValue);
        } else {
            ButtonBuilder.withInitialValue(config.getDefaultList().get(0));
        }
        if (showName == null || showName.isEmpty() || showName.equals("false")){
            ButtonBuilder.displayOnlyValue();
        }
        if (config.hasList("ctrl")) {
            ButtonBuilder.withValues(
                Screen::hasControlDown,
                config.getDefaultList(),
                config.getList("ctrl")
            );
        }
        if (config.hasList("shift")) {
            ButtonBuilder.withValues(
                Screen::hasShiftDown,
                config.getDefaultList(),
                config.getList("shift")
            );
        }
        if (config.hasList("alt")) {
            ButtonBuilder.withValues(
                Screen::hasAltDown,
                config.getDefaultList(),
                config.getList("alt")
            );
        }
        CycleButton<String> themeButton = ButtonBuilder
        .create(
            buttonX, buttonY, 
            buttonWidth, buttonHeight, 
            Component.literal(name).setStyle(Style.EMPTY.withColor(color)), 
            (button, theme) -> {
            // System.out.println("主题切换为: " + theme);
            handleButtonClick(onClick);
        });
        if (tooltip != null && !tooltip.isEmpty()) {
            themeButton.setTooltip(Tooltip.create(Component.literal(tooltip)));
        }
        widgets.add(themeButton);
    }
    
    // 添加插槽组件
    private void createSoltWidget(MCMLNode node) {
        String orderStr = node.getAttributeOrStyle("order");
        String bindGroup = node.getAttributeOrStyle("bindGroup");
        String tooltip = node.getAttributeOrStyle("tooltip");
        
        int soltWidth = parsePosition(node.getAttributeOrStyle("width"), this.width, 18); // 默认18px
        int soltHeight = parsePosition(node.getAttributeOrStyle("height"), this.height, 18);
        
        int[] translate = parseTranslate(node, soltWidth, soltHeight, "0", "0");
        int soltX = this.left + parsePosition(node.getAttributeOrStyle("left"), this.width, 0) + translate[0];
        int soltY = this.top + parsePosition(node.getAttributeOrStyle("top"), this.height, 0) + translate[1];
        
        // 创建物品槽组件
        SoltWidget soltWidget = new SoltWidget(
            soltX, soltY, 
            soltWidth, soltHeight,
            orderStr != null ? Integer.parseInt(orderStr) : 0,
            bindGroup != null ? bindGroup : "",
            node
        );

        if (tooltip != null && !tooltip.isEmpty()) {
            soltWidget.setTooltip(Tooltip.create(Component.literal(tooltip)));
        }
        
        widgets.add(soltWidget);
    }
    
    // 添加物品栏组件
    private void createInventoryWidget(MCMLNode node) {
        String type = node.getAttributeOrStyle("type");
        if (type == null) type = "both";
        
        int slotSize = parsePosition(node.getAttributeOrStyle("size"), this.width, 18);
        
        // 创建物品栏组件
        InventoryWidget inventoryWidget = new InventoryWidget(
            this.left, this.top, 
            this.width, this.height,
            type, slotSize, isItemBound(), 
            node
        );
        
        widgets.add(inventoryWidget);
    }
    //#endregion

    //#region 渲染方法
    // 渲染网络背景
    private void renderNetworkBackground(GuiGraphics guiGraphics, String imageUrl) {
        long currentTime = System.currentTimeMillis();
        
        // 检查是否需要重新加载（每10秒检查一次）
        if (networkBackgroundTexture == null && !isNetworkBackgroundLoading && 
            (currentTime - lastBackgroundCheckTime > 10000)) {
            
            isNetworkBackgroundLoading = true;
            lastBackgroundCheckTime = currentTime;
            
            NetworkImageLoader.loadImage(imageUrl).thenAccept(location -> {
                Minecraft.getInstance().execute(() -> {
                    networkBackgroundTexture = location;
                    isNetworkBackgroundLoading = false;
                    
                    if (location == null) {
                        UIJS.LOGGER.warn("Network background image loaded but location is null");
                    }
                });
            }).exceptionally(throwable -> {
                Minecraft.getInstance().execute(() -> {
                    UIJS.LOGGER.error("Failed to load network background: {}", imageUrl, throwable);
                    isNetworkBackgroundLoading = false;
                });
                return null;
            });
        }
        
        if (networkBackgroundTexture != null) {
            // 渲染网络图片
            try {
                guiGraphics.blit(
                    networkBackgroundTexture,
                    this.left, this.top,
                    0, 0, 
                    this.width, this.height, 
                    this.width, this.height
                );
            } catch (Exception e) {
                UIJS.LOGGER.error("Failed to render network background: {}", imageUrl, e);
                networkBackgroundTexture = null; // 重置，下次重新加载
                renderSolidBackground(guiGraphics);
            }
        } else if (isNetworkBackgroundLoading) {
            // 显示加载中的背景
            renderLoadingBackground(guiGraphics);
        } else {
            // 加载失败或尚未开始加载
            renderSolidBackground(guiGraphics);
        }
    }

    // 渲染本地背景
    private void renderLocalBackground(GuiGraphics guiGraphics, String backgroundImage) {
        try {
            ResourceLocation imageLocation = ResourceLocation.parse(backgroundImage);
            guiGraphics.blit(
                imageLocation,
                this.left, this.top,
                0, 0, 
                this.width, this.height, 
                this.width, this.height
            );
        } catch (Exception e) {
            UIJS.LOGGER.error("Failed to render local background: {}", backgroundImage, e);
            renderSolidBackground(guiGraphics);
        }
    }

    // 渲染纯色背景
    private void renderSolidBackground(GuiGraphics guiGraphics) {
        guiGraphics.fill(
            this.left, this.top,
            this.left + this.width, this.top + this.height,
            parseColor(uiRoot.getAttributeOrStyle("backgroundColor"), 0x80000000)
        );
    }

    // 渲染加载中的背景
    private void renderLoadingBackground(GuiGraphics guiGraphics) {
        // 显示加载中的渐变背景
        int baseColor = parseColor(uiRoot.getAttributeOrStyle("backgroundColor"), 0x80000000);
        
        // 创建加载动画效果
        long time = System.currentTimeMillis() % 2000;
        float progress = (float) time / 2000.0f;
        int alpha = (int) (128 + 127 * Math.sin(progress * Math.PI * 2));
        
        int animatedColor = (alpha << 24) | (baseColor & 0xFFFFFF);
        
        guiGraphics.fill(
            this.left, this.top,
            this.left + this.width, this.top + this.height,
            animatedColor
        );
        
        // 显示加载文字
        String loadingText = "Loading background...";
        int textWidth = this.font.width(loadingText);
        int textX = this.left + (this.width - textWidth) / 2;
        int textY = this.top + (this.height - this.font.lineHeight) / 2;

        guiGraphics.drawString(
            this.font, 
            Component.literal(loadingText), 
            textX, textY, 
            0xFFFFFFFF
        );
    }

    // 渲染自定义节点
    private void renderCustomNodes(GuiGraphics guiGraphics, MCMLNode node, int x, int y) {
        // 渲染节点
        switch (node.getTagName()) {
            case "text":
                renderTextNode(guiGraphics, node);
                break;
            case "button":
                renderImageButtonNode(guiGraphics, node);
                break;
        }
        
        // 递归渲染子节点
        for (MCMLNode child : node.getChildren()) {
            // 使用计算后的样式，如果没有则使用属性
            String leftStr = child.getComputedStyles().get("left");
            if (leftStr == null) leftStr = child.getAttributeOrStyle("left");
            
            String topStr = child.getComputedStyles().get("top");
            if (topStr == null) topStr = child.getAttributeOrStyle("top");
            
            renderCustomNodes(guiGraphics, child, x, y);
        }
    }
    
    // 渲染文本节点
    private void renderTextNode(GuiGraphics guiGraphics, MCMLNode node) {
        String text = node.getTextContent();
        String type = node.getAttributeOrStyle("type");
        String colorStr = node.getAttributeOrStyle("color");
        int color = parseColor(colorStr, 0xFFFFFF);

        int textWidth = this.font.width(text);
        int textHeight = this.font.lineHeight;
        int[] translate = parseTranslate(node, textWidth, textHeight, "0", "0");

        int textX = this.left + parsePosition(node.getAttributeOrStyle("left"), this.width, 0) + translate[0];
        int textY = this.top + parsePosition(node.getAttributeOrStyle("top"), this.height, 0) + translate[1];
        Component textComponent = null;
        if (type == null || type.equals("plain")) {
            textComponent = Component.literal(text);
        } else if (type.equals("translatable")) {
            textComponent = Component.translatable(text);
        }
        if (textComponent != null) {
            guiGraphics.drawString(this.font, textComponent, textX, textY, color);
            
        }
    }

    // 渲染图片按钮节点
    private void renderImageButtonNode(GuiGraphics guiGraphics, MCMLNode node) {
        String text = node.getTextContent();
        if (text.isEmpty()) text = node.getAttributeOrStyle("text");
        String backgroundImage = node.getAttributeOrStyle("backgroundImage");
        String colorStr = node.getAttributeOrStyle("color");
        int color = parseColor(colorStr, 0xFFFFFF);
        int buttonWidth = parsePosition(node.getAttributeOrStyle("width"), this.width, 0);
        int buttonHeight = parsePosition(node.getAttributeOrStyle("height"), this.height, 0);
        
        int[] translate = parseTranslate(node, buttonWidth, buttonHeight, "0", "0");

        int buttonX = this.left + parsePosition(node.getAttributeOrStyle("left"), this.width, 0)+translate[0];
        int buttonY = this.top + parsePosition(node.getAttributeOrStyle("top"), this.height, 0)+translate[1];

        int textWidth = this.font.width(text);
        int textHeight = this.font.lineHeight;
        
        int textX = buttonX + buttonWidth / 2 - textWidth / 2;
        int textY = buttonY + buttonHeight / 2 - textHeight / 2;
        Component textComponent = Component.literal(text).withStyle(Style.EMPTY.withColor(color));
        
        if (backgroundImage != null && !backgroundImage.isEmpty()) {
            guiGraphics.drawString(this.font, textComponent, textX, textY, color);
        }
    }
    //#endregion

    // 处理按钮点击事件
    private void handleButtonClick(String onClick) {
        if (onClick != null && !onClick.isEmpty()) {
            if (onClick.startsWith("openUIById(")) {
                String uiId = onClick.substring("openUIById(".length(), onClick.length() - 1);
                // 关闭当前UI
                Minecraft.getInstance().setScreen(null);
                // 打开指定UI
                UIRenderer.openUI(ResourceLocation.tryParse(uiId));
            } else if (onClick.startsWith("openUIByFilePath(")) {
                String filePath = onClick.substring("openUIByFilePath(".length(), onClick.length() - 1);
                Minecraft.getInstance().setScreen(null);
                openUIByFilePath(filePath);
            }
        }
    }

    // 通过文件路径打开UI的方法
    private void openUIByFilePath(String filePath) {
        try {
            // 从文件路径创建 ResourceLocation
            // 假设文件路径是相对于 MCML 目录的
            String normalizedPath = filePath
                .replace("\\", "/")  // 统一使用正斜杠
                .replace(".mcml", "") // 移除扩展名
                .toLowerCase();
            
            // 如果路径以斜杠开头，移除它
            if (normalizedPath.startsWith("/")) {
                normalizedPath = normalizedPath.substring(1);
            }
            
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath("mcml", normalizedPath);
            
            // 检查UI是否已注册
            if (UIRegistry.hasUI(location)) {
                UIRenderer.openUI(location);
            } else {
                // 如果UI未注册，尝试从文件直接注册并打开
                // boolean registered = 
                registerAndOpenUIFromFile(filePath, location);

            }
        } catch (Exception e) {
            UIJS.LOGGER.error("Error opening UI from path: {}", filePath, e);
        }
    }

    //#region 工具方法
    private int parsePosition(String position, int totalSize, int defaultValue) {
        if (position == null) return defaultValue;
        
        if (position.endsWith("%")) {
            // 百分比
            double percent = Double.parseDouble(position.substring(0, position.length() - 1)) / 100.0;
            return (int) (totalSize * percent);
        } else if (position.endsWith("px")) {
            // 像素值
            return Integer.parseInt(position.substring(0, position.length() - 2));
        } else {
            // 默认像素值
            try {
                return Integer.parseInt(position);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
    }

    private int[] parseTranslate(MCMLNode node, int elWidth, int elHeight, int OffsetX, int OffsetY) {
        String translateX = null;
        String translateY = null;
        Pattern pattern = Pattern.compile("(-?\\d+(?:\\.\\d+)?(?:px|%)?)\\s*,\\s*(-?\\d+(?:\\.\\d+)?(?:px|%)?)");
        if (node.getAttributeOrStyle("translate") != null){
            Matcher matcher = pattern.matcher(node.getAttributeOrStyle("translate"));
            if (matcher.find()) {
                translateX = matcher.group(1);
                translateY = matcher.group(2);
            }
        }
        return new int[]{
            parsePosition(translateX, elWidth, OffsetX),
            parsePosition(translateY, elHeight, OffsetY)
        };
    }

    private int[] parseTranslate(MCMLNode node, int elWidth, int elHeight, String defaultOffsetX, String defaultOffsetY) {
        int OffsetX = parsePosition(defaultOffsetX, elWidth, 0);
        int OffsetY = parsePosition(defaultOffsetY, elHeight, 0);
        return parseTranslate(node, elWidth, elHeight, OffsetX, OffsetY);
    }

    private int parseColor(String colorStr, int defaultColor) {
        if (colorStr == null) return defaultColor;
        
        try {
            if (colorStr.startsWith("#")) {
                // 十六进制颜色
                return (int) Long.parseLong(formatHex(colorStr), 16);
            } else if (colorStr.startsWith("rgb(")) {
                // RGB颜色
                String[] rgb = colorStr.substring(4, colorStr.length() - 1).split(",");
                int r = Integer.parseInt(rgb[0].trim());
                int g = Integer.parseInt(rgb[1].trim());
                int b = Integer.parseInt(rgb[2].trim());
                return (r << 16) | (g << 8) | b;
            } else if (colorStr.startsWith("rgba(")) {
                // RGBA颜色
                String[] rgba = colorStr.substring(5, colorStr.length() - 1).split(",");
                int r = Integer.parseInt(rgba[0].trim());
                int g = Integer.parseInt(rgba[1].trim());
                int b = Integer.parseInt(rgba[2].trim());
                float a = Float.parseFloat(rgba[3].trim());
                return (int) ((r << 16) | (g << 8) | b | ((int)(a * 255) << 24));
            }
        } catch (Exception e) {
            UIJS.LOGGER.warn("Invalid color format: {}", colorStr);
        }
        
        return defaultColor;
    }

    // hex格式化
    private String formatHex(String colorStr) {
        String hex = colorStr.substring(1);
        if (hex.length() == 3) {
            hex = "" + hex.charAt(0) + hex.charAt(0) + 
                hex.charAt(1) + hex.charAt(1) + 
                hex.charAt(2) + hex.charAt(2);
        } else if (hex.length() == 6) {
            hex = "FF" + hex; // 添加不透明度
        } else if (hex.length() == 4) {
            // RGBA to ARGB
            hex = "" + hex.charAt(3)  + hex.charAt(3) + 
                hex.charAt(0) + hex.charAt(0) + 
                hex.charAt(1) + hex.charAt(1) + 
                hex.charAt(2) + hex.charAt(2);
        } else if (hex.length() == 8) {
            // RRGGBBAA to AARRGGGBB
            hex = "" + hex.charAt(6) + hex.charAt(7) + 
                hex.charAt(0) + hex.charAt(1) + 
                hex.charAt(2) + hex.charAt(3) + 
                hex.charAt(4) + hex.charAt(5);
        }
        return hex;
    }

    // 从文件注册并打开UI
    private boolean registerAndOpenUIFromFile(String filePath, ResourceLocation location) {
        try {
            // 构建完整的文件路径
            Path fullPath = Path.of(filePath);
            if (!Files.exists(fullPath)) {
                // 如果文件不存在，尝试在 MCML 目录下查找
                String mcmlDirectory = Config.INSTANCE.mcmlDirectory.get();
                fullPath = Path.of(mcmlDirectory, filePath);
            }
            
            // 注册并打开UI
            if (!Files.exists(fullPath)) UIJS.LOGGER.error("MCML file not found: {}", fullPath.toAbsolutePath());
            
            boolean success = UIRegistry.registerUI(location, fullPath);
            if (!success)UIJS.LOGGER.error("Failed to register UI from file: {}", fullPath);
            UIRenderer.openUI(location);
            return true;

        } catch (Exception e) {
            UIJS.LOGGER.error("Error registering UI from file: {}", filePath, e);
        }
        return false;
    }

    // 判断是否为 HTTP URL
    private boolean isHttpUrl(String url) {
        return url != null && (url.startsWith("http://") || url.startsWith("https://"));
    }

    // 刷新组件列表
    private void refreshWidgets() {
        // 清除当前所有可渲染组件
        this.clearWidgets();
        
        // 重新添加所有widget到屏幕
        for (AbstractWidget widget : widgets) {
            this.addRenderableWidget(widget);
        }
    }

    // 根据uiId判断是否绑定了物品
    boolean isItemBound() {
        for (Map.Entry<ResourceLocation, ResourceLocation> entry : UIRegistry.ITEM_UI_BINDINGS.entrySet()) {
            if (entry.getValue().equals(uiId)) {
                return true;
            }
        }
        return false;
    }
    //#endregion
}
