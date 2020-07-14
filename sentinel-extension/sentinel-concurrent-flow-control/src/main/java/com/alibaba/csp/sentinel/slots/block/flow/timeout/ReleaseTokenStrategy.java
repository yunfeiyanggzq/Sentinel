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
package com.alibaba.csp.sentinel.slots.block.flow.timeout;

import com.alibaba.csp.sentinel.cluster.TokenResult;
import com.alibaba.csp.sentinel.cluster.TokenResultStatus;
import com.alibaba.csp.sentinel.cluster.TokenService;
import com.alibaba.csp.sentinel.context.Context;
import com.alibaba.csp.sentinel.log.RecordLog;
import com.alibaba.csp.sentinel.slots.block.flow.concurrent.ConcurrentFlowRuleChecker;
import io.netty.util.Timeout;

/**
 * @author yunfeiyanggzq
 */
public class ReleaseTokenStrategy implements ReSourceTimeoutStrategy {

    @Override
    public void doWithSourceTimeout(Context context, long tokenId, Timeout timeout) {
        synchronized (timeout) {
            TokenService service = ConcurrentFlowRuleChecker.pickClusterService();
            if (!ReSourceTimeoutStrategyUtil.valid(context, tokenId, timeout, service)) {
                return;
            }
            if (context.getCurEntry().getIsClusterToken()) {
                TokenResult result = service.releaseConcurrentToken(context.getCurEntry().getTokenId());
                if (result == null || result.getStatus() != TokenResultStatus.RELEASE_OK) {
                    RecordLog.warn("[ReleaseTokenStrategy] release cluster source timeout token unexpected failed", result);
                }
            }
        }
    }
}
