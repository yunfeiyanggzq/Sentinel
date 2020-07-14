package com.alibaba.csp.sentinel.cluster.server.codec.data;

import com.alibaba.csp.sentinel.cluster.codec.EntityWriter;
import com.alibaba.csp.sentinel.cluster.response.data.ConcurrentFlowAcquireResponseData;
import com.alibaba.csp.sentinel.cluster.response.data.ConcurrentFlowReleaseResponseData;
import io.netty.buffer.ByteBuf;

/**
 * @author yunfeiyanggzq
 */
public class ConcurrentFlowReleaseResponseDataWriter  implements EntityWriter<ConcurrentFlowReleaseResponseData, ByteBuf> {
    @Override
    public void writeTo(ConcurrentFlowReleaseResponseData entity, ByteBuf target) {
    }
}
