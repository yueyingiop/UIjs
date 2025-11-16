package com.core.UIJS.mcml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;

// MCML节点
public class MCMLNode {
    private final String tagName; // 标签名称
    private final Map<String, String> attributes; // 属性
    private String textContent; // 文本内容
    private final List<MCMLNode> children; // 子节点
    private final Map<String, String> computedStyles; // 计算后的样式
    
    /**
     * 创建一个MCML节点(构造函数)
     * @param tagName
     * @param attributes
     * @param textContent
    */
    public MCMLNode(String tagName, Map<String, String> attributes, String textContent) {
        this.tagName = tagName;
        this.attributes = attributes != null ? attributes : new HashMap<>();
        this.textContent = textContent != null ? textContent : "";
        this.children = new ArrayList<>();
        this.computedStyles = new HashMap<>();
    }
    
    // 添加子节点
    public void addChild(MCMLNode child) {
        children.add(child);
    }

    // 设置计算后的样式
    public void setComputedStyles(Map<String, String> styles) {
        computedStyles.clear();
        computedStyles.putAll(styles);
    }
    
    //#region Getters
    public String getTagName() { return tagName; }
    public Map<String, String> getAttributes() { return attributes; }
    public String getTextContent() { return textContent; }
    public List<MCMLNode> getChildren() { return children; }
    public Map<String, String> getComputedStyles() { return computedStyles; }
    public String getStyle(String key) { return computedStyles.get(key); }
    public String getAttribute(String key) { return attributes.get(key); }
    public String getId() { return attributes.get("id"); }
    public String getClassName() { return attributes.get("class"); }
    
    // 获取样式, 内联样式优先,内部样式表其次
    public String getAttributeOrStyle(String key) {
        String value = getAttribute(key);
        if (value != null) {
            return value;
        }
        return getStyle(key);
    }
    //#endregion

    public void setTextContent(String textContent) {
        this.textContent = textContent != null ? textContent : "";
    }
   
    // 转换为JSON对象
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("tag", tagName); // 标签名称
        
        // 添加属性
        if (!attributes.isEmpty()) {
            JsonObject attrs = new JsonObject();
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                attrs.addProperty(entry.getKey(), entry.getValue());
            }
            json.add("attributes", attrs);
        }
        
        // 添加文本内容
        if (!textContent.isEmpty()) {
            json.addProperty("text", textContent);
        }

        // 添加样式
        if (!computedStyles.isEmpty()) {
            JsonObject styles = new JsonObject();
            for (Map.Entry<String, String> entry : computedStyles.entrySet()) {
                styles.addProperty(entry.getKey(), entry.getValue());
            }
            json.add("styles", styles);
        }
        
        // 添加子节点
        if (!children.isEmpty()) {
            JsonObject childrenJson = new JsonObject();
            for (int i = 0; i < children.size(); i++) {
                childrenJson.add("child_" + i, children.get(i).toJson());
            }
            json.add("children", childrenJson);
        }
        
        return json;
    }
}
