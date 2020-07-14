package com.alibaba.csp.sentinel.cluster.client.codec.data;

import com.alibaba.csp.sentinel.cluster.codec.EntityDecoder;
import com.alibaba.csp.sentinel.cluster.response.data.ConcurrentFlowReleaseResponseData;
import io.netty.buffer.ByteBuf;

/**
 * @author yunfeiyanggzq
 * @Date 2020/7/9 11:53
 */
public class ConcurrentFlowReleaseResponseDataDecoder implements EntityDecoder<ByteBuf, ConcurrentFlowReleaseResponseData> {
    @Override
    public ConcurrentFlowReleaseResponseData decode(ByteBuf source) {
        return new ConcurrentFlowReleaseResponseData();
    }
}
