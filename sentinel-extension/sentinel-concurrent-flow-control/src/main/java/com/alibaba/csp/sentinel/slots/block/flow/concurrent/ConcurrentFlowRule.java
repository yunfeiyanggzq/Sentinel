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

import com.alibaba.csp.sentinel.context.Context;
import com.alibaba.csp.sentinel.node.DefaultNode;
import com.alibaba.csp.sentinel.slots.block.AbstractRule;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.ClusterFlowConfig;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In order to achieve concurrent flow control, we need to reform FlowRule.
 * concurrencyLevel is the maximum number of concurrency, clientTimeout is
 * the token client disconnection detection time, and it is also a sign to
 * determine a token expires. The clientTimeout of different FlowRules are
 * often equivalent. sourceTimeout is the client's call resource timeout
 * detection time. This value will be based on the actual situation of the
 * resource. The sourceTimeout of different FlowRules is often not equal.
 *
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
    private long resourceTimeout;

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
     * flow rule config for cluster mode.
     */
    private ClusterFlowConfig clusterConfig;

    /**
     * if the resource is called timeout,token client will do will it
     */
    private int resourceTimeoutStrategy;

    private int count;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
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

    public long getResourceTimeout() {
        return resourceTimeout;
    }

    public void setResourceTimeout(long sourceTimeout) {
        this.resourceTimeout = sourceTimeout;
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

    public int getResourceTimeoutStrategy() {
        return resourceTimeoutStrategy;
    }

    public void setResourceTimeoutStrategy(int resourceTimeoutStrategy) {
        this.resourceTimeoutStrategy = resourceTimeoutStrategy;
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
                resourceTimeout == that.resourceTimeout &&
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
        result = 31 * result + (int) resourceTimeout;
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
                ", resourceTimeout=" + resourceTimeout +
                ", expireCount=" + expireCount +
                ", releaseCount=" + releaseCount +
                ", clusterMode=" + clusterMode +
                ", clusterConfig=" + clusterConfig +
                ", resourceTimeoutStrategy=" + resourceTimeoutStrategy +
                ", count=" + count +
                '}';
    }
}
