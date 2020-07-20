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
package com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent;

import com.alibaba.csp.sentinel.concurrent.NamedThreadFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * We use a ConcurrentHashMap<long, AtomicInteger> type structure to store current concurrency corresponding to
 * rules, where the key is flowId and the value is concurrency. Because concurrency may be accessed and
 * modified by multiple threads, we consider to design it as an AtomicInteger class .
 * Each newly  created rule will add a concurrency object to this map.
 * If the concurrency corresponding to a rule changes, we will update the corresponding nowCalls in real time.
 * Each request to obtain a token will increase the concurrency;
 * and the request to release the token will reduce the concurrency.
 *
 * @author yunfeiyanggzq
 */
public final class CurrentConcurrencyManager {
    /**
     * use ConcurrentHashMap to store the nowCalls of rules.
     */
    private static final ConcurrentHashMap<Long, AtomicInteger> NOW_CALLS_MAP = new ConcurrentHashMap<Long, AtomicInteger>();

    private static ClusterConcurrentLogListener logTask = null;

    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(1,
            new NamedThreadFactory("sentinel-cluster-concurrency-record-task", true));

    static {
        logTask = new ClusterConcurrentLogListener();
        SCHEDULER.scheduleAtFixedRate(logTask, 0, 1, TimeUnit.SECONDS);
    }

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

    /**
     * check flow id.
     */
    public static boolean containsFlowId(Long flowId) {
        return NOW_CALLS_MAP.containsKey(flowId);
    }

    /**
     * get NOW_CALLS_MAP.
     */
    public static ConcurrentHashMap<Long, AtomicInteger> getConcurrencyMap() {
        return NOW_CALLS_MAP;
    }
}
