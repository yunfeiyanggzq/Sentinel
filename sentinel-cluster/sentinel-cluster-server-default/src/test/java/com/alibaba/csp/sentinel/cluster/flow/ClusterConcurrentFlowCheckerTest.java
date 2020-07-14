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
package com.alibaba.csp.sentinel.cluster.flow;

import com.alibaba.csp.sentinel.cluster.TokenResult;
import com.alibaba.csp.sentinel.cluster.TokenResultStatus;
import com.alibaba.csp.sentinel.cluster.flow.rule.ClusterConcurrentFlowRuleManager;
import com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent.CurrentConcurrencyManager;
import com.alibaba.csp.sentinel.slots.block.flow.ClusterFlowConfig;
import com.alibaba.csp.sentinel.slots.block.flow.concurrent.ConcurrentFlowRule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
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
        rule.setResourceTimeout(100L);
        rule.setConcurrencyLevel(1000);
        rule.setClusterConfig(new ClusterFlowConfig());
        rule.getClusterConfig().setFlowId(111L);
        rule.setClusterMode(true);
        ArrayList<ConcurrentFlowRule> rules = new ArrayList<>();
        rules.add(rule);
        ClusterConcurrentFlowRuleManager.registerPropertyIfAbsent("1-name");
        ClusterConcurrentFlowRuleManager.loadRules("1-name", rules);
        CurrentConcurrencyManager.put(111L, 0);
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
                    TokenResult tokenResult = ClusterConcurrentFlowChecker.acquireClusterToken(null,rule, 1, false);
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
        pool.shutdown();
        while ((CurrentConcurrencyManager.get(111L).get() != 0)) {
            Thread.sleep(100);
        }
        Assert.assertEquals(0, ClusterConcurrentFlowChecker.getSize());
        Assert.assertEquals("token pass must be the sum of expired token and released token",
                rule.getReleaseCount().get() + rule.getExpireCount(), sum.get());
    }

    @Test
    public void testKeepToken() {
        TokenResult tokenResult = ClusterConcurrentFlowChecker.acquireClusterToken(null,rule, 1, false);
        ClusterConcurrentFlowChecker.keepClusterToken(tokenResult.getTokenId());
    }
}
