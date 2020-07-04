package com.alibaba.csp.sentinel.slots.block.flow.concurrent;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;

import java.util.Comparator;

/**
 * Comparator for flow rules.
 *
 * @Author: yunfeiyanggzq
 * @Date: 2020/7/3 19:32
 */

public class ConcurrentFlowRuleComparator implements Comparator<ConcurrentFlowRule> {

    @Override
    public int compare(ConcurrentFlowRule o1, ConcurrentFlowRule o2) {
        // Clustered mode will be on the top.
        if (o1.isClusterMode() && !o2.isClusterMode()) {
            return 1;
        }

        if (!o1.isClusterMode() && o2.isClusterMode()) {
            return -1;
        }

        if (o1.getLimitApp() == null) {
            return 0;
        }

        if (o1.getLimitApp().equals(o2.getLimitApp())) {
            return 0;
        }

        if (RuleConstant.LIMIT_APP_DEFAULT.equals(o1.getLimitApp())) {
            return 1;
        } else if (RuleConstant.LIMIT_APP_DEFAULT.equals(o2.getLimitApp())) {
            return -1;
        } else {
            return 0;
        }
    }

}

