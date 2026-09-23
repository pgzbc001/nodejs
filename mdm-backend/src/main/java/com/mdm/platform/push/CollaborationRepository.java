package com.mdm.platform.push;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface CollaborationRepository extends JpaRepository<CollaborationEntity, Long>, JpaSpecificationExecutor<CollaborationEntity> {

    List<CollaborationEntity> findByDataIdAndDelFlagOrderByIdDesc(Long dataId, Integer delFlag);

    Optional<CollaborationEntity> findFirstByDataIdAndStatusAndDelFlagOrderByIdDesc(Long dataId, String status, Integer delFlag);
}
