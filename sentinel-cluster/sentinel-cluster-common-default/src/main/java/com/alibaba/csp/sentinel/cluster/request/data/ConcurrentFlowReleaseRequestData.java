package com.alibaba.csp.sentinel.cluster.request.data;

/**
 * @author yunfeiyanggzq
 * @Date 2020/7/9 14:44
 */
public class ConcurrentFlowReleaseRequestData {

    private long tokenId;

    public long getTokenId() {
        return tokenId;
    }

    public ConcurrentFlowReleaseRequestData setTokenId(long tokenId) {
        this.tokenId = tokenId;
        return this;
    }
}
