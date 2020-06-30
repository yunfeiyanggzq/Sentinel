package com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static org.junit.Assert.*;
public class NowCallsManagerTest {
    @Test
    public void updateTest() throws InterruptedException {
        NowCallsManager.put(111L, 0);
        NowCallsManager.put(222L, 0);
        final CountDownLatch countDownLatch=new CountDownLatch(1000);
        ExecutorService pool = Executors.newFixedThreadPool(100);
        for (int i = 0; i < 1000; i++) {
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    NowCallsManager.update(111L, 1);
                    NowCallsManager.update(222L, 2);
                    countDownLatch.countDown();
                }
            };
            pool.execute(task);
        }
        countDownLatch.await();
        assertEquals(1000, NowCallsManager.get(111L).get());
        assertEquals(2000, NowCallsManager.get(222L).get());
    }
}
