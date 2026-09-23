package com.mdm.platform.push;

import com.mdm.platform.data.MasterDataEntity;
import org.springframework.stereotype.Component;

/**
 * Mock 下游适配器：确定性随机 95% PASS / 4% FAIL / 1% CHECKING，
 * 同一数据×系统结果稳定（便于演示与测试）。
 */
@Component
public class MockDownstreamAdapter implements DownstreamAdapter {

    @Override
    public String precheck(MasterDataEntity data, DownstreamSystemEntity system) {
        long seed = (data.getId() == null ? 0 : data.getId()) * 31L
                + (system.getId() == null ? 0 : system.getId()) * 17L
                + (data.getCode() == null ? 0 : data.getCode().hashCode());
        int bucket = (int) (Math.abs(seed) % 100);
        if (bucket < 95) {
            return PASS;
        }
        if (bucket < 99) {
            return FAIL;
        }
        return CHECKING;
    }
}
