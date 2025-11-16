package com.core.UIJS.mcml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 样式表
public class StyleSheet {
    private final List<StyleRule> rules;

    public StyleSheet() {
        this.rules = new ArrayList<>();
    }
    
    public void addRule(StyleRule rule) {
        rules.add(rule);
    }

    public Map<String, String> getStylesForElement(String id, String className) {
        Map<String, String> styles = new HashMap<>();

        // 按优先级收集样式
        List<StyleRule> matchedRules = new ArrayList<>();
        
        // 应用匹配的规则（后面的规则覆盖前面的）
        for (StyleRule rule : rules) {
            if (rule.matches(id, className)) {
                matchedRules.add(rule);
            }
        }

        // 按优先级排序：ID选择器 > class选择器
        matchedRules.sort((rule1, rule2) -> {
            int priority1 = getSelectorPriority(rule1.getSelector());
            int priority2 = getSelectorPriority(rule2.getSelector());
            return Integer.compare(priority2, priority1); // 降序排列，优先级高的在前
        });
        
        // 应用样式，高优先级的规则会覆盖低优先级的
        for (StyleRule rule : matchedRules) {
            styles.putAll(rule.getProperties());
        }
        
        return styles;
    }

    /**
     * 计算选择器优先级
     * 优先级规则（模仿CSS）：
     * - ID选择器 (#id): 优先级 100
     * - class选择器 (@class): 优先级 10
     * - 其他选择器: 优先级 1
     */
    private int getSelectorPriority(String selector) {
        if (selector.startsWith("#")) {
            return 100; // ID选择器最高优先级
        } else if (selector.startsWith("@")) {
            return 10;  // class选择器中等优先级
        } else {
            return 1;   // 其他选择器最低优先级
        }
    }
    
    public List<StyleRule> getRules() {
        return rules;
    }
}
