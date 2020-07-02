package com.alibaba.csp.sentinel.cluster.flow.statistic.expire;

import com.alibaba.csp.sentinel.cluster.flow.rule.ConcurrentFlowRuleManager;
import com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent.NowCallsManager;
import com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent.TokenCacheNode;
import com.alibaba.csp.sentinel.util.AssertUtil;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


public class RegularExpireStrategy implements ExpireStrategy {
    // The max number of token deleted each time,
    // the number of expired key-value pairs deleted each time does not exceed this number
    private long executeCount;
    // Length of time for task execution
    private long executeDuration;
    // Frequency of task execution
    private long executeRate;
    // the local cache of tokenId
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
    public int removeExpireKey(ConcurrentLinkedHashMap localCache) {
        AssertUtil.isTrue(localCache != null, " local cache can't be null");
        this.localCache = localCache;

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(new MyTask(), 0, executeRate, TimeUnit.MILLISECONDS);
        return 0;
    }

    private class MyTask implements Runnable {

        @Override
        public void run() {
            long start = System.currentTimeMillis();
            List<Object> keyList = localCache.keySet().stream().collect(Collectors.toList());
            for (int i = 0; i < executeCount && i < keyList.size(); i++) {
                // use ConcurrentLinkedHashMap to improve the expiration detection progress
                Long key = (Long) keyList.get(i);
                TokenCacheNode node = localCache.get(key);
                // If  we find that token's save time is much longer than the client's
                // call resource timeout time, token will be determined to timeout and the client go wrong
                if (node != null && node.getClientTimeout() - System.currentTimeMillis() < 0) {
                    //  communicate with the client to confirm disconnection
                    if (isClientShoutDown()) {
                        // find the corresponding FlowRule to sync nowCalls
                        // remove the token
                        if (localCache.remove(key) == null) {
                            return;
                        }
                        AtomicInteger nowCalls = NowCallsManager.get(node.getFlowId());
                        nowCalls.getAndAdd((int) node.getAcquireCount() * -1);
                        ConcurrentFlowRuleManager.getFlowRule(node.getFlowId()).addExpireCount(node.getAcquireCount());
                    }
                }
                // time out execution exit
                if (System.currentTimeMillis() - start > executeDuration) {
                    break;
                }
            }
        }

    }

    // TODO:implement this function
    public Boolean isClientShoutDown() {
        return true;
    }
}
