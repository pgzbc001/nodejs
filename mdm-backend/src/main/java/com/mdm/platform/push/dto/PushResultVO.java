package com.mdm.platform.push.dto;

import java.util.List;

/**
 * 推送执行结果汇总。
 */
public class PushResultVO {

    private int successCount;

    private int failCount;

    private List<PushItemResult> items;

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailCount() {
        return failCount;
    }

    public void setFailCount(int failCount) {
        this.failCount = failCount;
    }

    public List<PushItemResult> getItems() {
        return items;
    }

    public void setItems(List<PushItemResult> items) {
        this.items = items;
    }
}
