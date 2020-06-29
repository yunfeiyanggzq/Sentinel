package com.alibaba.csp.sentinel.cluster.flow.rule;

import com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent.ConcurrentFlowRule;
import com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent.NowCallsManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClusterConcurrentFlowRuleManager {
    /**
     * (flowId, concurrentRule)
     */
    private static final Map<Long, ConcurrentFlowRule> CONCURRENT_FLOW_RULES = new ConcurrentHashMap<>();

    public static void addFlowRule(Long flowId, ConcurrentFlowRule rule) {
        NowCallsManager.put(flowId, 0);
        CONCURRENT_FLOW_RULES.put(flowId, rule);
    }

    public static ConcurrentFlowRule getFlowRule(Long flowId) {
        return CONCURRENT_FLOW_RULES.get(flowId);
    }

    public static void removeFlowRule(Long flowId) {
        CONCURRENT_FLOW_RULES.remove(flowId);
        NowCallsManager.remove(flowId);
    }
}
