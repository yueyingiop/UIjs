package com.core.UIJS.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.core.UIJS.UIJS;
import com.core.UIJS.mcml.MCMLNode;
import com.core.UIJS.ui.NetworkImageLoader;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class UIRenderHelper {
    // 网络背景图片相关字段
    private static ResourceLocation networkBackgroundTexture = null;
    private static boolean isNetworkBackgroundLoading = false;
    private static long lastBackgroundCheckTime = 0;


    //#region 绘制函数
    /**
     * 渲染物品在插槽中
     */
    public static void renderItemInSlot(
        GuiGraphics guiGraphics, 
        ItemStack itemStack, 
        int slotX, int slotY, 
        int slotWidth, int slotHeight
    ) {
        if (!itemStack.isEmpty()) {
            // 计算物品在插槽中的居中位置
            int itemX = slotX + (slotWidth - 16) / 2;  // 物品默认宽度为16px
            int itemY = slotY + (slotHeight - 16) / 2; // 物品默认高度为16px
            
            // 如果插槽太小，至少保证物品可见
            itemX = Math.max(slotX + 1, itemX);
            itemY = Math.max(slotY + 1, itemY);
            
            guiGraphics.renderItem(itemStack, itemX, itemY);
            guiGraphics.renderItemDecorations(net.minecraft.client.Minecraft.getInstance().font, itemStack, itemX, itemY);
        }
    }

    /**
     * 渲染鼠标悬停效果
    */
    public static void renderHover(
        GuiGraphics guiGraphics, 
        int x, int y, 
        int slotWidth, int slotHeight, 
        int mouseX, int mouseY, 
        int HoverCoverColor, 
        int HoverBorderColor
    ) {
        if (
            ItemInteractionHelper.isMouseOverSlot(mouseX, mouseY, x, y, slotWidth, slotHeight)
        ) {
            // 覆盖层
            guiGraphics.fill(
                x, y,
                x + slotWidth, y + slotHeight,
                HoverCoverColor
            );
            // 边框
            guiGraphics.renderOutline(
                x, y,
                slotWidth, slotHeight,
                HoverBorderColor
            );
        }
    }
    public static void renderHover(
        GuiGraphics guiGraphics, 
        int x, int y, 
        int soltSize, 
        int mouseX, int mouseY, 
        int HoverCoverColor, 
        int HoverBorderColor
    ){
        renderHover(guiGraphics, x, y, soltSize, soltSize, mouseX, mouseY, HoverCoverColor, HoverBorderColor);
    }
    
    /**
     * 渲染网络背景
    */
    public static void renderNetworkBackground(GuiGraphics guiGraphics, MCMLNode node, String imageUrl, int left, int top, int width, int height, Runnable renderSolidBackground) {
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
                    left, top,
                    0, 0, 
                    width, height, 
                    width, height
                );
            } catch (Exception e) {
                UIJS.LOGGER.error("Failed to render network background: {}", imageUrl, e);
                networkBackgroundTexture = null; // 重置，下次重新加载
                renderSolidBackground.run();
            }
        } else if (isNetworkBackgroundLoading) {
            // 显示加载中的背景
            renderLoadingBackground(guiGraphics, node, left, top, width, height);
        } else {
            // 加载失败或尚未开始加载
            renderSolidBackground.run();
        }
    }

    /**
     * 渲染本地背景
    */
    public static void renderLocalBackground(GuiGraphics guiGraphics, String backgroundImage, int left, int top, int width, int height, Runnable renderSolidBackground) {
        try {
            ResourceLocation imageLocation = ResourceLocation.parse(backgroundImage);
            guiGraphics.blit(
                imageLocation,
                left, top,
                0, 0, 
                width, height, 
                width, height
            );
        } catch (Exception e) {
            UIJS.LOGGER.error("Failed to render local background: {}", backgroundImage, e);
            renderSolidBackground.run();
        }
    }

    /**
     * 渲染加载中的背景
    */
    public static void renderLoadingBackground(GuiGraphics guiGraphics, MCMLNode node, int left, int top, int width, int height) {
        // 显示加载中的渐变背景
        int baseColor = parseColor(node.getAttributeOrStyle("backgroundColor"), 0x80000000);
        
        // 创建加载动画效果
        long time = System.currentTimeMillis() % 2000;
        float progress = (float) time / 2000.0f;
        int alpha = (int) (128 + 127 * Math.sin(progress * Math.PI * 2));
        
        int animatedColor = (alpha << 24) | (baseColor & 0xFFFFFF);
        
        guiGraphics.fill(
            left, top,
            left + width, top + height,
            animatedColor
        );
        
        // 显示加载文字
        String loadingText = "Loading background...";
        int textWidth = Minecraft.getInstance().font.width(loadingText);
        int textX = left + (width - textWidth) / 2;
        int textY = top + (height - Minecraft.getInstance().font.lineHeight) / 2;

        guiGraphics.drawString(
            Minecraft.getInstance().font, 
            Component.literal(loadingText), 
            textX, textY, 
            0xFFFFFFFF
        );
    }
    //#endregion

    //#region 计算/解析函数
    public static int parsePosition(String position, int totalSize, int defaultValue) {
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

    public static int[] parseTranslate(MCMLNode node, int elWidth, int elHeight, int OffsetX, int OffsetY) {
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

    public static int[] parseTranslate(MCMLNode node, int elWidth, int elHeight, String defaultOffsetX, String defaultOffsetY) {
        int OffsetX = parsePosition(defaultOffsetX, elWidth, 0);
        int OffsetY = parsePosition(defaultOffsetY, elHeight, 0);
        return parseTranslate(node, elWidth, elHeight, OffsetX, OffsetY);
    }

    public static int parseColor(String colorStr, int defaultColor) {
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
    public static String formatHex(String colorStr) {
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
    //#endregion

    // 判断是否为 HTTP URL
    public static boolean isHttpUrl(String url) {
        return url != null && (url.startsWith("http://") || url.startsWith("https://"));
    }
}
