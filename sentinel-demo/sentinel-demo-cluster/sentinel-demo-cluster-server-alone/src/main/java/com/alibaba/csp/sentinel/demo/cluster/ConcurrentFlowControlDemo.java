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
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.cluster.ClusterStateManager;
import com.alibaba.csp.sentinel.cluster.client.config.ClusterClientAssignConfig;
import com.alibaba.csp.sentinel.cluster.client.config.ClusterClientConfig;
import com.alibaba.csp.sentinel.cluster.client.config.ClusterClientConfigManager;
import com.alibaba.csp.sentinel.cluster.flow.rule.ClusterConcurrentFlowRuleManager;
import com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent.NowCallsManager;
import com.alibaba.csp.sentinel.cluster.server.config.ClusterServerConfigManager;
import com.alibaba.csp.sentinel.cluster.server.config.ServerTransportConfig;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.ClusterRuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.ClusterFlowConfig;
import com.alibaba.csp.sentinel.slots.block.flow.concurrent.ConcurrentFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.concurrent.ConcurrentFlowRuleManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Eric Zhao
 */
public class ConcurrentFlowControlDemo {
    private static void initClusterFlowRule() {
        List<ConcurrentFlowRule> flowRules = new ArrayList<ConcurrentFlowRule>();
        ConcurrentFlowRule flowRule = new ConcurrentFlowRule();
        //集群限流规则配置
        ClusterFlowConfig clusterFlowConfig = new ClusterFlowConfig();
        //集群失效是否转移
        clusterFlowConfig.setFallbackToLocalWhenFail(true);
        //指定flowId 可以使用IP+进程号区分
        clusterFlowConfig.setFlowId(1L);
        //限流规则 全局总数或者平均分摊
        clusterFlowConfig.setThresholdType(ClusterRuleConstant.FLOW_THRESHOLD_GLOBAL);
        flowRule.setClusterConfig(clusterFlowConfig);
        //指定限流规则的Resource
        flowRule.setResource("source");
        //是否启用Cluster模式
        flowRule.setClusterMode(true);
        flowRule.setConcurrencyLevel(500);
        flowRule.setClientTimeout(5000);
        flowRule.setSourceTimeout(5000);
        //默认限流规则，具体可以看FlowSlot介绍
        flowRules.add(flowRule);
        //加载配置
        ConcurrentFlowRuleManager.loadRules(flowRules);
        //注册NameSpace
        ClusterConcurrentFlowRuleManager.registerPropertyIfAbsent("1-name");
        //NameSpace 下面的规则加载到集群模式
        ClusterConcurrentFlowRuleManager.loadRules("1-name", flowRules);
        NowCallsManager.put(1L, 0);
    }

    private static void initServer() {
        //指定提供TokenService的端口和地址
        ServerTransportConfig ServerTransportConfig = new ServerTransportConfig(18730, 600);
        //加载配置
        ClusterServerConfigManager.loadGlobalTransportConfig(ServerTransportConfig);
        Set<String> nameSpaceSet = new HashSet<String>();
        nameSpaceSet.add("1-name");
        //服务配置namespace
        ClusterServerConfigManager.loadServerNamespaceSet(nameSpaceSet);
        //ClusterServerConfigManager.loadGlobalFlowConfig();
        //配置了nameSpace对应的ServerFlowConfig
    }

    private static void initClusterClientRule() {
        //初始化客户端规则
        ClusterClientConfig clusterClientConfig = new ClusterClientConfig();
        //指定获取Token超时时间
        clusterClientConfig.setRequestTimeout(1000);
        //Client指定配置
        ClusterClientConfigManager.applyNewConfig(clusterClientConfig);
        //指定TokenServer的Ip和地址
        ClusterClientAssignConfig clusterClientAssignConfig = new ClusterClientAssignConfig("127.0.0.1", 18730);
        //应用
        ClusterClientConfigManager.applyNewAssignConfig(clusterClientAssignConfig);
    }

    public static void main(String[] args) throws InterruptedException {
        initServer();
        initClusterClientRule();
        initClusterFlowRule();
        ClusterStateManager.setToServer();
        // You will see this in record.log, indicating that the custom slot chain builder is activated:
        // [SlotChainProvider] Global slot chain builder resolved: com.alibaba.csp.sentinel.demo.slot.DemoSlotChainBuilder
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(5000);
        final AtomicInteger sum = new AtomicInteger(0);
        for (long i = 0; i < 1; i++) {
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    Entry entry = null;
                    try {
                        Thread.sleep((long) (Math.random() * 600));
                        entry = SphU.entry("source");
//                        Thread.sleep((long) (Math.random() * 300));
                        System.out.println("pass");
                    } catch (BlockException | InterruptedException ex) {
                        System.out.println("block");
                        ex.printStackTrace();
                    } finally {
                        countDownLatch.countDown();
                        if (entry != null) {
                            entry.exit();
                        }
                    }
                }
            };
            pool.execute(task);
        }
        countDownLatch.await();
        Thread.sleep(1000);
        System.out.println("最终并发量：" + "|" + NowCallsManager.get(1L));
    }
}
