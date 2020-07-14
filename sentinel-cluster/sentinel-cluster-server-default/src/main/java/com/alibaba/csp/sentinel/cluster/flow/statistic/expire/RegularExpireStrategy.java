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
package com.alibaba.csp.sentinel.cluster.flow.statistic.expire;

import com.alibaba.csp.sentinel.cluster.flow.rule.ClusterConcurrentFlowRuleManager;
import com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent.CurrentConcurrencyManager;
import com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent.TokenCacheNode;
import com.alibaba.csp.sentinel.cluster.server.connection.ConnectionManager;
import com.alibaba.csp.sentinel.log.RecordLog;
import com.alibaba.csp.sentinel.util.AssertUtil;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * We need to consider the situation that the token client goes offline
 * or the resource call times out. It can be detected by sourceTimeout
 * and clientTimeout. The resource calls timeout detection is triggered
 * on the token client. If the resource is called over time, the token
 * client will request the token server to release token or refresh the
 * token. The client offline detection is triggered on the token server.
 * If the offline detection time is exceeded, token server will trigger
 * the detection token client’s status. If the token client is offline,
 * token server will delete the corresponding tokenId. If it is not offline,
 * token server will continue to save it.
 *
 * @author yunfeiyanggzq
 **/
public class RegularExpireStrategy implements ExpireStrategy {
    /**
     * The max number of token deleted each time,
     * the number of expired key-value pairs deleted each time does not exceed this number
     */
    private long executeCount;
    /**
     * Length of time for task execution
     */
    private long executeDuration;
    /**
     * Frequency of task execution
     */
    private long executeRate;
    /**
     * the local cache of tokenId
     */
    private ConcurrentLinkedHashMap<Long, TokenCacheNode> localCache;

    public RegularExpireStrategy(Long executeCount, Long executeDuration, Long executeRate) {
        AssertUtil.isTrue(executeCount != null && executeCount > 0, " executeCount can't be null and should be positive");
        AssertUtil.isTrue(executeDuration != null && executeDuration > 0, " executeDuration can't be null and should be positive");
        AssertUtil.isTrue(executeRate != null && executeRate > 0, " executeRate can't be null and should be positive");
        AssertUtil.isTrue(executeDuration < executeRate, "executeDuration must be smaller than executeRate");

        this.executeCount = executeCount;
        this.executeDuration = executeDuration;
        this.executeRate = executeRate;
    }

    @Override
    public void removeExpireKey(ConcurrentLinkedHashMap localCache) {
        AssertUtil.isTrue(localCache != null, " local cache can't be null");
        this.localCache = localCache;
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(new MyTask(), 0, executeRate, TimeUnit.MILLISECONDS);
    }

    private class MyTask implements Runnable {

        @Override
        public void run() {
            long start = System.currentTimeMillis();
            List<Object> keyList = localCache.keySet().stream().collect(Collectors.toList());
            for (int i = 0; i < executeCount && i < keyList.size(); i++) {
                // time out execution exit
                if (System.currentTimeMillis() - start > executeDuration) {
                    RecordLog.info("[RegularExpireStrategy] End the process of expired token detection because of execute time is more than executeDuration<{}>", executeDuration);
                    break;
                }
                // use ConcurrentLinkedHashMap to improve the expiration detection progress
                Long key = (Long) keyList.get(i);
                TokenCacheNode node = localCache.get(key);
                if (node != null && node.getClientTimeout() - System.currentTimeMillis() < 0) {
                    //  communicate with the client to confirm disconnection
                    if (!ConnectionManager.isClientOnline(node.getClientAddress())) {
                        // find the corresponding FlowRule to sync nowCalls
                        // remove the token
                        removeToken(key, node);
                        RecordLog.info("[RegularExpireStrategy] Delete the expired token<{}> because of client offline", node.getTokenId());
                        continue;
                    }
                }
                // If  we find that token's save time is much longer than the client's
                // call resource timeout time, token will be determined to timeout and the client go wrong
                if (node != null && System.currentTimeMillis() - node.getResourceTimeout() > 3 * ClusterConcurrentFlowRuleManager.getFlowRuleById(node.getFlowId()).getResourceTimeout()) {
                    removeToken(key, node);
                    RecordLog.info("[RegularExpireStrategy] Delete the expired token<{}> because of resource timeout", node.getTokenId());
                }
            }
        }

    }

    private void removeToken(long tokenId, TokenCacheNode node) {
        if (localCache.remove(tokenId) == null) {
            return;
        }
        AtomicInteger nowCalls = CurrentConcurrencyManager.get(node.getFlowId());
        nowCalls.getAndAdd((int) node.getAcquireCount() * -1);
        ClusterConcurrentFlowRuleManager.getFlowRuleById(node.getFlowId()).addExpireCount(node.getAcquireCount());
    }
}
