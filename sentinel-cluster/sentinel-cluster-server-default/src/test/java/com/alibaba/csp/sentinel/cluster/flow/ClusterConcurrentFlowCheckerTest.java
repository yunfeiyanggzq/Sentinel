package com.alibaba.csp.sentinel.cluster.flow;

import com.alibaba.csp.sentinel.cluster.TokenResult;
import com.alibaba.csp.sentinel.cluster.TokenResultStatus;
import com.alibaba.csp.sentinel.cluster.flow.rule.ClusterConcurrentFlowRuleManager;
import com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent.ConcurrentFlowRule;
import com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent.NowCallsManager;
import org.junit.Test;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ClusterConcurrentFlowCheckerTest {
    @Test
    public void testAcquire() throws InterruptedException {
        final ConcurrentFlowRule rule = new ConcurrentFlowRule();
        rule.setClientTimeout(5000L);
        rule.setSourceTimeout(1000L);
        rule.setConcurrencyLevel(3000);
        rule.setFlowId(111L);
        ClusterConcurrentFlowRuleManager.addFlowRule(111L, rule);
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
                    TokenResult tokenResult = ClusterConcurrentFLowChecker.acquireClusterToken(rule, 1, false);
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

                    ClusterConcurrentFLowChecker.releaseClusterToken(tokenResult.getTokenId());
                    countDownLatch.countDown();
                }
            };
            pool.execute(task);
        }
        countDownLatch.await();
//        System.out.println("添加完成");
        Thread.sleep(10 * 1000);
//        System.out.println(sum.get() + "通过");
//        System.out.println(rule.toString().toString() + "规则情况");
//        System.out.println(ClusterConcurrentFLowChecker.getSize() + "缓存大小");
//        System.out.println(NowCallsManager.get(111L) + "|nowCalls");
    }
}
