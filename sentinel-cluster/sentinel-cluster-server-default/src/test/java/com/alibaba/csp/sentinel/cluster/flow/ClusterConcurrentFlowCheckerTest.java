package com.alibaba.csp.sentinel.cluster.flow;

import com.alibaba.csp.sentinel.cluster.TokenResult;
import com.alibaba.csp.sentinel.cluster.TokenResultStatus;
import com.alibaba.csp.sentinel.cluster.flow.rule.ConcurrentFlowRuleManager;
import com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent.ConcurrentFlowRule;
import com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent.NowCallsManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ClusterConcurrentFlowCheckerTest {
    ConcurrentFlowRule rule = null;

    @Before
    public void prepare() {
        rule = new ConcurrentFlowRule();
        rule.setClientTimeout(500L);
        rule.setSourceTimeout(100L);
        rule.setConcurrencyLevel(3000);
        rule.setFlowId(111L);
        ConcurrentFlowRuleManager.addFlowRule(111L, rule);
    }

    @Test
    public void testAcquireAndRelease() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(100000);
        ExecutorService pool = Executors.newFixedThreadPool(10000);
        final AtomicInteger sum = new AtomicInteger(0);
        for (long i = 0; i < 100000; i++) {
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep((long) (Math.random() * 3000));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    TokenResult tokenResult = ClusterConcurrentFlowChecker.acquireClusterToken(rule, 1, false);
                    if (tokenResult.getStatus() == TokenResultStatus.BLOCKED) {
                        countDownLatch.countDown();
                        return;
                    }
                    sum.incrementAndGet();
                    try {
                        Thread.sleep((long) (Math.random() * 1000));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    ClusterConcurrentFlowChecker.releaseClusterToken(tokenResult.getTokenId());
                    countDownLatch.countDown();
                }
            };
            pool.execute(task);
        }
        countDownLatch.await();
        while ((NowCallsManager.get(111L).get() != 0)) {
            Thread.sleep(100);
        }
        Assert.assertEquals(0, ClusterConcurrentFlowChecker.getSize());
        Assert.assertEquals("token pass must be the sum of expired token and released token",
                rule.getReleaseCount().get() + rule.getExpireCount(), sum.get());
    }

    @Test
    public void testKeepToken() {
        TokenResult tokenResult = ClusterConcurrentFlowChecker.acquireClusterToken(rule, 1, false);
        ClusterConcurrentFlowChecker.keepClusterToken(tokenResult.getTokenId());
    }
}
