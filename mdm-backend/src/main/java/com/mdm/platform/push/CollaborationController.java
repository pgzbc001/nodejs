package com.mdm.platform.push;

import com.mdm.platform.common.ApiResponse;
import com.mdm.platform.common.PageResult;
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

import java.util.Map;

/**
 * 协同确认接口（契约 3.6：/collaborations）。
 */
@RestController
@RequestMapping("/api/v1/collaborations")
public class CollaborationController {

    private final PushService pushService;

    public CollaborationController(PushService pushService) {
        this.pushService = pushService;
    }

    /** 协同工单列表（status 可筛选 PENDING/CONFIRMED/REJECTED）。 */
    @GetMapping
    public ApiResponse<PageResult<CollaborationEntity>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(pushService.collaborations(status, page, size));
    }

    /** 协同确认（comment 可选）。 */
    @PostMapping("/{id}/confirm")
    @NoRepeatSubmit
    @RequirePermission({Role.DATA_AUDITOR, Role.SYS_ADMIN})
    public ApiResponse<Void> confirm(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        pushService.confirm(id, body == null ? null : body.get("comment"));
        return ApiResponse.ok();
    }

    /** 协同驳回（数据自动恢复修改前版本）。 */
    @PostMapping("/{id}/reject")
    @NoRepeatSubmit
    @RequirePermission({Role.DATA_AUDITOR, Role.SYS_ADMIN})
    public ApiResponse<Void> reject(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        pushService.reject(id, body == null ? null : body.get("comment"));
        return ApiResponse.ok();
    }
}
