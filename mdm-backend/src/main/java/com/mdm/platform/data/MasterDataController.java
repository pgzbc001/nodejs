package com.mdm.platform.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdm.platform.common.ApiResponse;
import com.mdm.platform.common.BusinessException;
import com.mdm.platform.common.ErrorCode;
import com.mdm.platform.common.PageResult;
import com.mdm.platform.data.dto.DataDetailVO;
import com.mdm.platform.data.dto.DataDiffVO;
import com.mdm.platform.data.dto.DataQueryRequest;
import com.mdm.platform.data.dto.DataUpsertRequest;
import com.mdm.platform.data.dto.DataViewSchemaVO;
import com.mdm.platform.data.dto.DataViewVO;
import com.mdm.platform.data.dto.DataVersionVO;
import com.mdm.platform.duplicate.dto.DuplicateCheckRequest;
import com.mdm.platform.duplicate.dto.DuplicateCheckResultVO;
import com.mdm.platform.duplicate.DuplicateCheckService;
import com.mdm.platform.model.ModelEntity;
import com.mdm.platform.quality.QualityRuleService;
import com.mdm.platform.quality.dto.QualityViolationVO;
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
 * 主数据接口（契约 3.3/3.4：/data，含版本管理）。
 */
@RestController
@RequestMapping("/api/v1/data")
public class MasterDataController {

    private final MasterDataService dataService;
    private final VersionService versionService;
    private final DuplicateCheckService duplicateCheckService;
    private final QualityRuleService qualityRuleService;
    private final ObjectMapper objectMapper;

    public MasterDataController(MasterDataService dataService,
                                VersionService versionService,
                                DuplicateCheckService duplicateCheckService,
                                QualityRuleService qualityRuleService) {
        this.dataService = dataService;
        this.versionService = versionService;
        this.duplicateCheckService = duplicateCheckService;
        this.qualityRuleService = qualityRuleService;
        this.objectMapper = new ObjectMapper();
    }

    /** 动态列表视图（列定义 + 检索字段 + 全字段）。 */
    @GetMapping("/{modelId}/view")
    public ApiResponse<DataViewSchemaVO> view(@PathVariable Long modelId) {
        return ApiResponse.ok(dataService.view(modelId));
    }

    /**
     * 分页查询：keyword/status 直传，filters 为 URL 编码 JSON（{"字段名":"值"}）。
     */
    @GetMapping("/{modelId}")
    public ApiResponse<PageResult<DataViewVO>> page(@PathVariable Long modelId,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(required = false) String filters,
                                                    @RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        DataQueryRequest request = new DataQueryRequest();
        request.setModelId(modelId);
        request.setKeyword(keyword);
        request.setStatus(status);
        request.setFilters(parseFilters(filters));
        request.setPage(page);
        request.setSize(size);
        return ApiResponse.ok(dataService.page(request));
    }

    /** 详情（明文属性供编辑回显 + 质量结果 + 版本摘要）。 */
    @GetMapping("/{modelId}/{id}")
    public ApiResponse<DataDetailVO> detail(@PathVariable Long modelId, @PathVariable Long id) {
        return ApiResponse.ok(dataService.detail(id));
    }

    /** 提交前查重（Top 5 相似数据，≥80 高度相似）。 */
    @PostMapping("/{modelId}/check-duplicate")
    public ApiResponse<DuplicateCheckResultVO> checkDuplicate(@PathVariable Long modelId,
                                                              @RequestBody Map<String, Object> body) {
        DuplicateCheckRequest request = new DuplicateCheckRequest();
        request.setModelId(modelId);
        request.setAttributes(castAttributes(body.get("attributes")));
        Object exclude = body.get("excludeDataId");
        if (exclude instanceof Number number) {
            request.setExcludeDataId(number.longValue());
        }
        return ApiResponse.ok(duplicateCheckService.check(request));
    }

    /** 提交前质量校验（逐条违规结果，CRITICAL 前端阻止提交）。 */
    @PostMapping("/{modelId}/check-quality")
    public ApiResponse<List<QualityViolationVO>> checkQuality(@PathVariable Long modelId,
                                                              @RequestBody Map<String, Object> body) {
        ModelEntity model = dataService.requireModel(modelId);
        return ApiResponse.ok(qualityRuleService.evaluate(model, castAttributes(body.get("attributes"))));
    }

