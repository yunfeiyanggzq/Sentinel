package com.alibaba.csp.sentinel.cluster.server.command.handler;

import com.alibaba.csp.sentinel.cluster.flow.statistic.concurrent.CurrentConcurrencyManager;
import com.alibaba.csp.sentinel.command.CommandHandler;
import com.alibaba.csp.sentinel.command.CommandRequest;
import com.alibaba.csp.sentinel.command.CommandResponse;
import com.alibaba.csp.sentinel.command.annotation.CommandMapping;
import com.alibaba.fastjson.JSON;

/**
 * @author yunfeiyanggzq
 * @Date 2020/7/11 12:04
 */
@CommandMapping(name = "cluster/server/concurrentFlowNowCalls", desc = "get cluster concurrent flow nowCalls")
public class FetchClusterConcurrentFlowNowCallsCommandHandler implements CommandHandler<String> {

    @Override
    public CommandResponse<String> handle(CommandRequest request) {
        Long flowId = Long.parseLong(request.getParam("flowId"));
        return CommandResponse.ofSuccess(JSON.toJSONString(CurrentConcurrencyManager.get(flowId)));
    }
}