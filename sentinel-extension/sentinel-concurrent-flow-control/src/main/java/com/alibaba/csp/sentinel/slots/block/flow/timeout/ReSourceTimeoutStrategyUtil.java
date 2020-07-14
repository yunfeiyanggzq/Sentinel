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

import com.alibaba.csp.sentinel.cluster.TokenService;
import com.alibaba.csp.sentinel.context.Context;
import io.netty.util.Timeout;

/**
 * @author yunfeiyanggzq
 */
public class ReSourceTimeoutStrategyUtil {

    public final static int RELEASE_TOKEN = 1;

    public final static int KEEP_TOKEN = 2;

    private ReSourceTimeoutStrategyUtil() {

    }

    public static ReSourceTimeoutStrategy getTimeoutStrategy(int sourceTimeoutStrategyStatus) {
        switch (sourceTimeoutStrategyStatus) {
            case 2:
                return new RefreshTokenStrategy();
            default:
                return new ReleaseTokenStrategy();
        }
    }

    public static boolean valid(Context context, long tokenId, Timeout timeout, TokenService service) {
        if (service == null || timeout.isCancelled() || context.getCurEntry() == null || context.getCurEntry().getTokenId() == 0) {
            return false;
        }
        return true;
    }
}
