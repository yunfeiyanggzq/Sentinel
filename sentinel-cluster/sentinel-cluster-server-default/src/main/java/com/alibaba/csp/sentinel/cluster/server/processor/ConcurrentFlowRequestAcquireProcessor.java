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
package com.alibaba.csp.sentinel.cluster.server.processor;

import com.alibaba.csp.sentinel.cluster.ClusterConstants;
import com.alibaba.csp.sentinel.cluster.TokenResult;
import com.alibaba.csp.sentinel.cluster.TokenResultStatus;
import com.alibaba.csp.sentinel.cluster.TokenService;
import com.alibaba.csp.sentinel.cluster.annotation.RequestType;
import com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent.Queue;
import com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent.RequestObject;
import com.alibaba.csp.sentinel.cluster.request.ClusterRequest;
import com.alibaba.csp.sentinel.cluster.request.data.ConcurrentFlowAcquireRequestData;
import com.alibaba.csp.sentinel.cluster.response.ClusterResponse;
import com.alibaba.csp.sentinel.cluster.response.data.ConcurrentFlowAcquireResponseData;
import com.alibaba.csp.sentinel.cluster.server.TokenServiceProvider;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import io.netty.channel.ChannelHandlerContext;

import java.net.InetSocketAddress;

/**
 * @author yunfeiyanggzq
 */
@RequestType(ClusterConstants.MSG_TYPE_CONCURRENT_FLOW_ACQUIRE)
public class ConcurrentFlowRequestAcquireProcessor implements RequestProcessor<ConcurrentFlowAcquireRequestData, ConcurrentFlowAcquireResponseData> {
    @Override
    public ClusterResponse processRequest(ChannelHandlerContext ctx, ClusterRequest<ConcurrentFlowAcquireRequestData> request) {

        TokenService tokenService = TokenServiceProvider.getService();
        long flowId = request.getData().getFlowId();
        int count = request.getData().getCount();
        boolean prioritized=false;
        String clientAddress = getRemoteAddress(ctx);
        TokenResult result = tokenService.requestConcurrentToken(clientAddress, flowId, count);
        if(result.getStatus()== TokenResultStatus.BLOCKED&&prioritized){
           Queue.addRequestToWaitQueue(new RequestObject(ctx,clientAddress,request));
           return null;
        }
        return toResponse(result, request);
    }

    private ClusterResponse<ConcurrentFlowAcquireResponseData> toResponse(TokenResult result, ClusterRequest request) {
        return new ClusterResponse<>(request.getId(), request.getType(), result.getStatus(),
                new ConcurrentFlowAcquireResponseData().setTokenId(result.getTokenId())
        );
    }

    private String getRemoteAddress(ChannelHandlerContext ctx) {
        if (ctx.channel().remoteAddress() == null) {
            return null;
        }
        InetSocketAddress inetAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        return inetAddress.getAddress().getHostAddress() + ":" + inetAddress.getPort();
    }
}
