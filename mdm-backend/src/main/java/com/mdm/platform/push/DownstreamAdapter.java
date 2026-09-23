package com.mdm.platform.push;

import com.mdm.platform.data.MasterDataEntity;

/**
 * 下游系统适配器（REQ-BKD08）：v1.0 Mock 实现（确定性随机），v2.0 替换 HTTP 实现。
 */
public interface DownstreamAdapter {

    String PASS = "PASS";
    String FAIL = "FAIL";
    String CHECKING = "CHECKING";

    /**
     * 推送前预检：校验下游系统是否可接收该数据。
     *
     * @param data   待推送主数据
     * @param system 目标下游系统
     * @return PASS / FAIL / CHECKING
     */
    String precheck(MasterDataEntity data, DownstreamSystemEntity system);
}
