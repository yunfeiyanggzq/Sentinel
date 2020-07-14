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
package com.alibaba.csp.sentinel.slots.block.flow.concurrent;

import com.alibaba.csp.sentinel.log.RecordLog;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.csp.sentinel.util.function.Function;
import com.alibaba.csp.sentinel.util.function.Predicate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yunfeiyanggzq
 */
public class ConcurrentFlowRuleUtil {
    /**
     * Build the flow rule map from raw list of flow rules, grouping by resource name.
     *
     * @param list raw list of flow rules
     * @return constructed new flow rule map; empty map if list is null or empty, or no valid rules
     */
    public static Map<String, List<ConcurrentFlowRule>> buildFlowRuleMap(List<ConcurrentFlowRule> list) {
        return buildFlowRuleMap(list, null);
    }

    /**
     * Build the flow rule map from raw list of flow rules, grouping by resource name.
     *
     * @param list   raw list of flow rules
     * @param filter rule filter
     * @return constructed new flow rule map; empty map if list is null or empty, or no wanted rules
     */
    public static Map<String, List<ConcurrentFlowRule>> buildFlowRuleMap(List<ConcurrentFlowRule> list, Predicate<ConcurrentFlowRule> filter) {
        return buildFlowRuleMap(list, filter, true);
    }

    /**
     * Build the flow rule map from raw list of flow rules, grouping by resource name.
     *
     * @param list       raw list of flow rules
     * @param filter     rule filter
     * @param shouldSort whether the rules should be sorted
     * @return constructed new flow rule map; empty map if list is null or empty, or no wanted rules
     */
    public static Map<String, List<ConcurrentFlowRule>> buildFlowRuleMap(List<ConcurrentFlowRule> list, Predicate<ConcurrentFlowRule> filter,
                                                                         boolean shouldSort) {
        return buildFlowRuleMap(list, extractResource, filter, shouldSort);
    }

    /**
     * Build the flow rule map from raw list of flow rules, grouping by provided group function.
     *
     * @param list          raw list of flow rules
     * @param groupFunction grouping function of the map (by key)
     * @param filter        rule filter
     * @param shouldSort    whether the rules should be sorted
     * @param <K>           type of key
     * @return constructed new flow rule map; empty map if list is null or empty, or no wanted rules
     */
    public static <K> Map<K, List<ConcurrentFlowRule>> buildFlowRuleMap(List<ConcurrentFlowRule> list, Function<ConcurrentFlowRule, K> groupFunction,
                                                                        Predicate<ConcurrentFlowRule> filter, boolean shouldSort) {
        Map<K, List<ConcurrentFlowRule>> newRuleMap = new ConcurrentHashMap<>();
        if (list == null || list.isEmpty()) {
            return newRuleMap;
        }
        Map<K, Set<ConcurrentFlowRule>> tmpMap = new ConcurrentHashMap<>();

        for (ConcurrentFlowRule rule : list) {
            if (!isValidRule(rule)) {
                RecordLog.warn("[ConcurrentFlowRuleManager] Ignoring invalid flow rule when loading new flow rules: " + rule);
                continue;
            }
            if (filter != null && !filter.test(rule)) {
                continue;
            }
            if (StringUtil.isBlank(rule.getLimitApp())) {
                rule.setLimitApp(RuleConstant.LIMIT_APP_DEFAULT);
            }

            K key = groupFunction.apply(rule);
            if (key == null) {
                continue;
            }
            Set<ConcurrentFlowRule> flowRules = tmpMap.get(key);

            if (flowRules == null) {
                // Use hash set here to remove duplicate rules.
                flowRules = new HashSet<>();
                tmpMap.put(key, flowRules);
            }

            flowRules.add(rule);
        }
        Comparator<ConcurrentFlowRule> comparator = new ConcurrentFlowRuleComparator();
        for (Map.Entry<K, Set<ConcurrentFlowRule>> entries : tmpMap.entrySet()) {
            List<ConcurrentFlowRule> rules = new ArrayList<>(entries.getValue());
            if (shouldSort) {
                // Sort the rules.
                Collections.sort(rules, comparator);
            }
            newRuleMap.put(entries.getKey(), rules);
        }

        return newRuleMap;
    }

    /**
     * Check whether provided flow rule is valid.
     *
     * @param rule flow rule to check
     * @return true if valid, otherwise false
     */
    public static boolean isValidRule(ConcurrentFlowRule rule) {
        return rule != null && !StringUtil.isBlank(rule.getResource()) && rule.getClientTimeout() >= 0 && rule.getResourceTimeout() >= 0;

    }

    private static final Function<ConcurrentFlowRule, String> extractResource = new Function<ConcurrentFlowRule, String>() {
        @Override
        public String apply(ConcurrentFlowRule rule) {
            return rule.getResource();
        }
    };
}
