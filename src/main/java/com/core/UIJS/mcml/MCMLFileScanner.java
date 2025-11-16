package com.core.UIJS.mcml;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.core.UIJS.Config;
import com.core.UIJS.ui.UIRegistry;

import net.minecraft.resources.ResourceLocation;

// MCML文件扫描器
public class MCMLFileScanner {
    private static final Logger LOGGER = LogManager.getLogger("UIJS-FileScanner");
    
    public static void scanAndRegisterMCMLFiles() {
        try {
            String mcmlDirectory = Config.INSTANCE.mcmlDirectory.get();
            Path mcmlPath = Paths.get(mcmlDirectory);
            
            if (!Files.exists(mcmlPath)) {
                LOGGER.info("MCML directory does not exist, creating: {}", mcmlPath.toAbsolutePath());
                Files.createDirectories(mcmlPath);
                return;
            }
            
            List<Path> mcmlFiles = findMCMLFiles(mcmlPath);
            LOGGER.info("Found {} MCML files in directory: {}", mcmlFiles.size(), mcmlPath.toAbsolutePath());
            
            for (Path file : mcmlFiles) {
                registerMCMLFile(file, mcmlPath);
            }
            
        } catch (Exception e) {
            LOGGER.error("Error scanning MCML files", e);
        }
    }
    
    private static List<Path> findMCMLFiles(Path directory) throws IOException {
        List<Path> mcmlFiles = new ArrayList<>();
        
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.toString().toLowerCase().endsWith(".mcml")) {
                    mcmlFiles.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                LOGGER.warn("Failed to visit file: {}", file, exc);
                return FileVisitResult.CONTINUE;
            }
        });
        
        return mcmlFiles;
    }
    
    private static void registerMCMLFile(Path file, Path baseDirectory) {
        try {
            // 生成UI ID：使用相对于基础目录的路径作为ID
            String relativePath = baseDirectory.relativize(file).toString();
            String uiId = relativePath
                .replace("\\", "/")  // 统一使用正斜杠
                .replace(".mcml", "") // 移除扩展名
                .toLowerCase();
            
            // 如果路径以斜杠开头，移除它
            if (uiId.startsWith("/")) {
                uiId = uiId.substring(1);
            }
            
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath("mcml", uiId);
            
            // 注册UI
            boolean success = UIRegistry.registerUI(location, file);
            if (success) {
                LOGGER.info("Auto-registered MCML UI: {} from file: {}", location, file.getFileName());
            } else {
                LOGGER.error("Failed to auto-register MCML UI: {} from file: {}", location, file.getFileName());
            }
            
        } catch (Exception e) {
            LOGGER.error("Error registering MCML file: {}", file, e);
        }
    }
}
