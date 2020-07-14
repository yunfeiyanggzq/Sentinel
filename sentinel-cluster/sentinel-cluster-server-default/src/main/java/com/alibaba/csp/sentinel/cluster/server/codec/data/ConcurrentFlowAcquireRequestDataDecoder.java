package com.alibaba.csp.sentinel.cluster.server.codec.data;

import com.alibaba.csp.sentinel.cluster.codec.EntityDecoder;
import com.alibaba.csp.sentinel.cluster.request.data.ConcurrentFlowAcquireRequestData;
import com.alibaba.csp.sentinel.cluster.request.data.FlowRequestData;
import io.netty.buffer.ByteBuf;

/**
 * @author yunfeiyanggzq
 */
public class ConcurrentFlowAcquireRequestDataDecoder implements EntityDecoder<ByteBuf, ConcurrentFlowAcquireRequestData> {

    @Override
    public ConcurrentFlowAcquireRequestData decode(ByteBuf source) {
        if (source.readableBytes() >= 12) {
            ConcurrentFlowAcquireRequestData requestData = new ConcurrentFlowAcquireRequestData()
                    .setFlowId(source.readLong())
                    .setCount(source.readInt());
            if (source.readableBytes() >= 1) {
                requestData.setPriority(source.readBoolean());
            }
            return requestData;
        }
        return null;
    }
}
