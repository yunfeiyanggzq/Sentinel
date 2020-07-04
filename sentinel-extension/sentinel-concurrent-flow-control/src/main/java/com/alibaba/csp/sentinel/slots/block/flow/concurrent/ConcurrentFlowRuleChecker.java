package com.alibaba.csp.sentinel.slots.block.flow.concurrent;

import com.alibaba.csp.sentinel.cluster.ClusterStateManager;
import com.alibaba.csp.sentinel.cluster.TokenResult;
import com.alibaba.csp.sentinel.cluster.TokenResultStatus;
import com.alibaba.csp.sentinel.cluster.TokenService;
import com.alibaba.csp.sentinel.cluster.client.TokenClientProvider;
import com.alibaba.csp.sentinel.cluster.server.EmbeddedClusterTokenServerProvider;
import com.alibaba.csp.sentinel.context.Context;
import com.alibaba.csp.sentinel.log.RecordLog;
import com.alibaba.csp.sentinel.node.DefaultNode;
import com.alibaba.csp.sentinel.slotchain.ResourceWrapper;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.util.function.Function;

import java.util.Collection;

/**
 * @Author: yunfeiyanggzq
 * @Date: 2020/7/3 17:33
 */
public class ConcurrentFlowRuleChecker {

    public void checkFlow(Function<String, Collection<ConcurrentFlowRule>> ruleProvider, ResourceWrapper resource,
                          Context context, DefaultNode node, int count, boolean prioritized) throws BlockException {
        if (ruleProvider == null || resource == null) {
            return;
        }
        Collection<ConcurrentFlowRule> rules = ruleProvider.apply(resource.getName());
        if (rules != null) {
            for (ConcurrentFlowRule rule : rules) {
                if (!canPassCheck(rule, context, node, count, prioritized)) {
                    throw new ConcurrentFlowException(rule.getLimitApp(), rule);
                }
            }
        }
    }

    public void releaseFlow(Context context, ResourceWrapper resourceWrapper, int count){
        // TODO:检查以前是什么方式限流的 本地还是集群？
        TokenService clusterService = pickClusterService();
        if (clusterService == null) {
            return;
        }
        if(context.getCurEntry().getTokenId()==0){
            return;
        }
        TokenResult result = clusterService.releaseConcurrentToken(context.getCurEntry().getTokenId());

    }

    public boolean canPassCheck(/*@NonNull*/ ConcurrentFlowRule rule, Context context, DefaultNode node,
                                             int acquireCount) {
        return canPassCheck(rule, context, node, acquireCount, false);
    }

    public boolean canPassCheck(/*@NonNull*/ ConcurrentFlowRule rule, Context context, DefaultNode node, int acquireCount,
                                             boolean prioritized) {
        String limitApp = rule.getLimitApp();
        if (limitApp == null) {
            return true;
        }

        if (rule.isClusterMode()) {
            return passClusterCheck(rule, context, node, acquireCount, prioritized);
        }

        return passLocalCheck(rule, context, node, acquireCount, prioritized);
    }
    private static boolean fallbackToLocalOrPass(ConcurrentFlowRule rule, Context context, DefaultNode node, int acquireCount,
                                                     boolean prioritized) {
        return true;
    }

    private static boolean passLocalCheck(ConcurrentFlowRule rule, Context context, DefaultNode node, int acquireCount,
                                              boolean prioritized) {
//        System.out.println("进入本地流控");
        return true;
    }

    private static boolean passClusterCheck(ConcurrentFlowRule rule, Context context, DefaultNode node, int acquireCount,
                                                boolean prioritized) {
//        System.out.println("进入集群流控");
        try {
            TokenService clusterService = pickClusterService();
            if (clusterService == null) {
                return fallbackToLocalOrPass(rule, context, node, acquireCount, prioritized);
            }
            long flowId = rule.getClusterConfig().getFlowId();
            TokenResult result = clusterService.acquireConcurrentToken(flowId, acquireCount, prioritized);
            context.getCurEntry().setTokenId(result.getTokenId());
            return applyTokenResult(result, rule, context, node, acquireCount, prioritized);
            // If client is absent, then fallback to local mode.
        } catch (Throwable ex) {
            RecordLog.warn("[FlowRuleChecker] Request cluster token unexpected failed", ex);
        }
        // Fallback to local flow control when token client or server for this rule is not available.
        // If fallback is not enabled, then directly pass.
        return fallbackToLocalOrPass(rule, context, node, acquireCount, prioritized);
    }

    private static TokenService pickClusterService() {
        if (ClusterStateManager.isClient()) {
            return TokenClientProvider.getClient();
        }
        if (ClusterStateManager.isServer()) {
            return EmbeddedClusterTokenServerProvider.getServer();
        }
        return null;
    }

    private static boolean applyTokenResult(/*@NonNull*/ TokenResult result, ConcurrentFlowRule rule, Context context,
                                                         DefaultNode node,
                                                         int acquireCount, boolean prioritized) {
        switch (result.getStatus()) {
            case TokenResultStatus.OK:
                return true;
            case TokenResultStatus.SHOULD_WAIT:
                // Wait for next tick.
                try {
                    Thread.sleep(result.getWaitInMs());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return true;
            case TokenResultStatus.NO_RULE_EXISTS:
            case TokenResultStatus.BAD_REQUEST:
            case TokenResultStatus.FAIL:
            case TokenResultStatus.TOO_MANY_REQUEST:
                return fallbackToLocalOrPass(rule, context, node, acquireCount, prioritized);
            case TokenResultStatus.BLOCKED:
            default:
                return false;
        }
    }
}
