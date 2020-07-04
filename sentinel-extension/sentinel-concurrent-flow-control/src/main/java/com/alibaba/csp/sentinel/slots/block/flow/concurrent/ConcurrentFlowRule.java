package com.alibaba.csp.sentinel.slots.block.flow.concurrent;

import com.alibaba.csp.sentinel.context.Context;
import com.alibaba.csp.sentinel.node.DefaultNode;
import com.alibaba.csp.sentinel.slots.block.AbstractRule;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.ClusterFlowConfig;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author: yunfeiyanggzq
 * @Date: 2020/7/3 17:25
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

    /**
     * the max concurrency
     */
    private int concurrencyLevel;

    /**
     * client offline detection time, which is the token expiration time
     */
    private long clientTimeout;

    /**
     * client call resource overtime detection time
     */
    private long sourceTimeout;

    /**
     * the count of expired token
     */
    private int expireCount = 0;

    /**
     * the count of released token
     * TODO: remove this filed, only for test
     */
    private AtomicInteger releaseCount = new AtomicInteger(0);

    private boolean clusterMode;
    /**
     * Flow rule config for cluster mode.
     */
    private ClusterFlowConfig clusterConfig;

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

    public int getExpireCount() {
        return expireCount;
    }

    public void setExpireCount(int expireCount) {
        this.expireCount = expireCount;
    }

    public AtomicInteger getReleaseCount() {
        return releaseCount;
    }

    public void setReleaseCount(AtomicInteger releaseCount) {
        this.releaseCount = releaseCount;
    }

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

    public void addReleaseCount(Integer count) {
        this.releaseCount.getAndAdd(count);
    }
    public void addExpireCount(Integer count) {
        this.expireCount = this.expireCount + count;

    }
    @Override
    public boolean passCheck(Context context, DefaultNode node, int count, Object... args) {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ConcurrentFlowRule that = (ConcurrentFlowRule) o;
        return concurrencyLevel == that.concurrencyLevel &&
                clientTimeout == that.clientTimeout &&
                sourceTimeout == that.sourceTimeout &&
                expireCount == that.expireCount &&
                clusterMode == that.clusterMode &&
                Objects.equals(releaseCount, that.releaseCount) &&
                Objects.equals(clusterConfig, that.clusterConfig);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        long temp;
        result = 31 * result + (int) clientTimeout;
        result = 31 * result + (int) sourceTimeout;
        result = 31 * result + concurrencyLevel;
        result = 31 * result + expireCount;
        result = 31 * result + releaseCount.get();
        result = 31 * result + (clusterMode ? 1 : 0);
        result = 31 * result + (clusterConfig != null ? clusterConfig.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ConcurrentFlowRule{" +
                "concurrencyLevel=" + concurrencyLevel +
                ", clientTimeout=" + clientTimeout +
                ", resourceName=" + this.getResource() +
                ", sourceTimeout=" + sourceTimeout +
                ", expireCount=" + expireCount +
                ", releaseCount=" + releaseCount +
                ", clusterMode=" + clusterMode +
                ", clusterConfig=" + clusterConfig +
                '}';
    }
}
