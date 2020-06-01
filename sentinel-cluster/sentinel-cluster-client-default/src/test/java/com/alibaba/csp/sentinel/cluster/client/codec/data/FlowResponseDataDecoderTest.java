package com.alibaba.csp.sentinel.cluster.client.codec.data;

import com.alibaba.csp.sentinel.cluster.response.data.FlowTokenResponseData;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;

public class FlowResponseDataDecoderTest {
    @Test
    public void testDecode(){
        ByteBuf buf = Unpooled.buffer();
        FlowResponseDataDecoder decoder = new FlowResponseDataDecoder();
        FlowTokenResponseData data=new FlowTokenResponseData();
        data.setRemainingCount(12);
        data.setWaitInMs(13);
        buf.writeInt(12);
        buf.writeInt(13);
        Assert.assertEquals(decoder.decode(buf),data);
    }
}
