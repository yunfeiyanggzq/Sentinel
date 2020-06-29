package com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent;

import com.alibaba.csp.sentinel.context.Context;
import com.alibaba.csp.sentinel.node.DefaultNode;
import com.alibaba.csp.sentinel.slots.block.AbstractRule;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;

public class ConcurrentFlowRule extends AbstractRule {

    public ConcurrentFlowRule() {
        super();
        setLimitApp(RuleConstant.LIMIT_APP_DEFAULT);
    }

    public ConcurrentFlowRule(String resourceName) {
        super();
        setResource(resourceName);
        setLimitApp(RuleConstant.LIMIT_APP_DEFAULT);
    }

    // rule’s flueId
    private long flowId;

    // the max concurrency
    private int concurrencyLevel;

    // client offline detection time, which is the token expiration time
    private long clientTimeout;

    // client call resource overtime detection time
    private long sourceTimeout;

    public long getFlowId() {
        return flowId;
    }

    public void setFlowId(long flowId) {
        this.flowId = flowId;
    }

    public int getConcurrencyLevel() {
        return concurrencyLevel;
    }

    public void setConcurrencyLevel(int concurrencyLevel) {
        this.concurrencyLevel = concurrencyLevel;
    }

    public long getClientTimeout() {
        return clientTimeout;
    }

    public void setClientTimeout(long clientTimeout) {
        this.clientTimeout = clientTimeout;
    }

    public long getSourceTimeout() {
        return sourceTimeout;
    }

    public void setSourceTimeout(long sourceTimeout) {
        this.sourceTimeout = sourceTimeout;
    }

    @Override
    public boolean passCheck(Context context, DefaultNode node, int count, Object... args) {
        return false;
    }
}
