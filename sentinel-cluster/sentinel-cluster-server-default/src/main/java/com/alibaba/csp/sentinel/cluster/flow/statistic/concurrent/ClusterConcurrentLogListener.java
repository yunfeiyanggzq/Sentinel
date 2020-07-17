package com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent;

import com.alibaba.csp.sentinel.cluster.flow.ConcurrentClusterFlowChecker;
import com.alibaba.csp.sentinel.cluster.flow.rule.ClusterFlowRuleManager;
import com.alibaba.csp.sentinel.cluster.server.log.ClusterServerStatLogUtil;
import com.alibaba.csp.sentinel.log.RecordLog;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yunfeiyanggzq
 */
public class ClusterConcurrentLogListener implements Runnable {
    @Override
    public void run() {
        try {
            collectInformation();
        } catch (Exception e) {
            RecordLog.warn("[ClusterConcurrentLogListener] Failed to record concurrent flow control  regularly", e);
        }
    }

    private void collectInformation() {
        ConcurrentHashMap<Long, AtomicInteger> nowCallsMap = CurrentConcurrencyManager.getConcurrencyMap();
        for (long flowId : nowCallsMap.keySet()) {
            FlowRule rule = ClusterFlowRuleManager.getFlowRuleById(flowId);
            if (rule == null || nowCallsMap.get(flowId).get() == 0) {
                continue;
            }
            double concurrencyLevel = ConcurrentClusterFlowChecker.calcGlobalThreshold(rule);
            String resource = rule.getResource();
            ClusterServerStatLogUtil.log("concurrent|resource:" + resource + "|flowId:" + flowId + "|concurrencyLevel:" + concurrencyLevel, nowCallsMap.get(flowId).get());
        }
    }
}
