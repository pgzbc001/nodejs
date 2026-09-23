package com.mdm.platform.push;

import com.mdm.platform.common.ApiResponse;
import com.mdm.platform.common.PageResult;
import com.mdm.platform.push.dto.PrecheckResultVO;
import com.mdm.platform.push.dto.PushExecuteRequest;
import com.mdm.platform.push.dto.PushResultVO;
import com.mdm.platform.security.NoRepeatSubmit;
import com.mdm.platform.security.RequirePermission;
import com.mdm.platform.security.Role;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 推送接口（契约 3.6：/push）。
 */
@RestController
@RequestMapping("/api/v1/push")
public class PushController {

    private final PushService pushService;

    public PushController(PushService pushService) {
        this.pushService = pushService;
    }

    /** 下游系统列表。 */
    @GetMapping("/systems")
    public ApiResponse<List<DownstreamSystemEntity>> systems() {
        return ApiResponse.ok(pushService.systems());
    }

    /** 推送前下游预检（dataIds × systemIds 组合结果）。 */
    @PostMapping("/precheck")
    public ApiResponse<List<PrecheckResultVO>> precheck(@RequestBody PushExecuteRequest request) {
        return ApiResponse.ok(pushService.precheck(request.getDataIds(), request.getSystemIds()));
    }

    /** 单数据预检快捷入口（全部启用系统）。 */
    @GetMapping("/precheck/{dataId}")
    public ApiResponse<List<PrecheckResultVO>> precheckData(@PathVariable Long dataId) {
        return ApiResponse.ok(pushService.precheckData(dataId));
    }

    /** 执行推送（待协同 40908；下游 FAIL 未强制 40909）。 */
    @PostMapping("/execute")
    @NoRepeatSubmit
    @RequirePermission({Role.DATA_STAFF, Role.DATA_AUDITOR, Role.SYS_ADMIN})
    public ApiResponse<PushResultVO> execute(@RequestBody PushExecuteRequest request) {
        return ApiResponse.ok(pushService.execute(request));
    }

    /** 推送日志分页（可按数据过滤）。 */
    @GetMapping("/logs")
    public ApiResponse<PageResult<PushLogEntity>> logs(@RequestParam(required = false) Long dataId,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(pushService.pushLogs(dataId, page, size));
    }

    /** 某数据的推送历史（详情页组合查询）。 */
    @GetMapping("/logs/{dataId}")
    public ApiResponse<List<PushLogEntity>> logsOfData(@PathVariable Long dataId) {
        return ApiResponse.ok(pushService.logsOfData(java.util.Set.of(dataId)));
    }

    /** 兼容入口：body 传 dataIds 批量查推送历史。 */
    @PostMapping("/logs/batch")
    public ApiResponse<List<PushLogEntity>> logsBatch(@RequestBody Map<String, List<Long>> body) {
        List<Long> dataIds = body.get("dataIds");
        return ApiResponse.ok(pushService.logsOfData(
                dataIds == null ? java.util.Set.of() : java.util.Set.copyOf(dataIds)));
    }
}
