package com.core.UIJS.mcml;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// MCML解析器
public class MCMLParser {
    private static final Logger LOGGER = LogManager.getLogger("UIJS-MCMLParser");
    // 正则表达式匹配标签和属性
    private static final Pattern FULL_TAG_PATTERN = Pattern.compile("<(\\w+)([^>]*)>([^<]*)</\\1>");
    private static final Pattern SELF_CLOSING_TAG_PATTERN = Pattern.compile("<(\\w+)([^>]*)/>");
    private static final Pattern ATTR_PATTERN = Pattern.compile("(\\w+)=[\"']([^\"']*)[\"']");
    private static final Pattern STYLE_RULE_PATTERN = Pattern.compile("([#@]\\w+)\\s*\\{([^}]*)\\}");
    private static final Pattern STYLE_PROPERTY_PATTERN = Pattern.compile("([\\w-]+)\\s*:\\s*([^;]+);?");

    private StyleSheet styleSheet; // 样式表

    public MCMLParser() {
        this.styleSheet = new StyleSheet();
    }

    // 解析文件
    public MCMLNode parseFile(Path filePath) {
        // 读取文件
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            StringBuilder content = new StringBuilder(); // 用于存储文件内容
            String line; // 用于存储行
            // 逐行读取文件,去除多余空白然后存储
            while ((line = reader.readLine()) != null) {
                content.append(line.trim());
            }
            return parse(content.toString());
        } catch (IOException e) {
            LOGGER.error("Failed to parse MCML file: {}", filePath, e);
            return null;
        }
    }
    
    // 解析字符串
    public MCMLNode parse(String content) {
        // 首先提取并解析所有样式
        parseStyleRulesFromContent(content);
        // 移除样式标签，避免干扰后续解析
        content = content.replaceAll("<style[^>]*>.*?</style>", "");

        MCMLNode rootNode = null;
        Stack<MCMLNode> nodeStack = new Stack<>();

        // 解析主标签
        parseMain(content, nodeStack);
        
        // 解析自闭合标签
        parseSelfClosingTags(content, nodeStack);
        
        // 解析完整标签
        parseFullTags(content, nodeStack);
        
        if (!nodeStack.isEmpty()) {
            rootNode = nodeStack.get(0);
        }
        
        // 应用样式
        if (rootNode != null) {
            applyStyles(rootNode);
        }
        
        return rootNode;
    }

    // 主标签解析方法
    private void parseMain(String content, Stack<MCMLNode> nodeStack) {
        // 解析主标签
        Pattern mainPattern = Pattern.compile("<(\\w+)([^>]*)>");
        Matcher matcher = mainPattern.matcher(content);
        while (matcher.find()) {
            String tagName = matcher.group(1);
            String attrString = matcher.group(2);
            String textContent = null;
            
            if (!tagName.equals("main")) return;
            Map<String, String> attributes = parseAttributes(attrString);
            MCMLNode node = new MCMLNode(tagName, attributes, textContent);
            
            addNodeToParent(node, nodeStack);
        }
    }

    // 自闭合标签解析方法
    private void parseSelfClosingTags(String content, Stack<MCMLNode> nodeStack) {
        Matcher matcher = SELF_CLOSING_TAG_PATTERN.matcher(content);
        while (matcher.find()) {
            String tagName = matcher.group(1);
            String attrString = matcher.group(2);
            
            Map<String, String> attributes = parseAttributes(attrString);
            MCMLNode node = new MCMLNode(tagName, attributes, "");
            
            addNodeToParent(node, nodeStack);
        }
    }
    

    // 完整标签解析方法
    private void parseFullTags(String content, Stack<MCMLNode> nodeStack) {
        Matcher matcher = FULL_TAG_PATTERN.matcher(content);
        while (matcher.find()) {
            String tagName = matcher.group(1);
            String attrString = matcher.group(2);
            String textContent = matcher.group(3).trim();
            
            Map<String, String> attributes = parseAttributes(attrString);
            MCMLNode node = new MCMLNode(tagName, attributes, textContent);
            
            addNodeToParent(node, nodeStack);
            
            // 如果不是叶子节点，压入栈以便后续子节点可以添加到其中
            if (!isLeafNode(tagName)) {
                nodeStack.push(node);
            }
        }
    }

    // 添加节点到父节点
    private void addNodeToParent(MCMLNode node, Stack<MCMLNode> nodeStack) {
        if (nodeStack.isEmpty()) {
            nodeStack.push(node);
        } else {
            nodeStack.peek().addChild(node);
        }
    }

    // 样式解析方法
    private void parseStyleRulesFromContent(String content) {
        Pattern stylePattern = Pattern.compile("<style[^>]*>(.*?)</style>", Pattern.DOTALL);
        Matcher styleMatcher = stylePattern.matcher(content);
        
        while (styleMatcher.find()) {
            String styleContent = styleMatcher.group(1);
            parseStyleRules(styleContent);
        }
    }

    // 解析样式规则
    private void parseStyleRules(String styleContent) {
        if (styleContent == null || styleContent.trim().isEmpty()) {
            return;
        }
        
        Matcher ruleMatcher = STYLE_RULE_PATTERN.matcher(styleContent);
        while (ruleMatcher.find()) {
            String selector = ruleMatcher.group(1);
            String propertiesStr = ruleMatcher.group(2);
            
            StyleRule rule = new StyleRule(selector);
            
            Matcher propMatcher = STYLE_PROPERTY_PATTERN.matcher(propertiesStr);
            while (propMatcher.find()) {
                String property = propMatcher.group(1);
                String value = propMatcher.group(2).trim();
                rule.setProperty(property, value);
            }
            
            styleSheet.addRule(rule);
        }
    }
    
    // 解析属性
    private Map<String, String> parseAttributes(String attrString) {
        Map<String, String> attributes = new HashMap<>();
        if (attrString == null || attrString.trim().isEmpty()) return attributes;
        
        Matcher attrMatcher = ATTR_PATTERN.matcher(attrString);
        while (attrMatcher.find()) {
            String key = attrMatcher.group(1);
            String value = attrMatcher.group(2);
            attributes.put(key, value);
        }
        
        return attributes;
    }

    // 应用样式
    private void applyStyles(MCMLNode node) {
        // 获取元素的ID和class
        String id = node.getId();
        String className = node.getClassName();
        
        // 应用匹配的样式规则
        Map<String, String> styles = styleSheet.getStylesForElement(id, className);
        node.setComputedStyles(styles);
        // 递归应用到子节点
        for (MCMLNode child : node.getChildren()) {
            applyStyles(child);
        }
    }

    // 判断是否为叶子节点
    private boolean isLeafNode(String tagName) {
        return "text".equals(tagName) || "br".equals(tagName) || "style".equals(tagName);
    }

    public StyleSheet getStyleSheet() {
        return styleSheet;
    }

}
