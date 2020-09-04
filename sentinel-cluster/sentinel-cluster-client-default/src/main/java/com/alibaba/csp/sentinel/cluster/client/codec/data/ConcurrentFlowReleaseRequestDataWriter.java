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
package com.alibaba.csp.sentinel.cluster.client.codec.data;

import com.alibaba.csp.sentinel.cluster.codec.EntityWriter;
import com.alibaba.csp.sentinel.cluster.request.data.ConcurrentFlowAcquireRequestData;
import com.alibaba.csp.sentinel.cluster.request.data.ConcurrentFlowReleaseRequestData;
import io.netty.buffer.ByteBuf;

/**
 * +-------------------+--------------+------------------+
 * | RequestID(8 byte) | Type(1 byte) | TokenID(8 byte) |
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
