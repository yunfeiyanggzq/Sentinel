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
package com.alibaba.csp.sentinel.slots.block.flow.concurrent;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.cluster.ClusterStateManager;
import com.alibaba.csp.sentinel.cluster.TokenResult;
import com.alibaba.csp.sentinel.cluster.TokenResultStatus;
import com.alibaba.csp.sentinel.cluster.TokenService;
import com.alibaba.csp.sentinel.cluster.client.TokenClientProvider;
import com.alibaba.csp.sentinel.cluster.server.EmbeddedClusterTokenServerProvider;
import com.alibaba.csp.sentinel.context.Context;
import com.alibaba.csp.sentinel.log.RecordLog;
import com.alibaba.csp.sentinel.node.ClusterNode;
import com.alibaba.csp.sentinel.node.DefaultNode;
import com.alibaba.csp.sentinel.slotchain.ResourceWrapper;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.flow.timeout.ReSourceTimeoutStrategy;
import com.alibaba.csp.sentinel.slots.block.flow.timeout.ReSourceTimeoutStrategyUtil;
import com.alibaba.csp.sentinel.slots.block.flow.timeout.TimerHolder;
import com.alibaba.csp.sentinel.slots.clusterbuilder.ClusterBuilderSlot;
import com.alibaba.csp.sentinel.util.HostNameUtil;
import com.alibaba.csp.sentinel.util.function.Function;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;

import java.util.Collection;
import java.util.concurrent.TimeUnit;


/**
 * @author yunfeiyanggzq
 */
public class ConcurrentFlowRuleChecker {
    private static long LOCAL_TOKEN_ID = 0L;

    /**
     * check flow and get token.
     */
    public void checkFlow(Function<String, Collection<ConcurrentFlowRule>> ruleProvider, ResourceWrapper resource,
                          Context context, DefaultNode node, int count, boolean prioritized) throws BlockException {
        if (ruleProvider == null || resource == null) {
            return;
        }
        Collection<ConcurrentFlowRule> rules = ruleProvider.apply(resource.getName());
        if (rules != null) {
            for (ConcurrentFlowRule rule : rules) {
                if (!canPassCheck(rule, resource, context, node, count, prioritized)) {
                    throw new ConcurrentFlowException(rule.getLimitApp(), rule);
                }
            }
        }
    }

    /**
     * release the token.
     */
    public void releaseConcurrentFlowToken(Context context, ResourceWrapper resourceWrapper) {
        Timeout timeout = context.getCurEntry().getTimeout();
        if (timeout == null || timeout.isCancelled() || timeout.isExpired()) {
            return;
        }
        timeout.cancel();
        releaseToken(context, resourceWrapper);
    }

    public boolean canPassCheck(/*@NonNull*/ ConcurrentFlowRule rule, ResourceWrapper resource, Context context, DefaultNode node,
                                             int acquireCount) {
        return canPassCheck(rule, resource, context, node, acquireCount, false);
    }

    public boolean canPassCheck(/*@NonNull*/ ConcurrentFlowRule rule, ResourceWrapper resource, Context context, DefaultNode node, int acquireCount,
                                             boolean prioritized) {
        String limitApp = rule.getLimitApp();
        if (limitApp == null) {
            return true;
        }

        if (rule.isClusterMode()) {
            return passClusterCheck(rule, context, node, acquireCount, prioritized);
        }

        return passLocalCheck(rule, resource, context, node, acquireCount, prioritized);
    }

    private static boolean fallbackToLocalOrPass(ConcurrentFlowRule rule, Context context, DefaultNode node, int acquireCount,
                                                 boolean prioritized) {
        return passLocalCheck(rule, null, context, node, acquireCount, prioritized);
    }

    private static boolean passLocalCheck(ConcurrentFlowRule rule, ResourceWrapper resource, Context context, DefaultNode node, int acquireCount,
                                          boolean prioritized) {
        ClusterNode clusterNode = node.getClusterNode();
        if (clusterNode == null) {
            return true;
        }
        if (clusterNode.tryConcurrentPass(acquireCount, rule.getCount())) {
            setEntry(rule, context, node, acquireCount, prioritized, LOCAL_TOKEN_ID, false);
            return true;
        }
        return false;
    }

