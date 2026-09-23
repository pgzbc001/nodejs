package com.mdm.platform.push;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DownstreamSystemRepository extends JpaRepository<DownstreamSystemEntity, Long> {

    List<DownstreamSystemEntity> findByDelFlagOrderByIdAsc(Integer delFlag);

    List<DownstreamSystemEntity> findByEnabledAndDelFlag(Integer enabled, Integer delFlag);

    Optional<DownstreamSystemEntity> findByIdAndDelFlag(Long id, Integer delFlag);

    boolean existsByCodeAndDelFlag(String code, Integer delFlag);
}
