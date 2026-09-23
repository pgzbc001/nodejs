package com.mdm.platform.io.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 导入结果摘要（REQ-BKD09：总数/成功/失败 + 失败行明细）。
 */
public class ImportResultVO {

    private int total;

    private int successCount;

    private int failCount;

    private List<ImportErrorVO> errors = new ArrayList<>();

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

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

    public List<ImportErrorVO> getErrors() {
        return errors;
    }

    public void setErrors(List<ImportErrorVO> errors) {
        this.errors = errors;
    }
}
