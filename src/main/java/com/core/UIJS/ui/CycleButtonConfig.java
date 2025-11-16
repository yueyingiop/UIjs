package com.core.UIJS.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.core.UIJS.UIJS;

/**
 * CycleButton 配置存储类
 * 使用 Map<String, List<String>> allLists 存储所有列表
 */
public class CycleButtonConfig {
    private final Map<String, List<String>> allLists;
    
    //#region 构造函数
    /**
     * 构造函数
     * @param configText MCML 配置文本
     */
    public CycleButtonConfig(String configText) {
        this.allLists = new HashMap<>();
        parseConfig(configText);
    }

    /**
     * 内部解析函数
     * 将 MCML 配置文本解析对应的键值对
     * @param configText MCML 配置文本
     */
    private void parseConfig(String configText) {
        if (configText == null || configText.trim().isEmpty()) {
            // 如果没有配置，创建一个空的默认列表
            allLists.put("default", new ArrayList<>());
            return;
        }
        
        try {
            // 使用正则表达式匹配键值对
            Pattern pattern = Pattern.compile("(\\w+)=\\s*\\[([^\\]]*)\\]");
            Matcher matcher = pattern.matcher(configText);
            
            while (matcher.find()) {
                String key = matcher.group(1);
                String valueStr = matcher.group(2);
                // 解析数组值
                List<String> values = parseArray(valueStr);
                allLists.put(key, values);
            }
            
            // 确保默认列表存在
            if (!allLists.containsKey("default")) {
                allLists.put("default", new ArrayList<>());
            }
            
        } catch (Exception e) {
            UIJS.LOGGER.error("Failed to parse CycleButton configuration: {}", configText, e);
            // 解析失败时至少确保有默认列表
            if (!allLists.containsKey("default")) {
                allLists.put("default", new ArrayList<>());
            }
        }
    }

    /**
     * 解析数组字符串
     * @param arrayStr 数组字符串，如 "aa", "bb", "cc" 或 ["aa", "bb", "cc"]
     * @return 字符串列表
     */
    private List<String> parseArray(String arrayStr) {
        List<String> result = new ArrayList<>();
        
        if (arrayStr == null || arrayStr.trim().isEmpty()) {
            return result;
        }
        
        // 清理字符串：移除方括号、引号和多余空格
        String cleaned = arrayStr.trim();
        
        // 如果以方括号开始和结束，移除它们
        if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }
        
        // 按逗号分割，并处理每个元素
        if (!cleaned.isEmpty()) {
            // 使用正则表达式分割
            String[] items = cleaned.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            
            for (String item : items) {
                String trimmedItem = item.trim();
                
                // 移除引号（如果存在）
                if (trimmedItem.startsWith("\"") && trimmedItem.endsWith("\"")) {
                    trimmedItem = trimmedItem.substring(1, trimmedItem.length() - 1);
                }
                
                if (!trimmedItem.isEmpty()) {
                    result.add(trimmedItem);
                }
            }
        }
        
        return result;
    }

    //#endregion

    // 获取所有列表的Map
    public Map<String, List<String>> getAllLists() {
        return Collections.unmodifiableMap(allLists);
    }
    
    // 获取特定列表
    public List<String> getList(String key) {
        return allLists.getOrDefault(key, Collections.emptyList());
    }

    // 获取默认列表
    public List<String> getDefaultList() {
        return getList("default");
    }

    // 检查列表是否存在且不为空
    public boolean hasList(String key) {
        List<String> list = allLists.get(key);
        return list != null && !list.isEmpty();
    }

    /**
     * 获取所有可用的列表键
     * @return 所有列表的键集合
     */
    public Set<String> getAvailableListKeys() {
        Set<String> keys = new HashSet<>();
        for (Map.Entry<String, List<String>> entry : allLists.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                keys.add(entry.getKey());
            }
        }
        return keys;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CycleButtonConfig{");
        for (Map.Entry<String, List<String>> entry : allLists.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append(", ");
        }
        if (!allLists.isEmpty()) {
            sb.setLength(sb.length() - 2); // 移除最后的逗号和空格
        }
        sb.append("}");
        return sb.toString();
    }
}
