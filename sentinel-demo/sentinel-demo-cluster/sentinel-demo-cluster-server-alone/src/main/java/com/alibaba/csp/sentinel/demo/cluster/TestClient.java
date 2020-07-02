package com.alibaba.csp.sentinel.demo.cluster;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.cluster.ClusterStateManager;
import com.alibaba.csp.sentinel.cluster.client.config.ClusterClientAssignConfig;
import com.alibaba.csp.sentinel.cluster.client.config.ClusterClientConfig;
import com.alibaba.csp.sentinel.cluster.client.config.ClusterClientConfigManager;
import com.alibaba.csp.sentinel.cluster.flow.rule.ClusterFlowRuleManager;
import com.alibaba.csp.sentinel.cluster.server.config.ClusterServerConfigManager;
import com.alibaba.csp.sentinel.cluster.server.config.ServerTransportConfig;
import com.alibaba.csp.sentinel.slots.block.ClusterRuleConstant;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.ClusterFlowConfig;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @Author:yunfeiyanggzq
 * @Date:2020/7/113:47
 */
public class TestClient {
    private static void initClusterFlowRule() {
        List<FlowRule> flowRules = new ArrayList<FlowRule>();
        FlowRule flowRule = new FlowRule();
        //指定限流规则的Resource
        flowRule.setResource("a");
        //集群限流规则配置
        ClusterFlowConfig clusterFlowConfig = new ClusterFlowConfig();
        //集群失效是否转移
        clusterFlowConfig.setFallbackToLocalWhenFail(true);
        //指定flowId 可以使用IP+进程号区分
        clusterFlowConfig.setFlowId(1L);
        //限流规则 全局总数或者平均分摊
        clusterFlowConfig.setThresholdType(ClusterRuleConstant.FLOW_THRESHOLD_GLOBAL);
        flowRule.setClusterConfig(clusterFlowConfig);
        //是否启用Cluster模式
        flowRule.setClusterMode(true);
        //默认限流规则，具体可以看FlowSlot介绍
        flowRule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT);
        //总数
        flowRule.setCount(10);
        //策略
        flowRule.setStrategy(RuleConstant.STRATEGY_DIRECT);
        //限制QPS 也可以指定为线程数
        flowRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        flowRules.add(flowRule);
        //加载配置
        FlowRuleManager.loadRules(flowRules);
        //注册NameSpace
        ClusterFlowRuleManager.registerPropertyIfAbsent("1-name");
        //NameSpace 下面的规则加载到集群模式
        ClusterFlowRuleManager.loadRules("1-name", flowRules);
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

    private static void setToServer() {
        ClusterStateManager.setToClient();
    }

    public static void main(String[] args) {
        //初始化限流规则
        initClusterFlowRule();
        //初始化Server
        initServer();
        //初始化Cluster Client规则，指定Server ip和端口
        initClusterClientRule();
        //设置为Server 并启动
        ClusterStateManager.setToClient();
        while (true) {
            //等待10秒
            try {
                Thread.sleep(10);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                //对资源A限流
                Entry entry = SphU.entry("a");
                System.out.println("pass");
                entry.exit();
            } catch (Exception e) {
                System.out.println("block");
            }
        }
    }
}


