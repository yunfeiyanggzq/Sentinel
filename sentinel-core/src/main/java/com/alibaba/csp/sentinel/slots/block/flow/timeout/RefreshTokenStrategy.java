package com.alibaba.csp.sentinel.slots.block.flow.timeout;

import io.netty.util.Timeout;

/**
 * @author yunfeiyanggzq
 */
public class RefreshTokenStrategy implements ReSourceTimeoutStrategy {
    @Override
    public void doWithSourceTimeout(long tokenId, Timeout timeout) {

    }
}