    private static boolean passClusterCheck(ConcurrentFlowRule rule, Context context, DefaultNode node, int acquireCount,
                                            boolean prioritized) {
        try {
            // If client is absent, then fallback to local mode.
            TokenService clusterService = pickClusterService();
            if (clusterService == null) {
                return fallbackToLocalOrPass(rule, context, node, acquireCount, prioritized);
            }
            long flowId = rule.getClusterConfig().getFlowId();

            // get the client address, if the server state is client, client address will be get by server.
            String clientAddress = null;
            if (ClusterStateManager.isServer()) {
                clientAddress = HostNameUtil.getIp();
            }

            TokenResult tokenResult = clusterService.acquireConcurrentToken(clientAddress, flowId, acquireCount, prioritized);
            boolean result = applyTokenResult(tokenResult, rule, context, node, acquireCount, prioritized);
            if (tokenResult.getStatus() == TokenResultStatus.OK) {
                node.getClusterNode().addConcurrency(acquireCount);
                // set tokenId for this entry.
                setEntry(rule, context, node, acquireCount, prioritized, tokenResult.getTokenId(), true);
            }
            return result;
        } catch (Throwable ex) {
            RecordLog.warn("[ConcurrentFlowRuleChecker] Request cluster token unexpected failed", ex);
        }
        // Fallback to local flow control when token client or server for this rule is not available.
        // If fallback is not enabled, then directly pass.
        return fallbackToLocalOrPass(rule, context, node, acquireCount, prioritized);
    }

    private static void setEntry(final ConcurrentFlowRule rule, final Context context, DefaultNode node, final int acquireCount,
                                 boolean prioritized, final long tokenId, boolean isClusterToken) {
        final Entry entry = context.getCurEntry();
        entry.setTokenId(tokenId);
        entry.setIsClusterToken(isClusterToken);
        entry.setTimeout(TimerHolder.getTimer().newTimeout(new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {
                ReSourceTimeoutStrategy strategy = ReSourceTimeoutStrategyUtil.getTimeoutStrategy(rule.getResourceTimeoutStrategy());
                if (strategy != null) {
                    ClusterNode node = ClusterBuilderSlot.getClusterNode(rule.getResource());
                    if (node != null) {
                        node.addConcurrency(acquireCount * -1);
                        strategy.doWithSourceTimeout(context, tokenId, timeout);
                    }
                }
            }
        }, rule.getResourceTimeout(), TimeUnit.MILLISECONDS));
    }

    public static TokenService pickClusterService() {
        if (ClusterStateManager.isClient()) {
            return TokenClientProvider.getClient();
        }
        if (ClusterStateManager.isServer()) {
            return EmbeddedClusterTokenServerProvider.getServer();
        }
        return null;
    }

    public static void releaseToken(Context context, ResourceWrapper resourceWrapper) {
        TokenService clusterService = pickClusterService();
        if (clusterService == null || context.getCurEntry() == null || context.getCurEntry().getTokenId() == 0) {
            return;
        }
        if (context.getCurEntry().getIsClusterToken()) {
            TokenResult result = clusterService.releaseConcurrentToken(context.getCurEntry().getTokenId());
            if (result.getStatus() != TokenResultStatus.RELEASE_OK) {
                RecordLog.warn("[ConcurrentFlowRuleChecker] release cluster token unexpected failed", result.getStatus());
            }
        }
        ClusterNode node = ClusterBuilderSlot.getClusterNode(resourceWrapper.getName());
        if (node != null) {
            node.addConcurrency(-1);
        }
    }

    private static boolean applyTokenResult(/*@NonNull*/ TokenResult result, ConcurrentFlowRule rule, Context context,
                                                         DefaultNode node,
                                                         int acquireCount, boolean prioritized) {
        switch (result.getStatus()) {
            case TokenResultStatus.OK:
                return true;
            case TokenResultStatus.NO_RULE_EXISTS:
            case TokenResultStatus.BAD_REQUEST:
            case TokenResultStatus.FAIL:
            case TokenResultStatus.TOO_MANY_REQUEST:
                return fallbackToLocalOrPass(rule, context, node, acquireCount, prioritized);
            default:
                return false;
        }
    }
}
