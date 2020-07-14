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
public class RefreshTokenStrategy implements ReSourceTimeoutStrategy {
    @Override
    public void doWithSourceTimeout(Context context, long tokenId, Timeout timeout) {
        synchronized (timeout) {
            TokenService service = ConcurrentFlowRuleChecker.pickClusterService();
            if (!ReSourceTimeoutStrategyUtil.valid(context, tokenId, timeout, service)) {
                return;
            }
            if (context.getCurEntry().getIsClusterToken()) {
                TokenResult result = service.keepConcurrentToken(context.getCurEntry().getTokenId());
                if (result == null || result.getStatus() != TokenResultStatus.KEEP_OK) {
                    RecordLog.warn("[ReleaseTokenStrategy] keep cluster source timeout token unexpected failed", result);
                }
            }
        }
    }
}
