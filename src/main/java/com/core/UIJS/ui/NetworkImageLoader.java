package com.core.UIJS.ui;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.core.UIJS.UIJS;
import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

public class NetworkImageLoader {
private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);
    private static final Map<String, ResourceLocation> CACHE = new HashMap<>();
    private static final Map<String, Long> CACHE_TIMESTAMPS = new HashMap<>();
    private static final long CACHE_DURATION = 30 * 60 * 1000; // 30分钟缓存
    
    public static CompletableFuture<ResourceLocation> loadImage(String imageUrl) {
        // 检查缓存
        if (CACHE.containsKey(imageUrl)) {
            long timestamp = CACHE_TIMESTAMPS.getOrDefault(imageUrl, 0L);
            if (System.currentTimeMillis() - timestamp < CACHE_DURATION) {
                return CompletableFuture.completedFuture(CACHE.get(imageUrl));
            } else {
                // 缓存过期，移除
                CACHE.remove(imageUrl);
                CACHE_TIMESTAMPS.remove(imageUrl);
            }
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                
                // 使用自定义的HTTP下载方法，避免HttpTexture的问题
                byte[] imageData = downloadImageData(imageUrl);
                if (imageData == null) {
                    UIJS.LOGGER.error("Failed to download image data from: {}", imageUrl);
                    return null;
                }
                
                // 将图像数据转换为NativeImage
                NativeImage nativeImage = loadNativeImageDirectly(imageData);
                if (nativeImage == null) {
                    UIJS.LOGGER.error("Failed to create NativeImage from downloaded data");
                    return null;
                }
                
                // 创建资源位置
                String hash = Integer.toHexString(imageUrl.hashCode());
                ResourceLocation location = ResourceLocation.fromNamespaceAndPath("network", "image_" + hash);
                
                // 创建动态纹理并注册
                Minecraft.getInstance().execute(() -> {
                    try {
                        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
                        DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);
                        textureManager.register(location, dynamicTexture);
                        
                        // 缓存结果
                        CACHE.put(imageUrl, location);
                        CACHE_TIMESTAMPS.put(imageUrl, System.currentTimeMillis());
                    } catch (Exception e) {
                        UIJS.LOGGER.error("Failed to register texture for: {}", imageUrl, e);
                    }
                });
                
                return location;
            } catch (Exception e) {
                UIJS.LOGGER.error("Failed to load network image: {}", imageUrl, e);
                return null;
            }
        }, EXECUTOR);
    }
    
    private static byte[] downloadImageData(String imageUrl) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(imageUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000); // 10秒连接超时
            connection.setReadTimeout(30000);    // 30秒读取超时
            connection.setRequestProperty("User-Agent", "Minecraft-UIJS/1.0");
            
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                UIJS.LOGGER.error("HTTP error {} when downloading: {}", responseCode, imageUrl);
                return null;
            }
            
            // 读取图像数据
            try (InputStream inputStream = connection.getInputStream();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                
                return outputStream.toByteArray();
            }
        } catch (Exception e) {
            UIJS.LOGGER.error("Error downloading image data from: {}", imageUrl, e);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    // 直接使用NativeImage的读取方法
    private static NativeImage loadNativeImageDirectly(byte[] imageData) {
        try {
            return NativeImage.read(new ByteArrayInputStream(imageData));
        } catch (Exception e) {
            UIJS.LOGGER.error("Error loading NativeImage directly from bytes", e);
            return null;
        }
    }
    
    public static void clearCache() {
        // 在主线程中释放纹理资源
        Minecraft.getInstance().execute(() -> {
            TextureManager textureManager = Minecraft.getInstance().getTextureManager();
            for (ResourceLocation location : CACHE.values()) {
                try {
                    textureManager.release(location);
                } catch (Exception e) {
                    UIJS.LOGGER.warn("Error releasing texture: {}", location, e);
                }
            }
            CACHE.clear();
            CACHE_TIMESTAMPS.clear();
        });
    }
    
    public static void cleanupExpiredCache() {
        long currentTime = System.currentTimeMillis();
        CACHE_TIMESTAMPS.entrySet().removeIf(entry -> {
            if (currentTime - entry.getValue() > CACHE_DURATION) {
                ResourceLocation location = CACHE.get(entry.getKey());
                if (location != null) {
                    try {
                        Minecraft.getInstance().getTextureManager().release(location);
                    } catch (Exception e) {
                        UIJS.LOGGER.warn("Error releasing expired texture: {}", location, e);
                    }
                    CACHE.remove(entry.getKey());
                }
                return true;
            }
            return false;
        });
    }
}
