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

import com.alibaba.csp.sentinel.slots.block.RuleConstant;

/**
 * @author yunfeiyanggzq
 */
public class ReSourceTimeoutStrategyUtil {

    private static ReSourceTimeoutStrategy releaseStrategy;

    static {
        loadStrategy();
    }

    private ReSourceTimeoutStrategyUtil() {
    }

    private static void loadStrategy() {
        releaseStrategy = new ReleaseTokenStrategy();
    }

    private static ReSourceTimeoutStrategy pickStrategy(int resourceTimeoutStrategyStatus) {
        switch (resourceTimeoutStrategyStatus) {
            case RuleConstant.KEEP_RESOURCE_TIMEOUT_STRATEGY:
                return releaseStrategy;
            default:
                return null;
        }
    }


    public static void doResourceTimeoutStrategy(int resourceTimeoutStrategyStatus,long tokenId) {
        ReSourceTimeoutStrategy strategy=pickStrategy(resourceTimeoutStrategyStatus);
        if(strategy==null){
            return;
        }
        strategy.doWithSourceTimeout(tokenId);

    }
}
