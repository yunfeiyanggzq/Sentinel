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

import com.alibaba.csp.sentinel.context.Context;
import com.alibaba.csp.sentinel.node.DefaultNode;
import com.alibaba.csp.sentinel.slotchain.AbstractLinkedProcessorSlot;
import com.alibaba.csp.sentinel.slotchain.ResourceWrapper;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.spi.SpiOrder;
import com.alibaba.csp.sentinel.util.AssertUtil;
import com.alibaba.csp.sentinel.util.function.Function;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @Author: yunfeiyanggzq
 * @Date: 2020/7/3 17:33
 */
@SpiOrder(-2500)
public class ConcurrentFlowSlot extends AbstractLinkedProcessorSlot<DefaultNode> {

    private final ConcurrentFlowRuleChecker checker;

    public ConcurrentFlowSlot() {
        this(new ConcurrentFlowRuleChecker());
    }

    ConcurrentFlowSlot(ConcurrentFlowRuleChecker checker) {
        AssertUtil.notNull(checker, "flow checker should not be null");
        this.checker = checker;
    }

    @Override
    public void entry(Context context, ResourceWrapper resourceWrapper, DefaultNode node, int count,
                      boolean prioritized, Object... args) throws Throwable {
        checkFlow(resourceWrapper, context, node, count, prioritized);
        fireEntry(context, resourceWrapper, node, count, prioritized, args);
    }

    void checkFlow(ResourceWrapper resource, Context context, DefaultNode node, int count, boolean prioritized)
            throws BlockException {
        checker.checkFlow(ruleProvider, resource, context, node, count, prioritized);
    }

    @Override
    public void exit(Context context, ResourceWrapper resourceWrapper, int count, Object... args) {
        checker.releaseFlow(context, resourceWrapper, count);
        fireExit(context, resourceWrapper, count, args);
    }

    private final Function<String, Collection<ConcurrentFlowRule>> ruleProvider = new Function<String, Collection<ConcurrentFlowRule>>() {
        @Override
        public Collection<ConcurrentFlowRule> apply(String resource) {
            // Flow rule map should not be null.
            Map<String, List<ConcurrentFlowRule>> flowRules = ConcurrentFlowRuleManager.getFlowRuleMap();
            return flowRules.get(resource);
        }
    };
}
