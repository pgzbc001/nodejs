package com.mdm.platform.quality;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QualityRuleRepository extends JpaRepository<QualityRuleEntity, Long> {

    /** 模型启用的规则（含全局规则 model_id 为 null） */
    List<QualityRuleEntity> findByModelIdAndEnabledAndDelFlag(Long modelId, Integer enabled, Integer delFlag);

    /** 全局启用的规则（model_id 为 null） */
    List<QualityRuleEntity> findByModelIdIsNullAndEnabledAndDelFlag(Integer enabled, Integer delFlag);

    List<QualityRuleEntity> findByModelIdAndDelFlag(Long modelId, Integer delFlag);

    List<QualityRuleEntity> findByModelIdIsNullAndDelFlag(Integer delFlag);

    List<QualityRuleEntity> findByEnabledAndDelFlag(Integer enabled, Integer delFlag);

    Optional<QualityRuleEntity> findByIdAndDelFlag(Long id, Integer delFlag);

    Page<QualityRuleEntity> findByDelFlag(Integer delFlag, Pageable pageable);
}
