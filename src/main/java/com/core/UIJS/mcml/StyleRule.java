package com.core.UIJS.mcml;

import java.util.HashMap;
import java.util.Map;

// 样式规则
public class StyleRule {
    private final String selector; // 标签选择器
    private final Map<String, String> properties; // 样式属性键值对

    public StyleRule(String selector) {
        this.selector = selector;
        this.properties = new HashMap<>();
    }

    public void setProperty(String key, String value) {
        properties.put(key, value);
    }
    
    public String getProperty(String key) {
        return properties.get(key);
    }
    
    public String getSelector() {
        return selector;
    }
    
    public Map<String, String> getProperties() {
        return properties;
    }
    
    public boolean matches(String elementId, String elementClass) {
        if (selector.startsWith("#")) {
            // ID选择器
            return selector.substring(1).equals(elementId);
        } else if (selector.startsWith("@")) {
            // Class选择器
            return elementClass != null && elementClass.contains(selector.substring(1));
        }
        return false;
    }

    /**
     * 获取选择器类型
     * @return "id", "class", 或 "tag"
     */
    public String getSelectorType() {
        if (selector.startsWith("#")) {
            return "id";
        } else if (selector.startsWith("@")) {
            return "class";
        } else {
            return "tag";
        }
    }
}
