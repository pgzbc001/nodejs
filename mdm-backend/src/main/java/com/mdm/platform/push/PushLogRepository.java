package com.mdm.platform.push;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface PushLogRepository extends JpaRepository<PushLogEntity, Long>, JpaSpecificationExecutor<PushLogEntity> {

    List<PushLogEntity> findByDataIdAndDelFlagOrderByPushedTimeDesc(Long dataId, Integer delFlag);
}
