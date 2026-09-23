package com.mdm.platform.coderule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CodeSequenceRepository extends JpaRepository<CodeSequenceEntity, Long> {

    Optional<CodeSequenceEntity> findByModelId(Long modelId);

    /** 原子递增（SEQ 段取号），返回递增后的值。 */
    @Modifying
    @Query("UPDATE CodeSequenceEntity s SET s.currentValue = s.currentValue + :step, s.updatedTime = :now WHERE s.modelId = :modelId")
    int incrementByModelId(@Param("modelId") Long modelId, @Param("step") int step, @Param("now") String now);
}
