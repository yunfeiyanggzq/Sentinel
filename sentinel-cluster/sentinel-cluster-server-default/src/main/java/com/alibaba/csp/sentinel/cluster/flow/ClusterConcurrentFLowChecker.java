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
    private static TokenCacheNodeManager tokenCacheNodeManager = new TokenCacheNodeManager();

    public static TokenResult acquireClusterToken(/*@Valid*/ ConcurrentFlowRule rule, int acquireCount, boolean prioritized) {

        Long flowId = rule.getFlowId();
        AtomicInteger nowCalls = NowCallsManager.get(flowId);
        System.out.println(nowCalls.get());
        if (nowCalls == null) {
            return new TokenResult(TokenResultStatus.FAIL);
        }
        synchronized (nowCalls) {
            if (nowCalls.get() >= rule.getConcurrencyLevel()) {
                return new TokenResult(TokenResultStatus.BLOCKED);
            } else {
                nowCalls.getAndAdd(acquireCount);
            }
        }
        TokenCacheNode node = TokenCacheNode.generateTokenCacheNode(rule, acquireCount);
        tokenCacheNodeManager.putTokenCacheNode(node.getTokenId(), node);
        TokenResult tokenResult = new TokenResult();
        tokenResult.setStatus(TokenResultStatus.OK);
        tokenResult.setTokenId(node.getTokenId());
        return tokenResult;
    }

    public static TokenResult releaseClusterToken(long tokenId) {
        TokenCacheNode node = tokenCacheNodeManager.getTokenCacheNode(tokenId);
        if (node == null) {
            return new TokenResult(TokenResultStatus.READY_REALSE);
        }
        if (tokenCacheNodeManager.removeTokenCacheNode(tokenId) == null) {
            return new TokenResult(TokenResultStatus.READY_REALSE);
        }
        int acquireCount = node.getAcquireCount();
        AtomicInteger nowCalls = NowCallsManager.get(node.getFlowId());
        nowCalls.getAndAdd(acquireCount * -1);
        ClusterConcurrentFlowRuleManager.getFlowRule(node.getFlowId()).addReleaseCount(node.getAcquireCount());
        return new TokenResult(TokenResultStatus.READY_REALSE);
    }

    public static int getSize() {
        return tokenCacheNodeManager.getSize();
    }
}
