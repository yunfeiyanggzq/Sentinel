package com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * We use a ConcurrentHashMap<Long, AtomicInteger> type structure to store nowCalls corresponding to
 * rules, where the key is flowId and the value is nowCalls. Because nowCalls may be accessed and
 * modified by multiple threads, we consider to design it as an AtomicInteger class or modified by
 * the valotile keyword. Each newly created rule will add a nowCalls object to this map. If the
 * concurrency corresponding to a rule changes, we will update the corresponding nowCalls in real time.
 * Each request to obtain a token will increase the nowCalls; and the request to release the token
 * will reduce the nowCalls.
 */
public final class NowCallsManager {
    /**
     * use ConcurrentHashMap to store the nowCalls of rules.
     */
    private static final ConcurrentHashMap<Long, AtomicInteger> NOW_CALLS_MAP = new ConcurrentHashMap<Long, AtomicInteger>();

    /**
     * update the nowCalls.
     */
    public static Boolean update(Long flowId, Integer count) {

        AtomicInteger nowCalls = NOW_CALLS_MAP.get(flowId);
        if (nowCalls == null) {
            return false;
        }
        nowCalls.getAndAdd(count);
        return true;
    }

    /**
     * get the nowCalls.
     */
    public static AtomicInteger get(Long flowId) {
        return NOW_CALLS_MAP.get(flowId);
    }

    /**
     * delete the nowCalls.
     */
    public static void remove(Long flowId) {
        NOW_CALLS_MAP.remove(flowId);
    }

    /**
     * add the nowCalls.
     */
    public static void put(Long flowId, Integer nowCalls) {
        NOW_CALLS_MAP.put(flowId, new AtomicInteger(nowCalls));
    }
}
