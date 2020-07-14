package com.alibaba.csp.sentinel.cluster.server.processor;

import com.alibaba.csp.sentinel.cluster.ClusterConstants;
import com.alibaba.csp.sentinel.cluster.TokenResult;
import com.alibaba.csp.sentinel.cluster.TokenService;
import com.alibaba.csp.sentinel.cluster.annotation.RequestType;
import com.alibaba.csp.sentinel.cluster.request.ClusterRequest;
import com.alibaba.csp.sentinel.cluster.request.data.ConcurrentFlowReleaseRequestData;
import com.alibaba.csp.sentinel.cluster.response.ClusterResponse;
import com.alibaba.csp.sentinel.cluster.response.data.ConcurrentFlowReleaseResponseData;
import com.alibaba.csp.sentinel.cluster.server.TokenServiceProvider;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author yunfeiyanggzq
 */

@RequestType(ClusterConstants.MSG_TYPE_CONCURRENT_FLOW_RELEASE)
public class ConcurrentFlowRequestReleaseProcessor implements RequestProcessor<ConcurrentFlowReleaseRequestData, ConcurrentFlowReleaseResponseData> {
    @Override
    public ClusterResponse<ConcurrentFlowReleaseResponseData> processRequest(ChannelHandlerContext ctx, ClusterRequest<ConcurrentFlowReleaseRequestData> request) {
        TokenService tokenService = TokenServiceProvider.getService();
        long tokenId = request.getData().getTokenId();
        TokenResult result = tokenService.releaseConcurrentToken(tokenId);
        return toResponse(result, request);
    }

    private ClusterResponse<ConcurrentFlowReleaseResponseData> toResponse(TokenResult result, ClusterRequest request) {
        return new ClusterResponse<>(request.getId(), request.getType(), result.getStatus(), null);
    }
}

