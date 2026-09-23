package com.mdm.platform.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdm.platform.common.BusinessException;
import com.mdm.platform.common.ErrorCode;
import com.mdm.platform.common.PageResult;
import com.mdm.platform.common.Times;
import com.mdm.platform.security.UserContext;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 操作日志服务（REQ-BKD10）：新增/修改/禁用/删除/推送/协同/导入/回滚全操作留痕，
 * 支持按时间范围与操作类型筛选。
 */
@Service
public class OperationLogService {

    private final OperationLogRepository repository;
    private final ObjectMapper objectMapper;

    public OperationLogService(OperationLogRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 记录操作日志。
     *
     * @param bizType    业务类型（CATEGORY/MODEL/DATA/PUSH/COLLABORATION/QUALITY/IMPORT_EXPORT）
     * @param operation  操作（CREATE/UPDATE/...）
     * @param targetId   目标对象 ID
     * @param targetDesc 目标描述
     * @param detail     详情（对象自动转 JSON）
     */
    public void log(String bizType, String operation, Object targetId, String targetDesc, Object detail) {
        OperationLogEntity entity = new OperationLogEntity();
        entity.setBizType(bizType);
        entity.setOperation(operation);
        entity.setTargetId(targetId == null ? null : String.valueOf(targetId));
        entity.setTargetDesc(targetDesc);
        entity.setDetail(toJson(detail));
        entity.setOperator(currentOperator());
        entity.setOperatedTime(Times.now());
        repository.save(entity);
    }

    /**
     * 分页筛选日志。
     */
    public PageResult<OperationLogEntity> page(String bizType, String operation, String operator,
                                               String startTime, String endTime, int page, int size) {
        Specification<OperationLogEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("delFlag"), 0));
            if (bizType != null && !bizType.isBlank()) {
                predicates.add(cb.equal(root.get("bizType"), bizType.trim()));
            }
            if (operation != null && !operation.isBlank()) {
                predicates.add(cb.equal(root.get("operation"), operation.trim()));
            }
            if (operator != null && !operator.isBlank()) {
                predicates.add(cb.like(root.get("operator"), "%" + operator.trim() + "%"));
            }
            if (startTime != null && !startTime.isBlank()) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("operatedTime"), startTime.trim()));
            }
            if (endTime != null && !endTime.isBlank()) {
                predicates.add(cb.lessThanOrEqualTo(root.get("operatedTime"), endTime.trim()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, size),
                Sort.by(Sort.Direction.DESC, "operatedTime").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<OperationLogEntity> result = repository.findAll(spec, pageable);
        return new PageResult<>(result.getTotalElements(), page, size, result.getContent());
    }

    private String currentOperator() {
        UserContext ctx = UserContext.get();
        return ctx == null ? "system" : ctx.operator();
    }

    private String toJson(Object detail) {
        if (detail == null) {
            return null;
        }
        if (detail instanceof String text) {
            return text;
        }
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "日志详情序列化失败");
        }
    }
}
