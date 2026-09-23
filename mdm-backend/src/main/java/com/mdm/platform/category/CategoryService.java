package com.mdm.platform.category;

import com.mdm.platform.audit.OperationLogService;
import com.mdm.platform.common.BusinessException;
import com.mdm.platform.common.ErrorCode;
import com.mdm.platform.common.Times;
import com.mdm.platform.model.ModelRepository;
import com.mdm.platform.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 分类服务（REQ-BKD01）：无限层级树 CRUD + 删除保护 + 关键字搜索。
 */
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ModelRepository modelRepository;
    private final OperationLogService logService;

    public CategoryService(CategoryRepository categoryRepository,
                           ModelRepository modelRepository,
                           OperationLogService logService) {
        this.categoryRepository = categoryRepository;
        this.modelRepository = modelRepository;
        this.logService = logService;
    }

    /**
     * 分类树（含各分类模型数，根节点 parent_id 为空）。
     */
    public List<CategoryTreeVO> tree() {
        List<CategoryEntity> all = categoryRepository.findByDelFlagOrderBySortNoAscIdAsc(0);
        Map<Long, Long> modelCounts = new HashMap<>();
        for (CategoryEntity category : all) {
            modelCounts.put(category.getId(), modelRepository.countByCategoryIdAndDelFlag(category.getId(), 0));
        }
        Map<Long, CategoryTreeVO> voMap = new HashMap<>();
        List<CategoryTreeVO> roots = new ArrayList<>();
        for (CategoryEntity entity : all) {
            voMap.put(entity.getId(), new CategoryTreeVO(entity, modelCounts.getOrDefault(entity.getId(), 0L)));
        }
        for (CategoryEntity entity : all) {
            CategoryTreeVO node = voMap.get(entity.getId());
            CategoryTreeVO parent = entity.getParentId() == null ? null : voMap.get(entity.getParentId());
            if (parent == null) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }

    /**
     * 关键字搜索：名称或编码包含（命中分类向上补全父级链路返回树片段）。
     */
    public List<CategoryTreeVO> search(String keyword) {
        List<CategoryEntity> all = categoryRepository.findByDelFlagOrderBySortNoAscIdAsc(0);
        if (keyword == null || keyword.isBlank()) {
            return tree();
        }
        String key = keyword.trim();
        Map<Long, CategoryEntity> entityMap = new HashMap<>();
        Set<Long> hit = new HashSet<>();
        for (CategoryEntity entity : all) {
            entityMap.put(entity.getId(), entity);
            if ((entity.getName() != null && entity.getName().contains(key))
                    || (entity.getCode() != null && entity.getCode().contains(key))) {
                hit.add(entity.getId());
            }
        }
        // 命中节点向上补全祖先链路
        Set<Long> visible = new HashSet<>(hit);
        for (Long id : hit) {
            Long parentId = entityMap.get(id).getParentId();
            while (parentId != null && entityMap.containsKey(parentId) && visible.add(parentId)) {
                parentId = entityMap.get(parentId).getParentId();
            }
        }
        Map<Long, CategoryTreeVO> voMap = new HashMap<>();
        List<CategoryTreeVO> roots = new ArrayList<>();
        for (CategoryEntity entity : all) {
            if (visible.contains(entity.getId())) {
                voMap.put(entity.getId(), new CategoryTreeVO(entity,
                        modelRepository.countByCategoryIdAndDelFlag(entity.getId(), 0)));
            }
        }
        for (Long id : visible) {
            CategoryTreeVO node = voMap.get(id);
            CategoryEntity entity = entityMap.get(id);
            CategoryTreeVO parent = entity.getParentId() == null ? null : voMap.get(entity.getParentId());
            if (parent == null) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        roots.sort((a, b) -> Long.compare(a.getId(), b.getId()));
        return roots;
    }

    public CategoryEntity create(CategoryEntity entity) {
        validateCode(entity.getCode(), null);
        validateParent(entity.getParentId(), null);
        entity.setId(null);
        entity.setCreatedBy(operator());
        entity.setCreatedTime(Times.now());
        entity.setDelFlag(0);
        CategoryEntity saved = categoryRepository.save(entity);
        logService.log("CATEGORY", "CREATE", saved.getId(), saved.getName(),
                Map.of("code", saved.getCode()));
        return saved;
    }

    public CategoryEntity update(Long id, CategoryEntity entity) {
        CategoryEntity exist = categoryRepository.findByIdAndDelFlag(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "分类不存在：" + id));
        validateCode(entity.getCode(), id);
        validateParent(entity.getParentId(), id);
        exist.setCode(entity.getCode());
        exist.setName(entity.getName());
        exist.setParentId(entity.getParentId());
        exist.setSortNo(entity.getSortNo());
        exist.setDescription(entity.getDescription());
        exist.setUpdatedBy(operator());
        exist.setUpdatedTime(Times.now());
        CategoryEntity saved = categoryRepository.save(exist);
        logService.log("CATEGORY", "UPDATE", saved.getId(), saved.getName(),
                Map.of("code", saved.getCode()));
        return saved;
    }

    /**
     * 删除分类：存在子分类或已关联模型时拒绝（40901）。
     */
    @Transactional
    public void delete(Long id) {
        CategoryEntity exist = categoryRepository.findByIdAndDelFlag(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "分类不存在：" + id));
        if (categoryRepository.countByParentIdAndDelFlag(id, 0) > 0) {
            throw new BusinessException(ErrorCode.CATEGORY_HAS_CHILDREN, "分类下存在子分类，不允许删除");
        }
        if (modelRepository.countByCategoryIdAndDelFlag(id, 0) > 0) {
            throw new BusinessException(ErrorCode.CATEGORY_HAS_CHILDREN, "分类下已关联数据模型，不允许删除");
        }
        exist.setDelFlag(1);
        exist.setUpdatedBy(operator());
        exist.setUpdatedTime(Times.now());
        categoryRepository.save(exist);
        logService.log("CATEGORY", "DELETE", id, exist.getName(), Map.of("code", exist.getCode()));
    }

    private void validateCode(String code, Long excludeId) {
        if (code == null || code.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "分类编码不能为空");
        }
        categoryRepository.findByCodeAndDelFlag(code.trim(), 0).ifPresent(exist -> {
            if (!exist.getId().equals(excludeId)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "分类编码已存在：" + code);
            }
        });
    }

    /** 父分类存在性 + 防环（父不可为自身或其后代）。 */
    private void validateParent(Long parentId, Long selfId) {
        if (parentId == null) {
            return;
        }
        CategoryEntity parent = categoryRepository.findByIdAndDelFlag(parentId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "父分类不存在：" + parentId));
        if (selfId != null) {
            if (parentId.equals(selfId)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "父分类不能为自身");
            }
            Set<Long> ancestors = new HashSet<>();
            Long cursor = parent.getParentId();
            while (cursor != null && ancestors.add(cursor)) {
                if (cursor.equals(selfId)) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "不能将分类移动到自身后代节点下");
                }
                CategoryEntity ancestor = categoryRepository.findByIdAndDelFlag(cursor, 0).orElse(null);
                cursor = ancestor == null ? null : ancestor.getParentId();
            }
        }
    }

    private static String operator() {
        UserContext ctx = UserContext.get();
        return ctx == null ? "system" : ctx.operator();
    }
}
