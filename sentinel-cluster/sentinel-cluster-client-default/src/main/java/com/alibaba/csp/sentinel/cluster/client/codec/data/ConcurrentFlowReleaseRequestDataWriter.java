package com.alibaba.csp.sentinel.cluster.client.codec.data;

import com.alibaba.csp.sentinel.cluster.codec.EntityWriter;
import com.alibaba.csp.sentinel.cluster.request.data.ConcurrentFlowAcquireRequestData;
import com.alibaba.csp.sentinel.cluster.request.data.ConcurrentFlowReleaseRequestData;
import io.netty.buffer.ByteBuf;

/**
 * +-------------------+--------------+------------------+
 * | RequestID(8 byte) | Type(1 byte) | TokenID(4 byte) |
 * +-------------------+--------------+-----------------+
 *
 * @author yunfeiyanggzq
 * @Date 2020/7/9 11:52
 */
public class ConcurrentFlowReleaseRequestDataWriter implements EntityWriter<ConcurrentFlowReleaseRequestData, ByteBuf> {
    @Override
    public void writeTo(ConcurrentFlowReleaseRequestData entity, ByteBuf target) {
        target.writeLong(entity.getTokenId());
    }
}
