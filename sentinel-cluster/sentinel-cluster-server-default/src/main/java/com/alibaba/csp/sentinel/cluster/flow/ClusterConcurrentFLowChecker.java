package com.alibaba.csp.sentinel.cluster.flow;

import com.alibaba.csp.sentinel.cluster.TokenResult;
import com.alibaba.csp.sentinel.cluster.TokenResultStatus;
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
        synchronized (nowCalls) {
            if (nowCalls.get() >= rule.getConcurrencyLevel()) {
                System.out.println("*****************"+NowCallsManager.get(flowId));
                return new TokenResult(TokenResultStatus.BLOCKED);
            } else {
                NowCallsManager.update(flowId, acquireCount);
                System.out.println("-----------------"+NowCallsManager.get(flowId));
            }
        }

        TokenCacheNode node = TokenCacheNode.generateTokenCacheNode(rule, acquireCount);
        tokenCacheNodeManager.putTokenCacheNode(node.getTokenId(), node);
        TokenResult tokenResult = new TokenResult();
        tokenResult.setStatus(TokenResultStatus.OK);
        tokenResult.setTokenId(node.getTokenId());
        return tokenResult;
    }

    static TokenResult releaseClusterToken(long tokenId)  {
        TokenCacheNode node = tokenCacheNodeManager.getTokenCacheNode(tokenId);
        if (node == null) {
            return new TokenResult(TokenResultStatus.READY_REALSE);
        }
        long acquireCount = node.getAquireCount();
        long flowId = node.getFlowId();
        tokenCacheNodeManager.removeTokenCacheNode(tokenId);
        NowCallsManager.update(flowId, (int) acquireCount * (-1));
        System.out.println("+++++++++++++++++++++"+NowCallsManager.get(flowId)+"|"+tokenCacheNodeManager.getSize());
        return new TokenResult(TokenResultStatus.READY_REALSE);
    }
}
