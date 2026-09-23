package com.mdm.platform.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface MasterDataRepository extends JpaRepository<MasterDataEntity, Long>, JpaSpecificationExecutor<MasterDataEntity> {

    Optional<MasterDataEntity> findByIdAndModelIdAndDelFlag(Long id, Long modelId, Integer delFlag);

    boolean existsByModelIdAndCodeAndDelFlag(Long modelId, String code, Integer delFlag);

    List<MasterDataEntity> findByModelIdAndDelFlagAndStatusIn(Long modelId, Integer delFlag, List<String> statuses);

    List<MasterDataEntity> findByModelIdAndDelFlag(Long modelId, Integer delFlag);

    long countByModelIdAndDelFlag(Long modelId, Integer delFlag);

    long countByDelFlag(Integer delFlag);

    long countByDelFlagAndStatus(Integer delFlag, String status);
}
