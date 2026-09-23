package com.mdm.platform.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ModelVersionRepository extends JpaRepository<ModelVersionEntity, Long> {

    List<ModelVersionEntity> findByModelIdAndDelFlagOrderByVersionNoDesc(Long modelId, Integer delFlag);

    Optional<ModelVersionEntity> findByModelIdAndVersionNoAndDelFlag(Long modelId, Integer versionNo, Integer delFlag);
}
