package com.alibaba.csp.sentinel.cluster.request.data;

/**
 * @Author:yunfeiyanggzq
 * @Date:2020/6/3017:01
 */
public class ConcurrentFlowAcquireRequestData {
    private long flowId;

    private int count;

    private boolean priority;

    public long getFlowId() {
        return flowId;
    }

    public void setFlowId(long flowId) {
        this.flowId = flowId;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public boolean isPriority() {
        return priority;
    }

    public void setPriority(boolean priority) {
        this.priority = priority;
    }
}
