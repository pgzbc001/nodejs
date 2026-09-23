package com.mdm.platform.model;

import com.mdm.platform.common.ApiResponse;
import com.mdm.platform.common.PageResult;
import com.mdm.platform.model.dto.ModelCreateRequest;
import com.mdm.platform.model.dto.ModelDiffVO;
import com.mdm.platform.model.dto.ModelUpdateRequest;
import com.mdm.platform.model.dto.ModelVO;
import com.mdm.platform.model.dto.ModelVersionVO;
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

import java.util.Map;

/**
 * 模型管理接口（契约 3.2：/models）。
 */
@RestController
@RequestMapping("/api/v1/models")
public class ModelController {

    private final ModelService modelService;

    public ModelController(ModelService modelService) {
        this.modelService = modelService;
    }

    /** 分页列表（关键字/分类/状态过滤）。 */
    @GetMapping
    public ApiResponse<PageResult<ModelVO>> page(@RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) Long categoryId,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(modelService.page(keyword, categoryId, status, page, size));
    }

    /** 模型详情（字段/编码规则/扩展配置结构化返回）。 */
    @GetMapping("/{id}")
    public ApiResponse<ModelVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(modelService.detail(id));
    }

    /** 创建模型（mode=BLANK 空白 / INHERIT 继承）。 */
    @PostMapping
    @NoRepeatSubmit
    @RequirePermission({Role.MODEL_ADMIN, Role.SYS_ADMIN})
    public ApiResponse<ModelVO> create(@RequestBody ModelCreateRequest request) {
        return ApiResponse.ok(modelService.create(request));
    }

    /** 更新模型定义（上线后结构锁定 40903）。 */
    @PutMapping("/{id}")
    @NoRepeatSubmit
    @RequirePermission({Role.MODEL_ADMIN, Role.SYS_ADMIN})
    public ApiResponse<ModelVO> update(@PathVariable Long id, @RequestBody ModelUpdateRequest request) {
        return ApiResponse.ok(modelService.update(id, request));
    }

    /** 上线（≥1 字段 + 编码规则合法校验）。 */
    @PostMapping("/{id}/online")
    @NoRepeatSubmit
    @RequirePermission({Role.MODEL_ADMIN, Role.SYS_ADMIN})
    public ApiResponse<ModelVO> online(@PathVariable Long id) {
        return ApiResponse.ok(modelService.online(id));
    }

    @PostMapping("/{id}/offline")
    @NoRepeatSubmit
    @RequirePermission({Role.MODEL_ADMIN, Role.SYS_ADMIN})
    public ApiResponse<ModelVO> offline(@PathVariable Long id) {
        return ApiResponse.ok(modelService.offline(id));
    }

    @DeleteMapping("/{id}")
    @NoRepeatSubmit
    @RequirePermission({Role.MODEL_ADMIN, Role.SYS_ADMIN})
    public ApiResponse<Void> delete(@PathVariable Long id) {
        modelService.delete(id);
        return ApiResponse.ok();
    }

    /** 版本列表（新版本在前）。 */
    @GetMapping("/{id}/versions")
    public ApiResponse<java.util.List<ModelVersionVO>> versions(@PathVariable Long id) {
        return ApiResponse.ok(modelService.versions(id));
    }

    /** 两版本差异（基础信息/字段/编码规则/扩展配置）。 */
    @GetMapping("/{id}/versions/{fromVersion}/diff/{toVersion}")
    public ApiResponse<ModelDiffVO> diff(@PathVariable Long id,
                                         @PathVariable Integer fromVersion,
                                         @PathVariable Integer toVersion) {
        return ApiResponse.ok(modelService.diff(id, fromVersion, toVersion));
    }

    /** 回滚到指定版本。 */
    @PostMapping("/{id}/versions/{versionNo}/rollback")
    @NoRepeatSubmit
    @RequirePermission({Role.MODEL_ADMIN, Role.SYS_ADMIN})
    public ApiResponse<ModelVO> rollback(@PathVariable Long id, @PathVariable Integer versionNo) {
        return ApiResponse.ok(modelService.rollback(id, versionNo));
    }

    /** 兼容入口：版本对比（body 指定 from/to）。 */
    @PostMapping("/{id}/diff")
    public ApiResponse<ModelDiffVO> diffByBody(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        return ApiResponse.ok(modelService.diff(id, body.get("fromVersion"), body.get("toVersion")));
    }
}
