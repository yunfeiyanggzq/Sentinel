package com.alibaba.csp.sentinel.cluster.flow;

import com.alibaba.csp.sentinel.cluster.TokenResult;
import com.alibaba.csp.sentinel.cluster.TokenResultStatus;
import com.alibaba.csp.sentinel.cluster.flow.rule.ClusterConcurrentFlowRuleManager;
import com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent.ConcurrentFlowRule;
import com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent.NowCallsManager;
import com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent.TokenCacheNode;
import com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent.TokenCacheNodeManager;

import java.util.concurrent.atomic.AtomicInteger;

public class ClusterConcurrentFLowChecker {
    static TokenCacheNodeManager tokenCacheNodeManager = new TokenCacheNodeManager();

    static TokenResult acquireClusterToken(/*@Valid*/ ConcurrentFlowRule rule, int acquireCount, boolean prioritized) {
        Long flowId = rule.getFlowId();
        AtomicInteger nowCalls = NowCallsManager.get(flowId);
        if (nowCalls == null) {
            return new TokenResult(TokenResultStatus.FAIL);
        }
        if (nowCalls.get() > rule.getConcurrencyLevel()) {
            return new TokenResult(TokenResultStatus.BLOCKED);
        }
        TokenCacheNode node = TokenCacheNode.generateTokenCacheNode(rule, acquireCount);
        NowCallsManager.update(flowId, acquireCount);
        tokenCacheNodeManager.putTokenCacheNode(flowId, node);
        TokenResult tokenResult = new TokenResult();
        tokenResult.setStatus(TokenResultStatus.OK);
        tokenResult.setTokenId(node.getTokenId());
        return tokenResult;
    }

    public static void main(String[] args) {
        ConcurrentFlowRule rule=new ConcurrentFlowRule();
        rule.setClientTimeout(100L);
        rule.setSourceTimeout(500L);
        rule.setConcurrencyLevel(50);
        rule.setFlowId(111L);
        ClusterConcurrentFlowRuleManager.addFlowRule(111L,rule);
        ClusterConcurrentFLowChecker.acquireClusterToken(rule,1,false);


    }
}
