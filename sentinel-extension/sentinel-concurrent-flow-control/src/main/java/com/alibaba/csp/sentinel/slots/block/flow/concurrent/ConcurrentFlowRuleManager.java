package com.alibaba.csp.sentinel.slots.block.flow.concurrent;

import com.alibaba.csp.sentinel.log.RecordLog;
import com.alibaba.csp.sentinel.property.DynamicSentinelProperty;
import com.alibaba.csp.sentinel.property.PropertyListener;
import com.alibaba.csp.sentinel.property.SentinelProperty;
import com.alibaba.csp.sentinel.util.AssertUtil;
import com.alibaba.csp.sentinel.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: yunfeiyanggzq
 * @Date: 2020/7/3 17:46
 */
public class ConcurrentFlowRuleManager {

    private static final Map<String, List<ConcurrentFlowRule>> flowRules = new ConcurrentHashMap<String, List<ConcurrentFlowRule>>();

    private static final ConcurrentFlowRuleManager.FlowPropertyListener LISTENER = new ConcurrentFlowRuleManager.FlowPropertyListener();
    private static SentinelProperty<List<ConcurrentFlowRule>> currentProperty = new DynamicSentinelProperty<List<ConcurrentFlowRule>>();

    static {
        currentProperty.addListener(LISTENER);
    }

    /**
     * Listen to the {@link SentinelProperty} for {@link ConcurrentFlowRule}s. The property is the source of {@link ConcurrentFlowRule}s.
     * Flow rules can also be set by {@link #loadRules(List)} directly.
     *
     * @param property the property to listen.
     */
    public static void register2Property(SentinelProperty<List<ConcurrentFlowRule>> property) {
        AssertUtil.notNull(property, "property cannot be null");
        synchronized (LISTENER) {
            RecordLog.info("[ConcurrentFlowRuleManager] Registering new property to flow rule manager");
            currentProperty.removeListener(LISTENER);
            property.addListener(LISTENER);
            currentProperty = property;
        }
    }

    /**
     * Get a copy of the rules.
     *
     * @return a new copy of the rules.
     */
    public static List<ConcurrentFlowRule> getRules() {
        List<ConcurrentFlowRule> rules = new ArrayList<ConcurrentFlowRule>();
        for (Map.Entry<String, List<ConcurrentFlowRule>> entry : flowRules.entrySet()) {
            rules.addAll(entry.getValue());
        }
        return rules;
    }

    /**
     * Load {@link ConcurrentFlowRule}s, former rules will be replaced.
     *
     * @param rules new rules to load.
     */
    public static void loadRules(List<ConcurrentFlowRule> rules) {
        currentProperty.updateValue(rules);
    }

    static Map<String, List<ConcurrentFlowRule>> getFlowRuleMap() {
        return flowRules;
    }

    public static boolean hasConfig(String resource) {
        return flowRules.containsKey(resource);
    }

    public static boolean isOtherOrigin(String origin, String resourceName) {
        if (StringUtil.isEmpty(origin)) {
            return false;
        }

        List<ConcurrentFlowRule> rules = flowRules.get(resourceName);

        if (rules != null) {
            for (ConcurrentFlowRule rule : rules) {
                if (origin.equals(rule.getLimitApp())) {
                    return false;
                }
            }
        }

        return true;
    }

    private static final class FlowPropertyListener implements PropertyListener<List<ConcurrentFlowRule>> {

        @Override
        public void configUpdate(List<ConcurrentFlowRule> value) {
            Map<String, List<ConcurrentFlowRule>> rules = ConcurrentFlowRuleUtil.buildFlowRuleMap(value);
            if (rules != null) {
                flowRules.clear();
                flowRules.putAll(rules);
            }
            RecordLog.info("[ConcurrentFlowRuleManager] Flow rules received: " + flowRules);
        }

        @Override
        public void configLoad(List<ConcurrentFlowRule> conf) {
            Map<String, List<ConcurrentFlowRule>> rules = ConcurrentFlowRuleUtil.buildFlowRuleMap(conf);
            if (rules != null) {
                flowRules.clear();
                flowRules.putAll(rules);
            }
            RecordLog.info("[ConcurrentFlowRuleManager] Flow rules loaded: " + flowRules);
        }
    }
}
