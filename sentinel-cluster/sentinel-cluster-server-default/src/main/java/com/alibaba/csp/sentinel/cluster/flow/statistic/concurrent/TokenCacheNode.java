package com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent;

import java.util.UUID;

/**
 * We use LocalCache to store the tokenId, whose the underlying storage structure
 * is ConcurrentLinkedHashMap, Its structure is shown in the following figure.
 * Its storage node is CacheNode. In order to operate the nowCalls value when
 * the expired tokenId is deleted regularly, we need to store the flowId in CacheNode.
 * @author yunfeiyanggzq
 */
public class TokenCacheNode {
    /**
     * the TokenId of the token
     */
    private Long tokenId;
    /**
     * the client goes offline detection time
     */
    private Long clientTimeout;
    /**
     * the resource called over time detection time
     */
    private Long sourceTimeout;
    /**
     * the flow rule id  corresponding to the token
     */
    private Long flowId;
    /**
     * the number this token occupied
     */
    private int acquireCount;

    TokenCacheNode() {
    }

    public static TokenCacheNode generateTokenCacheNode(ConcurrentFlowRule rule, int acquireCount) {
        TokenCacheNode node = new TokenCacheNode();
        long tokenId = UUID.randomUUID().getMostSignificantBits();
        node.setTokenId(tokenId);
        node.setFlowId(rule.getFlowId());
        node.setClientTimeout(rule.getClientTimeout());
        node.getSourceTimeout(rule.getSourceTimeout());
        node.setAcquireCount(acquireCount);
        return node;
    }

    public Long getTokenId() {
        return tokenId;
    }

    public void setTokenId(Long tokenId) {
        this.tokenId = tokenId;
    }

    public Long getClientTimeout() {
        return clientTimeout;
    }

    public void setClientTimeout(Long clientTimeout) {
        this.clientTimeout = clientTimeout + System.currentTimeMillis();
    }

    public Long getSourceTimeout(long sourceTimeout) {
        return this.sourceTimeout;
    }

    public void setSourceTimeout(Long sourceTimeout) {
        this.sourceTimeout = sourceTimeout + System.currentTimeMillis();
    }

    public Long getFlowId() {
        return flowId;
    }

    public void setFlowId(Long flowId) {
        this.flowId = flowId;
    }

    public int getAcquireCount() {
        return acquireCount;
    }

    public void setAcquireCount(int aquireCount) {
        this.acquireCount = aquireCount;
    }

    @Override
    public String toString() {
        return "TokenCacheNode{" +
                "tokenId=" + tokenId +
                ", clientTimeout=" + clientTimeout +
                ", sourceTimeout=" + sourceTimeout +
                ", flowId=" + flowId +
                ", acquireCount=" + acquireCount +
                '}';
    }
}
