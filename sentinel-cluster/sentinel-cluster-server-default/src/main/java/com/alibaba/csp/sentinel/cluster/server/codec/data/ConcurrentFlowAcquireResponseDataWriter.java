package com.alibaba.csp.sentinel.cluster.server.codec.data;

import com.alibaba.csp.sentinel.cluster.codec.EntityWriter;
import com.alibaba.csp.sentinel.cluster.response.data.ConcurrentFlowAcquireResponseData;
import io.netty.buffer.ByteBuf;

/**
 * @author yunfeiyanggzq
 */
public class ConcurrentFlowAcquireResponseDataWriter implements EntityWriter<ConcurrentFlowAcquireResponseData, ByteBuf> {
    @Override
    public void writeTo(ConcurrentFlowAcquireResponseData entity, ByteBuf out) {
        // TODO:modify
        out.writeLong(entity.getTokenId());
    }
}