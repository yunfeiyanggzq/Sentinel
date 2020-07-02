package com.alibaba.csp.sentinel.cluster.server.processor;

import com.alibaba.csp.sentinel.cluster.ClusterConstants;
import com.alibaba.csp.sentinel.cluster.TokenResult;
import com.alibaba.csp.sentinel.cluster.TokenService;
import com.alibaba.csp.sentinel.cluster.annotation.RequestType;
import com.alibaba.csp.sentinel.cluster.request.ClusterRequest;
import com.alibaba.csp.sentinel.cluster.request.data.ConcurrentFlowAcquireRequestData;
import com.alibaba.csp.sentinel.cluster.response.ClusterResponse;
import com.alibaba.csp.sentinel.cluster.response.data.ConcurrentFlowTokenResponseData;
import com.alibaba.csp.sentinel.cluster.server.TokenServiceProvider;

/**
 * @Author yunfeiyanggzq
 */
@RequestType(ClusterConstants.MSG_TYPE_CONCURRENT_FLOW_ACQUIRE)
public class ConcurrentFlowRequestAcquireProcessor implements RequestProcessor<ConcurrentFlowAcquireRequestData, ConcurrentFlowTokenResponseData> {
    @Override
    public ClusterResponse processRequest(ClusterRequest<ConcurrentFlowAcquireRequestData> request) {
        System.out.println("并发集群流控请求来了");
        TokenService tokenService = TokenServiceProvider.getService();
        long flowId = request.getData().getFlowId();
        int count = request.getData().getCount();
        boolean prioritized = request.getData().isPriority();

        TokenResult result = tokenService.acquireConcurrentToken(flowId, count, prioritized);
        return toResponse(result, request);
    }

    private ClusterResponse<ConcurrentFlowTokenResponseData> toResponse(TokenResult result, ClusterRequest request) {
        return new ClusterResponse<>(request.getId(), request.getType(), result.getStatus(),
                new ConcurrentFlowTokenResponseData().setTokenId(result.getTokenId())
        );
    }
}
