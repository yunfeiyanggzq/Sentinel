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
package com.alibaba.csp.sentinel.cluster.flow.rule;

import com.alibaba.csp.sentinel.cluster.flow.statistic.ClusterMetricStatistics;
import com.alibaba.csp.sentinel.cluster.flow.statistic.metric.ClusterMetric;
import com.alibaba.csp.sentinel.cluster.server.ServerConstants;
import com.alibaba.csp.sentinel.cluster.server.connection.ConnectionManager;
import com.alibaba.csp.sentinel.cluster.server.util.ClusterRuleUtil;
import com.alibaba.csp.sentinel.log.RecordLog;
import com.alibaba.csp.sentinel.property.DynamicSentinelProperty;
import com.alibaba.csp.sentinel.property.PropertyListener;
import com.alibaba.csp.sentinel.property.SentinelProperty;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.ClusterFlowConfig;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.concurrent.ConcurrentFlowRule;
import com.alibaba.csp.sentinel.util.AssertUtil;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.csp.sentinel.util.function.Function;
import com.alibaba.csp.sentinel.util.function.Predicate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public final class ClusterConcurrentFlowRuleManager {

    /**
     * The default cluster flow rule property supplier that creates a new dynamic property
     * for a specific namespace to do rule management manually.
     */
    public static final Function<String, SentinelProperty<List<ConcurrentFlowRule>>> DEFAULT_PROPERTY_SUPPLIER =
            new Function<String, SentinelProperty<List<ConcurrentFlowRule>>>() {
                @Override
                public SentinelProperty<List<ConcurrentFlowRule>> apply(String namespace) {
                    return new DynamicSentinelProperty<>();
                }
            };

    /**
     * (flowId, clusterRule)
     */
    private static final Map<Long, ConcurrentFlowRule> CONCURRENT_FLOW_RULES = new ConcurrentHashMap<>();
    /**
     * (namespace, [flowId...])
     */
    private static final Map<String, Set<Long>> NAMESPACE_FLOW_ID_MAP = new ConcurrentHashMap<>();
    /**
     * <p>This map (flowId, namespace) is used for getting connected count
     * when checking a specific rule in {@code ruleId}:</p>
     *
     * <pre>
     * ruleId -> namespace -> connection group -> connected count
     * </pre>
     */
    private static final Map<Long, String> FLOW_NAMESPACE_MAP = new ConcurrentHashMap<>();

    /**
     * (namespace, property-listener wrapper)
     */
    private static final Map<String, NamespaceFlowProperty<ConcurrentFlowRule>> PROPERTY_MAP = new ConcurrentHashMap<>();
    /**
     * Cluster flow rule property supplier for a specific namespace.
     */
    private static volatile Function<String, SentinelProperty<List<ConcurrentFlowRule>>> propertySupplier
            = DEFAULT_PROPERTY_SUPPLIER;

    private static final Object UPDATE_LOCK = new Object();

    static {
        initDefaultProperty();
    }

    private static void initDefaultProperty() {
        // The server should always support default namespace,
        // so register a default property for default namespace.
        SentinelProperty<List<ConcurrentFlowRule>> defaultProperty = new DynamicSentinelProperty<>();
        String defaultNamespace = ServerConstants.DEFAULT_NAMESPACE;
        registerPropertyInternal(defaultNamespace, defaultProperty);
    }

    public static void setPropertySupplier(Function<String, SentinelProperty<List<ConcurrentFlowRule>>> propertySupplier) {
        AssertUtil.notNull(propertySupplier, "concurrent flow rule property supplier cannot be null");
        ClusterConcurrentFlowRuleManager.propertySupplier = propertySupplier;
    }

    /**
     * Listen to the {@link SentinelProperty} for cluster {@link FlowRule}s.
     * The property is the source of cluster {@link FlowRule}s for a specific namespace.
     *
     * @param namespace namespace to register
     */
    public static void register2Property(String namespace) {
        AssertUtil.notEmpty(namespace, "namespace cannot be empty");
        if (propertySupplier == null) {
            RecordLog.warn(
                    "[ClusterConcurrentFlowRuleManager] Cluster flow property supplier is absent, cannot register property");
            return;
        }
        SentinelProperty<List<ConcurrentFlowRule>> property = propertySupplier.apply(namespace);
        if (property == null) {
            RecordLog.warn(
                    "[ClusterConcurrentFlowRuleManager] Wrong created property from cluster flow property supplier, ignoring");
            return;
        }
        synchronized (UPDATE_LOCK) {
            RecordLog.info("[ClusterConcurrentFlowRuleManager] Registering new property to cluster flow rule manager"
                    + " for namespace <{}>", namespace);
            registerPropertyInternal(namespace, property);
        }
    }

    /**
     * Listen to the {@link SentinelProperty} for cluster {@link FlowRule}s if current property for namespace is absent.
     * The property is the source of cluster {@link FlowRule}s for a specific namespace.
     *
     * @param namespace namespace to register
     */
    public static void registerPropertyIfAbsent(String namespace) {
        AssertUtil.notEmpty(namespace, "namespace cannot be empty");
        if (!PROPERTY_MAP.containsKey(namespace)) {
            synchronized (UPDATE_LOCK) {
                if (!PROPERTY_MAP.containsKey(namespace)) {
                    register2Property(namespace);
                }
            }
        }
    }

    private static void registerPropertyInternal(/*@NonNull*/ String namespace, /*@Valid*/
                                                              SentinelProperty<List<ConcurrentFlowRule>> property) {
        NamespaceFlowProperty<ConcurrentFlowRule> oldProperty = PROPERTY_MAP.get(namespace);
        if (oldProperty != null) {
            oldProperty.getProperty().removeListener(oldProperty.getListener());
        }
        PropertyListener<List<ConcurrentFlowRule>> listener = new FlowRulePropertyListener(namespace);
        property.addListener(listener);
        PROPERTY_MAP.put(namespace, new NamespaceFlowProperty<>(namespace, property, listener));
        Set<Long> flowIdSet = NAMESPACE_FLOW_ID_MAP.get(namespace);
        if (flowIdSet == null) {
            resetNamespaceFlowIdMapFor(namespace);
        }
    }

    /**
     * Remove cluster flow rule property for a specific namespace.
     *
     * @param namespace valid namespace
     */
    public static void removeProperty(String namespace) {
        AssertUtil.notEmpty(namespace, "namespace cannot be empty");
        synchronized (UPDATE_LOCK) {
            NamespaceFlowProperty<ConcurrentFlowRule> property = PROPERTY_MAP.get(namespace);
            if (property != null) {
                property.getProperty().removeListener(property.getListener());
                PROPERTY_MAP.remove(namespace);
            }
            RecordLog.info("[ClusterFlowRuleManager] Removing property from cluster flow rule manager"
                    + " for namespace <{}>", namespace);
        }
    }

    private static void removePropertyListeners() {
        for (NamespaceFlowProperty<ConcurrentFlowRule> property : PROPERTY_MAP.values()) {
            property.getProperty().removeListener(property.getListener());
        }
    }

    private static void restorePropertyListeners() {
        for (NamespaceFlowProperty<ConcurrentFlowRule> p : PROPERTY_MAP.values()) {
            p.getProperty().removeListener(p.getListener());
            p.getProperty().addListener(p.getListener());
        }
    }

    /**
     * Get flow rule by rule ID.
     *
     * @param id rule ID
     * @return flow rule
     */
    public static ConcurrentFlowRule getFlowRuleById(Long id) {
        if (!ClusterRuleUtil.validId(id)) {
            return null;
        }
        return CONCURRENT_FLOW_RULES.get(id);
    }

    public static Set<Long> getFlowIdSet(String namespace) {
        if (StringUtil.isEmpty(namespace)) {
            return new HashSet<>();
        }
        Set<Long> set = NAMESPACE_FLOW_ID_MAP.get(namespace);
        if (set == null) {
            return new HashSet<>();
        }
        return new HashSet<>(set);
    }

    public static List<ConcurrentFlowRule> getAllFlowRules() {
        return new ArrayList<>(CONCURRENT_FLOW_RULES.values());
    }

    /**
     * Get all cluster flow rules within a specific namespace.
     *
     * @param namespace valid namespace
     * @return cluster flow rules within the provided namespace
     */
    public static List<ConcurrentFlowRule> getFlowRules(String namespace) {
        if (StringUtil.isEmpty(namespace)) {
            return new ArrayList<>();
        }
        List<ConcurrentFlowRule> rules = new ArrayList<>();
        Set<Long> flowIdSet = NAMESPACE_FLOW_ID_MAP.get(namespace);
        if (flowIdSet == null || flowIdSet.isEmpty()) {
            return rules;
        }
        for (Long flowId : flowIdSet) {
            ConcurrentFlowRule rule = CONCURRENT_FLOW_RULES.get(flowId);
            if (rule != null) {
                rules.add(rule);
            }
        }
        return rules;
    }

    /**
     * Load flow rules for a specific namespace. The former rules of the namespace will be replaced.
     *
     * @param namespace a valid namespace
     * @param rules     rule list
     */
    public static void loadRules(String namespace, List<ConcurrentFlowRule> rules) {
        AssertUtil.notEmpty(namespace, "namespace cannot be empty");
        NamespaceFlowProperty<ConcurrentFlowRule> property = PROPERTY_MAP.get(namespace);
        if (property != null) {
            property.getProperty().updateValue(rules);
        }
    }

    private static void resetNamespaceFlowIdMapFor(/*@Valid*/ String namespace) {
        NAMESPACE_FLOW_ID_MAP.put(namespace, new HashSet<Long>());
    }

    /**
     * Clear all rules of the provided namespace and reset map.
     *
     * @param namespace valid namespace
     */
    private static void clearAndResetRulesFor(/*@Valid*/ String namespace) {
        Set<Long> flowIdSet = NAMESPACE_FLOW_ID_MAP.get(namespace);
        if (flowIdSet != null && !flowIdSet.isEmpty()) {
            for (Long flowId : flowIdSet) {
                CONCURRENT_FLOW_RULES.remove(flowId);
                FLOW_NAMESPACE_MAP.remove(flowId);
            }
            flowIdSet.clear();
        } else {
            resetNamespaceFlowIdMapFor(namespace);
        }
    }

    private static void clearAndResetRulesConditional(/*@Valid*/ String namespace, Predicate<Long> predicate) {
        Set<Long> oldIdSet = NAMESPACE_FLOW_ID_MAP.get(namespace);
        if (oldIdSet != null && !oldIdSet.isEmpty()) {
            for (Long flowId : oldIdSet) {
                if (predicate.test(flowId)) {
                    CONCURRENT_FLOW_RULES.remove(flowId);
                    FLOW_NAMESPACE_MAP.remove(flowId);
                    ClusterMetricStatistics.removeMetric(flowId);
                }
            }
            oldIdSet.clear();
        }
    }

    /**
     * Get connected count for associated namespace of given {@code flowId}.
     *
     * @param flowId unique flow ID
     * @return connected count
     */
    public static int getConnectedCount(long flowId) {
        if (flowId <= 0) {
            return 0;
        }
        String namespace = FLOW_NAMESPACE_MAP.get(flowId);
        if (namespace == null) {
            return 0;
        }
        return ConnectionManager.getConnectedCount(namespace);
    }

    public static String getNamespace(long flowId) {
        return FLOW_NAMESPACE_MAP.get(flowId);
    }

    private static void applyClusterFlowRule(List<ConcurrentFlowRule> list, /*@Valid*/ String namespace) {
        if (list == null || list.isEmpty()) {
            clearAndResetRulesFor(namespace);
            return;
        }
        final ConcurrentHashMap<Long, ConcurrentFlowRule> ruleMap = new ConcurrentHashMap<>();

        Set<Long> flowIdSet = new HashSet<>();

        for (ConcurrentFlowRule rule : list) {
            if (!rule.isClusterMode()) {
                continue;
            }
//            if (!FlowRuleUtil.isValidRule(rule)) {
//                RecordLog.warn(
//                    "[ClusterFlowRuleManager] Ignoring invalid flow rule when loading new flow rules: " + rule);
//                continue;
//            }
            if (StringUtil.isBlank(rule.getLimitApp())) {
                rule.setLimitApp(RuleConstant.LIMIT_APP_DEFAULT);
            }

            // Flow id should not be null after filtered.
            ClusterFlowConfig clusterConfig = rule.getClusterConfig();
            Long flowId = clusterConfig.getFlowId();
            if (flowId == null) {
                continue;
            }
            ruleMap.put(flowId, rule);
            FLOW_NAMESPACE_MAP.put(flowId, namespace);
            flowIdSet.add(flowId);

            // Prepare cluster metric from valid flow ID.
            ClusterMetricStatistics.putMetricIfAbsent(flowId,
                    new ClusterMetric(clusterConfig.getSampleCount(), clusterConfig.getWindowIntervalMs()));
        }

        // Cleanup unused cluster metrics.
        clearAndResetRulesConditional(namespace, new Predicate<Long>() {
            @Override
            public boolean test(Long flowId) {
                return !ruleMap.containsKey(flowId);
            }
        });

        CONCURRENT_FLOW_RULES.putAll(ruleMap);
        NAMESPACE_FLOW_ID_MAP.put(namespace, flowIdSet);
    }

    private static final class FlowRulePropertyListener implements PropertyListener<List<ConcurrentFlowRule>> {

        private final String namespace;

        public FlowRulePropertyListener(String namespace) {
            this.namespace = namespace;
        }

        @Override
        public synchronized void configUpdate(List<ConcurrentFlowRule> conf) {
            applyClusterFlowRule(conf, namespace);
            RecordLog.info("[ClusterFlowRuleManager] Cluster flow rules received for namespace <{}>: {}",
                    namespace, CONCURRENT_FLOW_RULES);
        }

        @Override
        public synchronized void configLoad(List<ConcurrentFlowRule> conf) {
            applyClusterFlowRule(conf, namespace);
            RecordLog.info("[ClusterFlowRuleManager] Cluster flow rules loaded for namespace <{}>: {}",
                    namespace, CONCURRENT_FLOW_RULES);
        }
    }

    private ClusterConcurrentFlowRuleManager() {
    }
}
