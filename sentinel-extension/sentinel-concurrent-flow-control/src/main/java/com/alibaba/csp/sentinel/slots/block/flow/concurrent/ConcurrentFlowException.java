package com.alibaba.csp.sentinel.slots.block.flow.concurrent;

import com.alibaba.csp.sentinel.slots.block.BlockException;

/**
 * @Author: yunfeiyanggzq
 * @Date: 2020/7/3 19:53
 */
public class ConcurrentFlowException extends BlockException {
    public ConcurrentFlowException(String ruleLimitApp) {
        super(ruleLimitApp);
    }

    public ConcurrentFlowException(String ruleLimitApp, ConcurrentFlowRule rule) {
        super(ruleLimitApp, rule);
    }

    public ConcurrentFlowException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConcurrentFlowException(String ruleLimitApp, String message) {
        super(ruleLimitApp, message);
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }

    /**
     * Get triggered rule.
     * Note: the rule result is a reference to rule map and SHOULD NOT be modified.
     *
     * @return triggered rule
     * @since 1.4.2
     */
    @Override
    public ConcurrentFlowRule getRule() {
        return rule.as(ConcurrentFlowRule.class);
    }
}
