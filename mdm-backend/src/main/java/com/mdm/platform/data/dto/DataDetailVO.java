package com.mdm.platform.data.dto;

import com.mdm.platform.quality.QualityResultEntity;

import java.util.List;

/**
 * 主数据详情视图：明文属性 + 质量结果 + 版本摘要。
 */
public class DataDetailVO {

    private DataViewVO data;

    private List<QualityResultEntity> qualityResults;

    private List<DataVersionVO> versions;

    public DataViewVO getData() {
        return data;
    }

    public void setData(DataViewVO data) {
        this.data = data;
    }

    public List<QualityResultEntity> getQualityResults() {
        return qualityResults;
    }

    public void setQualityResults(List<QualityResultEntity> qualityResults) {
        this.qualityResults = qualityResults;
    }

    public List<DataVersionVO> getVersions() {
        return versions;
    }

    public void setVersions(List<DataVersionVO> versions) {
        this.versions = versions;
    }
}
