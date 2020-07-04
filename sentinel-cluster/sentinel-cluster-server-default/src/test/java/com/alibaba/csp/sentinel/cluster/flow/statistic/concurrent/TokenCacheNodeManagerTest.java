package com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent;

import com.alibaba.csp.sentinel.cluster.flow.rule.ClusterConcurrentFlowRuleManager;
import com.alibaba.csp.sentinel.cluster.flow.statistic.expire.RegularExpireStrategy;
import com.alibaba.csp.sentinel.slots.block.flow.ClusterFlowConfig;
import com.alibaba.csp.sentinel.slots.block.flow.concurrent.ConcurrentFlowRule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TokenCacheNodeManagerTest {
    TokenCacheNodeManager localCache = null;

    @Before
    public void prepare() {
        RegularExpireStrategy regularExpireStrategy = new RegularExpireStrategy(1000L, 1000L, 1500L);
        this.localCache = new TokenCacheNodeManager();
    }

    @Test
    public void testPutTokenCacheNode() throws InterruptedException {
        ConcurrentFlowRule rule = new ConcurrentFlowRule();
        rule.setClientTimeout(10000L);
        rule.setSourceTimeout(50000L);
        rule.setConcurrencyLevel(50);
        ClusterFlowConfig config=new ClusterFlowConfig();
        config.setFlowId(111L);
        rule.setClusterConfig(config);
        ArrayList<ConcurrentFlowRule> rules=new ArrayList<>();
        rules.add(rule);
        ClusterConcurrentFlowRuleManager.registerPropertyIfAbsent("1-name");
        ClusterConcurrentFlowRuleManager.loadRules("1-name",rules);
        NowCallsManager.put(111L,0);

        final CountDownLatch countDownLatch = new CountDownLatch(5000);
        ExecutorService pool = Executors.newFixedThreadPool(100);

        for (long i = 0; i < 5000; i++) {
            final TokenCacheNode node = TokenCacheNode.generateTokenCacheNode(rule, 1);
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    localCache.putTokenCacheNode(node.getTokenId(), node);
                    countDownLatch.countDown();
                }
            };
            pool.execute(task);
        }
        countDownLatch.await();
        Assert.assertEquals(5000, localCache.getSize());
    }
}
