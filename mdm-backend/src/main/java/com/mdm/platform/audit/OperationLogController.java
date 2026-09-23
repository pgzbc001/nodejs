package com.mdm.platform.audit;

import com.mdm.platform.common.ApiResponse;
import com.mdm.platform.common.PageResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志接口（契约 3.8：/logs）。
 */
@RestController
@RequestMapping("/api/v1/logs")
public class OperationLogController {

    private final OperationLogService logService;

    public OperationLogController(OperationLogService logService) {
        this.logService = logService;
    }

    /** 操作日志分页（bizType/operation/operator/时间范围筛选）。 */
    @GetMapping
    public ApiResponse<PageResult<OperationLogEntity>> page(
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(logService.page(bizType, operation, operator, startTime, endTime, page, size));
    }
}
