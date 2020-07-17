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
package com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent.expire;

import com.alibaba.csp.sentinel.cluster.flow.rule.ClusterFlowRuleManager;
import com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent.CurrentConcurrencyManager;
import com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent.TokenCacheNode;
import com.alibaba.csp.sentinel.cluster.server.connection.ConnectionManager;
import com.alibaba.csp.sentinel.log.RecordLog;
import com.alibaba.csp.sentinel.util.AssertUtil;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
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

    private final int MULTIPLE = 1;

    /**
     * the biggest number of token will be deleted in a clear process.
     */
    private final long DEFAULT_EXECUTE_COUNT = 1000;

    /**
     * the biggest time execute in a clear process.
     */
    private final long DEFAULT_EXECUTE_DURATION = 600;

    /**
     * the rate of clear process work.
     */
    private final long DEFAULT_EXECUTE_RATE = 2000;

    public RegularExpireStrategy() {
        this.executeCount = DEFAULT_EXECUTE_COUNT;
        this.executeDuration = DEFAULT_EXECUTE_DURATION;
        this.executeRate = DEFAULT_EXECUTE_RATE;
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
            try {
                clearToken();
            } catch (Throwable e) {
                e.printStackTrace();
                RecordLog.warn("[RegularExpireStrategy] undefined throwable<{}> during clear token", e);
            }
        }
    }

    private void clearToken() {
        long start = System.currentTimeMillis();
        List<Long> keyList = new ArrayList<>(localCache.keySet());
        for (int i = 0; i < executeCount && i < keyList.size(); i++) {
            // time out execution exit
            if (System.currentTimeMillis() - start > executeDuration) {
                RecordLog.info("[RegularExpireStrategy] End the process of expired token detection because of execute time is more than executeDuration<{}>", executeDuration);
                break;
            }
            // use ConcurrentLinkedHashMap to improve the expiration detection progress
            Long key = keyList.get(i);
            TokenCacheNode node = localCache.get(key);
            if (node == null) {
                continue;
            }
            if (!ConnectionManager.isClientOnline(node.getClientAddress()) && node.getClientTimeout() - System.currentTimeMillis() < 0) {
                removeToken(key, node);
                RecordLog.info("[RegularExpireStrategy] Delete the expired token<{}> because of client offline", node);
                continue;
            }

            // If we find that token's save time is much longer than the client's call resource timeout time, token will be determined to timeout and the client go wrong
            long resourceTimeout = ClusterFlowRuleManager.getFlowRuleById(node.getFlowId()).getClusterConfig().getResourceTimeout();
            if (System.currentTimeMillis() - node.getResourceTimeout() > MULTIPLE * resourceTimeout) {
                removeToken(key, node);
                RecordLog.info("[RegularExpireStrategy] Delete the expired token<{}> because of resource timeout", node);
            }
        }
    }

    private void removeToken(long tokenId, TokenCacheNode node) {
        if (localCache.remove(tokenId) == null) {
            return;
        }
        AtomicInteger nowCalls = CurrentConcurrencyManager.get(node.getFlowId());
        if (nowCalls == null) {
            return;
        }
        nowCalls.getAndAdd((int) node.getAcquireCount() * -1);
    }
}
