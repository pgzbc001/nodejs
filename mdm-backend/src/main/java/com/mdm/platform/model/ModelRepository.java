package com.mdm.platform.model;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ModelRepository extends JpaRepository<ModelEntity, Long>, JpaSpecificationExecutor<ModelEntity> {

    Optional<ModelEntity> findByIdAndDelFlag(Long id, Integer delFlag);

    boolean existsByCodeAndDelFlag(String code, Integer delFlag);

    long countByCategoryIdAndDelFlag(Long categoryId, Integer delFlag);

    Page<ModelEntity> findByDelFlag(Integer delFlag, Pageable pageable);
}
