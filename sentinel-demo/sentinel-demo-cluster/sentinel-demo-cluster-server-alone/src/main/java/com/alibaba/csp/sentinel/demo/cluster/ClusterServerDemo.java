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
package com.alibaba.csp.sentinel.demo.cluster;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphO;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.cluster.ClusterStateManager;
import com.alibaba.csp.sentinel.cluster.flow.rule.ClusterFlowRuleManager;
import com.alibaba.csp.sentinel.cluster.flow.statistic.ClusterMetricStatistics;
import com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent.CurrentConcurrencyManager;
import com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent.TokenCacheNodeManager;
import com.alibaba.csp.sentinel.cluster.flow.statistic.metric.ClusterMetric;
import com.alibaba.csp.sentinel.cluster.server.ClusterTokenServer;
import com.alibaba.csp.sentinel.cluster.server.SentinelDefaultTokenServer;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.clusterbuilder.ClusterBuilderSlot;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>Cluster server demo (alone mode).</p>
 * <p>Here we init the cluster server dynamic data sources in
 * {@link com.alibaba.csp.sentinel.demo.cluster.init.DemoClusterServerInitFunc}.</p>
 *
 * @author Eric Zhao
 * @since 1.4.0
 */
public class ClusterServerDemo {

    public static void main(String[] args) throws Exception {
        // Not embedded mode by default (alone mode).
        ClusterTokenServer tokenServer = new SentinelDefaultTokenServer();

        // A sample for manually load config for cluster server.
        // It's recommended to use dynamic data source to cluster manage config and rules.
        // See the sample in DemoClusterServerInitFunc for detail.
//        ClusterServerConfigManager.loadGlobalTransportConfig(new ServerTransportConfig()
//            .setIdleSeconds(600)
//            .setPort(11111));
//        ClusterServerConfigManager.loadServerNamespaceSet(Collections.singleton(DemoConstants.APP_NAME));


        // Start the server.
//        tokenServer.start();

        ClusterStateManager.setToServer();
//        final CountDownLatch countDownLatch = new CountDownLatch(100000);
//        ExecutorService pool = Executors.newFixedThreadPool(100);
        ExecutorService pool1 = Executors.newFixedThreadPool(1);
//        final AtomicInteger sum = new AtomicInteger(0);
//        ClusterMetric metric = ClusterMetricStatistics.getMetric(111L);
//        Runnable task1 = new Runnable() {
//            @Override
//            public void run() {
//                while (true){
//                    try {
//                        Thread.sleep(20L);
//                        System.out.println(ClusterBuilderSlot.getClusterNode("cluster-resource").curThreadNum()+"|"+CurrentConcurrencyManager.get(111L) + "|" + TokenCacheNodeManager.getSize() + "|" + ClusterFlowRuleManager.getFlowRuleById(111L));
//
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//
//            }
//        };
//        pool1.execute(task1);
//        for (int i = 0; i < 100000; i++) {
//            Runnable task = new Runnable() {
//                @Override
//                public void run() {
//                    Entry entry = null;
//                    try {
//                        Thread.sleep((long)Math.random()*10000);
//                        entry = SphU.entry("cluster-resource");
//                    } catch (Exception ex) {
//                    } finally {
//                        countDownLatch.countDown();
//                        if (entry != null) {
//                            entry.exit();
//                        }
//                    }
//                }
//            };
//            pool.execute(task);
//        }
//        countDownLatch.await();
//        pool.shutdown();
        final CountDownLatch countDownLatch = new CountDownLatch(10000);
        ExecutorService pool = Executors.newFixedThreadPool(100);
        for (int i=0;i<10000;i++) {
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    Entry entry = null;
                    try {
//                        Thread.sleep((long)Math.random()*100);
                        entry = SphU.entry("cluster-resource");
                        Thread.sleep((long)Math.random()*100);
//                        Thread.sleep((long)Math.random()*100);
                        System.out.println("pass");
                    } catch (Exception ex) {
                        System.out.println("block");
                    } finally {
                        if (entry != null) {
                            entry.exit();
                        }
                        countDownLatch.countDown();
                    }
                }
            };
            pool.execute(task);
        }
        countDownLatch.await();
        pool.shutdown();
    }
}

//java -Dserver.port=8080 -Dcsp.sentinel.dashboard.server=localhost:8080 -Dproject.name=sentinel-dashboard -jar sentinel-dashboard-1.7.2.jar
//curl http://localhost:8719/cluster/server/flowRules
