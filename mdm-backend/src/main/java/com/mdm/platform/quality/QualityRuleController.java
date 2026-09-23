package com.mdm.platform.quality;

import com.mdm.platform.common.ApiResponse;
import com.mdm.platform.quality.dto.QualityRuleRequest;
import com.mdm.platform.security.NoRepeatSubmit;
import com.mdm.platform.security.RequirePermission;
import com.mdm.platform.security.Role;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 质量规则接口（契约 3.7：/quality）。
 */
@RestController
@RequestMapping("/api/v1/quality")
public class QualityRuleController {

    private final QualityRuleService qualityRuleService;

    public QualityRuleController(QualityRuleService qualityRuleService) {
        this.qualityRuleService = qualityRuleService;
    }

    /** 规则列表（modelId 空=仅全局规则）。 */
    @GetMapping("/rules")
    public ApiResponse<List<QualityRuleEntity>> rules(@RequestParam(required = false) Long modelId) {
        return ApiResponse.ok(modelId == null
                ? qualityRuleService.listGlobal()
                : qualityRuleService.listByModel(modelId));
    }

    @PostMapping("/rules")
    @NoRepeatSubmit
    @RequirePermission({Role.MODEL_ADMIN, Role.SYS_ADMIN})
    public ApiResponse<QualityRuleEntity> create(@RequestBody QualityRuleRequest request) {
        return ApiResponse.ok(qualityRuleService.create(request));
    }

    @PutMapping("/rules/{id}")
    @NoRepeatSubmit
    @RequirePermission({Role.MODEL_ADMIN, Role.SYS_ADMIN})
    public ApiResponse<QualityRuleEntity> update(@PathVariable Long id, @RequestBody QualityRuleRequest request) {
        return ApiResponse.ok(qualityRuleService.update(id, request));
    }

    @DeleteMapping("/rules/{id}")
    @NoRepeatSubmit
    @RequirePermission({Role.MODEL_ADMIN, Role.SYS_ADMIN})
    public ApiResponse<Void> delete(@PathVariable Long id) {
        qualityRuleService.delete(id);
        return ApiResponse.ok();
    }

    /** 某数据的当前有效质量结果。 */
    @GetMapping("/results")
    public ApiResponse<List<QualityResultEntity>> results(@RequestParam Long dataId) {
        return ApiResponse.ok(qualityRuleService.listResults(dataId));
    }

    /** 忽略告警（仅 WARNING/INFO；CRITICAL 返回 40907）。 */
    @PostMapping("/results/{id}/ignore")
    @NoRepeatSubmit
    @RequirePermission({Role.DATA_STAFF, Role.MODEL_ADMIN, Role.SYS_ADMIN})
    public ApiResponse<Void> ignore(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        qualityRuleService.ignoreResult(id, body == null ? null : body.get("reason"));
        return ApiResponse.ok();
    }
}
