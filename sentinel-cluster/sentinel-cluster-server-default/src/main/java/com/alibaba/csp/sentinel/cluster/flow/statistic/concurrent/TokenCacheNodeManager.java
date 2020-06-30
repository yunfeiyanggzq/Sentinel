package com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent;

import com.alibaba.csp.sentinel.cluster.flow.statistic.expire.ExpireStrategy;
import com.alibaba.csp.sentinel.cluster.flow.statistic.expire.RegularExpireStrategy;
import com.alibaba.csp.sentinel.util.AssertUtil;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.googlecode.concurrentlinkedhashmap.Weighers;

public class TokenCacheNodeManager {
    // storage structure
    private ConcurrentLinkedHashMap<Long, TokenCacheNode> TOKEN_CACHE_NODE_MAP;
    // expire strategy
    private ExpireStrategy expireStrategy;

    private final int DEFAULT_CONCURRENCY_LEVEL = 16;
    private final int DEFAULT_CAPACITY = Integer.MAX_VALUE;
    private final long DEFAULT_EXECUTE_COUNT = 1000;
    private final long DEFAULT_EXECUTE_DURATION = 600;
    private final long DEFAULT_EXECUTE_RATE = 1000;

    public void prepare(int concurrencyLevel, int maximumWeightedCapacity, ExpireStrategy expireStrategy) {
        AssertUtil.isTrue(concurrencyLevel > 0, "concurrencyLevel must be positive");
        AssertUtil.isTrue(maximumWeightedCapacity > 0, "maximumWeightedCapacity must be positive");
        AssertUtil.isTrue(expireStrategy != null, "expireStrategy can;t be null");

        this.TOKEN_CACHE_NODE_MAP = new ConcurrentLinkedHashMap.Builder<Long, TokenCacheNode>()
                .concurrencyLevel(concurrencyLevel)
                .maximumWeightedCapacity(maximumWeightedCapacity)
                .weigher(Weighers.singleton())
                .build();
        // Start the task of regularly clearing expired keys
        this.expireStrategy = expireStrategy;
        this.expireStrategy.removeExpireKey(TOKEN_CACHE_NODE_MAP);
    }

    public TokenCacheNodeManager() {
        ExpireStrategy expireStrategy = new RegularExpireStrategy(DEFAULT_EXECUTE_COUNT, DEFAULT_EXECUTE_DURATION, DEFAULT_EXECUTE_RATE);
        prepare(DEFAULT_CONCURRENCY_LEVEL, DEFAULT_CAPACITY, expireStrategy);
    }

    public TokenCacheNode getTokenCacheNode(long tokenId) {
        return TOKEN_CACHE_NODE_MAP.get(tokenId);
    }

    public void putTokenCacheNode(long tokenId, TokenCacheNode cacheNode) {
        TOKEN_CACHE_NODE_MAP.put(tokenId, cacheNode);
    }

    public Boolean isContainsTokenId(long tokenId) {
        return TOKEN_CACHE_NODE_MAP.containsKey(tokenId);
    }

    public TokenCacheNode removeTokenCacheNode(long tokenId) {
        return TOKEN_CACHE_NODE_MAP.remove(tokenId);
    }

    public int getSize() {
        return TOKEN_CACHE_NODE_MAP.size();
    }

    public ConcurrentLinkedHashMap<Long, TokenCacheNode> getCache(){
        return this.TOKEN_CACHE_NODE_MAP;
    }
}
