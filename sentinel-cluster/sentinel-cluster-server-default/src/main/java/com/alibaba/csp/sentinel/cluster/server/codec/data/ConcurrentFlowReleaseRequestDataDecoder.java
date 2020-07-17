package com.alibaba.csp.sentinel.cluster.server.codec.data;

import com.alibaba.csp.sentinel.cluster.codec.EntityDecoder;
import com.alibaba.csp.sentinel.cluster.request.data.ConcurrentFlowAcquireRequestData;
import com.alibaba.csp.sentinel.cluster.request.data.ConcurrentFlowReleaseRequestData;
import io.netty.buffer.ByteBuf;

/**
 * @author yunfeiyanggzq
 */
public class ConcurrentFlowReleaseRequestDataDecoder implements EntityDecoder<ByteBuf, ConcurrentFlowReleaseRequestData> {
    @Override
    public ConcurrentFlowReleaseRequestData decode(ByteBuf source) {
        ConcurrentFlowReleaseRequestData requestData = new ConcurrentFlowReleaseRequestData().setTokenId(source.readLong());
        return  requestData;
    }
}
