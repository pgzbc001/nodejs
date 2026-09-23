package com.mdm.platform.data;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DataVersionRepository extends JpaRepository<DataVersionEntity, Long> {

    List<DataVersionEntity> findByDataIdAndDelFlagOrderByVersionNoDesc(Long dataId, Integer delFlag);

    Optional<DataVersionEntity> findByDataIdAndVersionNoAndDelFlag(Long dataId, Integer versionNo, Integer delFlag);
}
