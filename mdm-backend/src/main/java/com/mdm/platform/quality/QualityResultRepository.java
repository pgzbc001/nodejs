package com.mdm.platform.quality;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QualityResultRepository extends JpaRepository<QualityResultEntity, Long> {

    List<QualityResultEntity> findByDataIdAndDelFlag(Long dataId, Integer delFlag);

    /** 重新检查前逻辑归档旧结果 */
    @Modifying
    @Query("UPDATE QualityResultEntity r SET r.delFlag = 1 WHERE r.dataId = :dataId AND r.delFlag = 0")
    int archiveByDataId(@Param("dataId") Long dataId);
}
