package com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;

public interface ExpireStrategy<K, V> {
    /**
     * clean expireKey-Value
     *
     * @return the number of the key cleaned
     */
    int removeExpireKey(ConcurrentLinkedHashMap<Long,TokenCacheNode> map);
}
