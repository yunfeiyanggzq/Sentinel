package com.alibaba.csp.sentinel.cluster.response.data;

/**
 * @Author:yunfeiyanggzq
 * @Date:2020/6/3017:06
 */
public class ConcurrentFlowTokenResponseData {
    private long tokenId;

    public long getTokenId() {
        return tokenId;
    }

    public ConcurrentFlowTokenResponseData setTokenId(long tokenId) {
        this.tokenId = tokenId;
        return this;
    }
}
