package com.alibaba.csp.sentinel.cluster.flow;

import com.alibaba.csp.sentinel.cluster.TokenResult;
import com.alibaba.csp.sentinel.cluster.TokenResultStatus;
import com.alibaba.csp.sentinel.cluster.flow.rule.ClusterConcurrentFlowRuleManager;
import com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent.ConcurrentFlowRule;
import com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent.NowCallsManager;
import com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent.TokenCacheNode;
import com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent.TokenCacheNodeManager;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ClusterConcurrentFlowCheckerTest {
    @Test
    public void testAcquire() throws InterruptedException {
        final ConcurrentFlowRule rule = new ConcurrentFlowRule();
        rule.setClientTimeout(5000L);
        rule.setSourceTimeout(5000L);
        rule.setConcurrencyLevel(100);
        rule.setFlowId(111L);
        ClusterConcurrentFlowRuleManager.addFlowRule(111L, rule);


        final CountDownLatch countDownLatch = new CountDownLatch(10000);
        ExecutorService pool = Executors.newFixedThreadPool(10000);
        final AtomicInteger sum=new AtomicInteger(0);
        for (long i = 0; i < 10000; i++) {
            Runnable task = new Runnable() {

                @Override
                public void run() {
                    try {
                        Thread.sleep((long) (Math.random() *50));
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    TokenResult tokenResult=ClusterConcurrentFLowChecker.acquireClusterToken(rule, 1, false);
                    if(tokenResult.getStatus()==TokenResultStatus.BLOCKED){
                        countDownLatch.countDown();
                        return;
                    }
                    sum.incrementAndGet();
                    try {
                       Thread.sleep((long) (Math.random() *50));
                   }catch (Exception e){
                       e.printStackTrace();
                   }
                    ClusterConcurrentFLowChecker.releaseClusterToken(tokenResult.getTokenId());
                    countDownLatch.countDown();
                }
            };
            pool.execute(task);
        }
        countDownLatch.await();
        System.out.println(sum.get()+"通过");
        System.out.println(NowCallsManager.get(rule.getFlowId())+"flowId"+"|");

    }
}