    /** 新增主数据（动态校验 + 质量求值 + 编码生成 + 加密 + 快照）。 */
    @PostMapping("/{modelId}")
    @NoRepeatSubmit
    @RequirePermission({Role.DATA_STAFF, Role.MODEL_ADMIN, Role.SYS_ADMIN})
    public ApiResponse<DataDetailVO> create(@PathVariable Long modelId, @RequestBody DataUpsertRequest request) {
        return ApiResponse.ok(dataService.create(modelId, request));
    }

    /** 修改主数据（新版本 + 敏感字段变更触发协同）。 */
    @PutMapping("/{modelId}/{id}")
    @NoRepeatSubmit
    @RequirePermission({Role.DATA_STAFF, Role.MODEL_ADMIN, Role.SYS_ADMIN})
    public ApiResponse<DataDetailVO> update(@PathVariable Long modelId, @PathVariable Long id,
                                            @RequestBody DataUpsertRequest request) {
        return ApiResponse.ok(dataService.update(id, request));
    }

    /** 禁用（下游预检 FAIL 且未 force 时 40909）。 */
    @PostMapping("/{modelId}/{id}/disable")
    @NoRepeatSubmit
    @RequirePermission({Role.DATA_STAFF, Role.MODEL_ADMIN, Role.SYS_ADMIN})
    public ApiResponse<DataDetailVO> disable(@PathVariable Long modelId, @PathVariable Long id,
                                             @RequestParam(defaultValue = "false") boolean force) {
        return ApiResponse.ok(dataService.disable(id, force));
    }

    /** 启用（DISABLED → VALID）。 */
    @PostMapping("/{modelId}/{id}/enable")
    @NoRepeatSubmit
    @RequirePermission({Role.DATA_STAFF, Role.MODEL_ADMIN, Role.SYS_ADMIN})
    public ApiResponse<DataDetailVO> enable(@PathVariable Long modelId, @PathVariable Long id) {
        return ApiResponse.ok(dataService.enable(id));
    }

    /** 逻辑删除（下游预检，force=true 强制）。 */
    @DeleteMapping("/{modelId}/{id}")
    @NoRepeatSubmit
    @RequirePermission({Role.DATA_STAFF, Role.MODEL_ADMIN, Role.SYS_ADMIN})
    public ApiResponse<Void> delete(@PathVariable Long modelId, @PathVariable Long id,
                                    @RequestParam(defaultValue = "false") boolean force) {
        dataService.delete(id, force);
        return ApiResponse.ok();
    }

    // ==================== 版本管理（契约 3.4） ====================

    /** 版本列表。 */
    @GetMapping("/{modelId}/{id}/versions")
    public ApiResponse<List<DataVersionVO>> versions(@PathVariable Long modelId, @PathVariable Long id) {
        return ApiResponse.ok(versionService.versions(id));
    }

    /** 版本详情（脱敏）。 */
    @GetMapping("/{modelId}/{id}/versions/{versionNo}")
    public ApiResponse<Map<String, Object>> versionDetail(@PathVariable Long modelId, @PathVariable Long id,
                                                          @PathVariable Integer versionNo) {
        return ApiResponse.ok(versionService.maskedVersionDetail(id, versionNo));
    }

    /** 两版本逐字段差异（解密后明文对比）。 */
    @GetMapping("/{modelId}/{id}/versions/{fromVersion}/diff/{toVersion}")
    public ApiResponse<DataDiffVO> diff(@PathVariable Long modelId, @PathVariable Long id,
                                        @PathVariable Integer fromVersion,
                                        @PathVariable Integer toVersion) {
        return ApiResponse.ok(versionService.diff(id, fromVersion, toVersion));
    }

    /** 回滚到指定版本。 */
    @PostMapping("/{modelId}/{id}/versions/{versionNo}/rollback")
    @NoRepeatSubmit
    @RequirePermission({Role.DATA_STAFF, Role.MODEL_ADMIN, Role.SYS_ADMIN})
    public ApiResponse<DataDetailVO> rollback(@PathVariable Long modelId, @PathVariable Long id,
                                              @PathVariable Integer versionNo) {
        versionService.rollback(id, versionNo);
        return ApiResponse.ok(dataService.detail(id));
    }

    // ==================== 内部工具 ====================

    private Map<String, String> parseFilters(String filters) {
        if (filters == null || filters.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(filters, new TypeReference<Map<String, String>>() {
            });
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "filters 参数须为 JSON 对象：{ \"字段名\": \"值\" }");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castAttributes(Object attributes) {
        return attributes == null ? Map.of() : (Map<String, Object>) attributes;
    }
}
