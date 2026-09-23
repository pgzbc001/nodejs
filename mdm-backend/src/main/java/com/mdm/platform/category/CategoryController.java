package com.mdm.platform.category;

import com.mdm.platform.common.ApiResponse;
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

/**
 * 分类管理接口（契约 3.1：/categories）。
 */
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /** 完整分类树（含每分类模型数）。 */
    @GetMapping("/tree")
    public ApiResponse<List<CategoryTreeVO>> tree() {
        return ApiResponse.ok(categoryService.tree());
    }

    /** 关键字过滤树（命中节点向上补全链路）。 */
    @GetMapping("/search")
    public ApiResponse<List<CategoryTreeVO>> search(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(categoryService.search(keyword));
    }

    @PostMapping
    @NoRepeatSubmit
    @RequirePermission({Role.MODEL_ADMIN, Role.SYS_ADMIN})
    public ApiResponse<CategoryEntity> create(@RequestBody CategoryEntity entity) {
        return ApiResponse.ok(categoryService.create(entity));
    }

    @PutMapping("/{id}")
    @NoRepeatSubmit
    @RequirePermission({Role.MODEL_ADMIN, Role.SYS_ADMIN})
    public ApiResponse<CategoryEntity> update(@PathVariable Long id, @RequestBody CategoryEntity entity) {
        return ApiResponse.ok(categoryService.update(id, entity));
    }

    /** 删除保护：存在子分类或已关联模型返回 40901。 */
    @DeleteMapping("/{id}")
    @NoRepeatSubmit
    @RequirePermission({Role.MODEL_ADMIN, Role.SYS_ADMIN})
    public ApiResponse<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ApiResponse.ok();
    }
}
