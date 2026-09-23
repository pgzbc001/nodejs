package com.mdm.platform.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {

    List<CategoryEntity> findByDelFlagOrderBySortNoAscIdAsc(Integer delFlag);

    List<CategoryEntity> findByParentIdAndDelFlag(Long parentId, Integer delFlag);

    long countByParentIdAndDelFlag(Long parentId, Integer delFlag);

    Optional<CategoryEntity> findByIdAndDelFlag(Long id, Integer delFlag);

    boolean existsByCodeAndDelFlag(String code, Integer delFlag);

    Optional<CategoryEntity> findByCodeAndDelFlag(String code, Integer delFlag);
}
