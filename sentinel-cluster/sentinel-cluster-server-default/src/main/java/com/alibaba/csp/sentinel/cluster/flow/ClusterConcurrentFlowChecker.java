/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel.cluster.flow;

import com.alibaba.csp.sentinel.cluster.TokenResult;
import com.alibaba.csp.sentinel.cluster.TokenResultStatus;
import com.alibaba.csp.sentinel.cluster.flow.rule.ClusterConcurrentFlowRuleManager;
import com.alibaba.csp.sentinel.cluster.flow.statistic.ClusterMetricStatistics;
import com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent.CurrentConcurrencyManager;
import com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent.TokenCacheNode;
import com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent.TokenCacheNodeManager;
import com.alibaba.csp.sentinel.cluster.flow.statistic.data.ClusterFlowEvent;
import com.alibaba.csp.sentinel.cluster.flow.statistic.metric.ClusterMetric;
import com.alibaba.csp.sentinel.slots.block.flow.concurrent.ConcurrentFlowRule;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yunfeiyanggzq
 */
public final class ClusterConcurrentFlowChecker {

    private static TokenCacheNodeManager tokenCacheNodeManager = new TokenCacheNodeManager();

    public static TokenResult acquireClusterToken(String clientAddress,/*@Valid*/ ConcurrentFlowRule rule, int acquireCount, boolean prioritized) {
        Long flowId = rule.getClusterConfig().getFlowId();
        AtomicInteger nowCalls = CurrentConcurrencyManager.get(flowId);
        ClusterMetric metric = ClusterMetricStatistics.getMetric(flowId);
        if (nowCalls == null) {
            return new TokenResult(TokenResultStatus.FAIL);
        }
        if (!CurrentConcurrencyManager.tryPass(rule.getConcurrencyLevel(), flowId, acquireCount)) {
            return new TokenResult(TokenResultStatus.BLOCKED);
        }
        TokenCacheNode node = TokenCacheNode.generateTokenCacheNode(rule, acquireCount, clientAddress);
        tokenCacheNodeManager.putTokenCacheNode(node.getTokenId(), node);
        TokenResult tokenResult = new TokenResult();
        tokenResult.setStatus(TokenResultStatus.OK);
        tokenResult.setTokenId(node.getTokenId());
        return tokenResult;
    }

    public static TokenResult releaseClusterToken(/*@Valid*/ long tokenId) {
//        System.out.println("+++++++++++++++请求释放+++++++++++++++");
        // TODO: do with the return tokenResult.
        TokenCacheNode node = tokenCacheNodeManager.getTokenCacheNode(tokenId);
        if (node == null) {
            return new TokenResult(TokenResultStatus.READY_RELEASE);
        }
        ConcurrentFlowRule rule = ClusterConcurrentFlowRuleManager.getFlowRuleById(node.getFlowId());
        if (rule == null) {
            return new TokenResult(TokenResultStatus.NO_RULE_EXISTS);
        }
        if (tokenCacheNodeManager.removeTokenCacheNode(tokenId) == null) {
            return new TokenResult(TokenResultStatus.READY_RELEASE);
        }
        int acquireCount = node.getAcquireCount();
        AtomicInteger nowCalls = CurrentConcurrencyManager.get(node.getFlowId());
        nowCalls.getAndAdd(acquireCount * -1);
        rule.addReleaseCount(acquireCount);
        return new TokenResult(TokenResultStatus.RELEASE_OK);
    }

    public static TokenResult keepClusterToken(/*@Valid*/ long tokenId) {
//        System.out.println("+++++++++++++++请求保持+++++++++++++++");
        TokenCacheNode node = tokenCacheNodeManager.getTokenCacheNode(tokenId);
        if (node == null) {
            return new TokenResult(TokenResultStatus.READY_RELEASE);
        }
        ConcurrentFlowRule rule = ClusterConcurrentFlowRuleManager.getFlowRuleById(node.getFlowId());
        if (rule == null) {
            return new TokenResult(TokenResultStatus.NO_RULE_EXISTS);
        }
        // TODO:concurrent safe
        node.setResourceTimeout(rule.getResourceTimeout());
        node.setClientTimeout(rule.getClientTimeout());
        return new TokenResult(TokenResultStatus.KEEP_OK);
    }

    public static int getSize() {
        return tokenCacheNodeManager.getSize();
    }
}
