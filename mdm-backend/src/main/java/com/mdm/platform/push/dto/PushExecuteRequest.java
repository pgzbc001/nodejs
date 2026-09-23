package com.mdm.platform.push.dto;

import java.util.List;

/**
 * 推送执行请求。
 */
public class PushExecuteRequest {

    private List<Long> dataIds;

    private List<Long> systemIds;

    /** 下游校验 FAIL 时是否强制执行 */
    private boolean force;

    public List<Long> getDataIds() {
        return dataIds;
    }

    public void setDataIds(List<Long> dataIds) {
        this.dataIds = dataIds;
    }

    public List<Long> getSystemIds() {
        return systemIds;
    }

    public void setSystemIds(List<Long> systemIds) {
        this.systemIds = systemIds;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }
}
