package com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent;

import com.alibaba.csp.sentinel.context.Context;
import com.alibaba.csp.sentinel.node.DefaultNode;
import com.alibaba.csp.sentinel.slots.block.AbstractRule;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.ClusterFlowConfig;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yunfeiyanggzq
 */
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

    private int expireCount=0;

    private boolean clusterMode;
    /**
     * Flow rule config for cluster mode.
     */
    private ClusterFlowConfig clusterConfig;

    private AtomicInteger releaseCount=new AtomicInteger(0);


    public boolean isClusterMode() {
        return clusterMode;
    }

    public void setClusterMode(boolean clusterMode) {
        this.clusterMode = clusterMode;
    }

    public ClusterFlowConfig getClusterConfig() {
        return clusterConfig;
    }

    public void setClusterConfig(ClusterFlowConfig clusterConfig) {
        this.clusterConfig = clusterConfig;
    }

    public AtomicInteger getReleaseCount() {
        return releaseCount;
    }

    public void addReleaseCount(Integer releaseCount) {
        this.releaseCount.getAndAdd(releaseCount);
    }

    public int getExpireCount() {
        return expireCount;
    }

    public void addExpireCount(Integer expireCount) {
        this.expireCount = this.expireCount+expireCount;
    }

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
    public String toString() {
        return "ConcurrentFlowRule{" +
                "flowId=" + flowId +
                ", concurrencyLevel=" + concurrencyLevel +
                ", clientTimeout=" + clientTimeout +
                ", sourceTimeout=" + sourceTimeout +
                ", expireCount=" + expireCount +
                ", releaseCount=" + releaseCount +
                '}';
    }

    @Override
    public boolean passCheck(Context context, DefaultNode node, int count, Object... args) {
        return false;
    }
}
