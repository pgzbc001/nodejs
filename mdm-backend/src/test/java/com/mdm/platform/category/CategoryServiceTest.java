package com.mdm.platform.category;

import com.mdm.platform.audit.OperationLogService;
import com.mdm.platform.common.BusinessException;
import com.mdm.platform.common.ErrorCode;
import com.mdm.platform.model.ModelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 分类服务单元测试（tasks.md T18）：删除保护（子分类/关联模型）、树构建、搜索祖先补全、编码校验与防环。
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ModelRepository modelRepository;
    @Mock
    private OperationLogService logService;

    private CategoryService service;

    @BeforeEach
    void setUp() {
        service = new CategoryService(categoryRepository, modelRepository, logService);
    }

    private static CategoryEntity category(long id, String code, String name, Long parentId) {
        CategoryEntity entity = new CategoryEntity();
        entity.setId(id);
        entity.setCode(code);
        entity.setName(name);
        entity.setParentId(parentId);
        entity.setSortNo((int) id);
        entity.setDelFlag(0);
        return entity;
    }

    // ---------- 树构建 ----------

    @Test
    void treeShouldAssembleHierarchyWithModelCount() {
        when(categoryRepository.findByDelFlagOrderBySortNoAscIdAsc(0))
                .thenReturn(List.of(
                        category(1, "RAW", "原材料", null),
                        category(2, "RAW_METAL", "金属材料", 1L),
                        category(4, "FIN", "成品", null)));
        when(modelRepository.countByCategoryIdAndDelFlag(1L, 0)).thenReturn(2L);

        List<CategoryTreeVO> roots = service.tree();

        assertEquals(2, roots.size());
        CategoryTreeVO root = roots.get(0);
        assertEquals(1L, root.getId());
        assertEquals(2L, root.getModelCount());
        assertEquals(1, root.getChildren().size());
        assertEquals("金属材料", root.getChildren().get(0).getName());
        assertEquals(0L, roots.get(1).getModelCount());
    }

    // ---------- 搜索 ----------

    @Test
    void searchShouldCompleteAncestorChain() {
        when(categoryRepository.findByDelFlagOrderBySortNoAscIdAsc(0))
                .thenReturn(List.of(
                        category(1, "RAW", "原材料", null),
                        category(2, "RAW_METAL", "金属材料", 1L),
                        category(3, "RAW_STEEL", "钢板", 2L),
                        category(4, "FIN", "成品", null)));

        List<CategoryTreeVO> roots = service.search("钢板");

        assertEquals(1, roots.size());
        assertEquals("原材料", roots.get(0).getName());
        assertEquals("金属材料", roots.get(0).getChildren().get(0).getName());
        assertEquals("钢板", roots.get(0).getChildren().get(0).getChildren().get(0).getName());
    }

    // ---------- 删除保护 ----------

    @Test
    void deleteShouldRejectWhenChildrenExist() {
        when(categoryRepository.findByIdAndDelFlag(1L, 0))
                .thenReturn(Optional.of(category(1, "RAW", "原材料", null)));
        when(categoryRepository.countByParentIdAndDelFlag(1L, 0)).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete(1L));
        assertEquals(ErrorCode.CATEGORY_HAS_CHILDREN.getCode(), ex.getErrorCode().getCode());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void deleteShouldRejectWhenModelsBound() {
        when(categoryRepository.findByIdAndDelFlag(1L, 0))
                .thenReturn(Optional.of(category(1, "RAW", "原材料", null)));
        when(categoryRepository.countByParentIdAndDelFlag(1L, 0)).thenReturn(0L);
        when(modelRepository.countByCategoryIdAndDelFlag(1L, 0)).thenReturn(3L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete(1L));
        assertEquals(ErrorCode.CATEGORY_HAS_CHILDREN.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    void deleteShouldMarkDelFlagWhenNoProtection() {
        CategoryEntity entity = category(1, "RAW", "原材料", null);
        when(categoryRepository.findByIdAndDelFlag(1L, 0)).thenReturn(Optional.of(entity));
        when(categoryRepository.countByParentIdAndDelFlag(1L, 0)).thenReturn(0L);
        when(modelRepository.countByCategoryIdAndDelFlag(1L, 0)).thenReturn(0L);

        service.delete(1L);

        assertEquals(1, entity.getDelFlag());
        assertNotNull(entity.getUpdatedTime());
        verify(categoryRepository).save(entity);
        verify(logService).log(eq("CATEGORY"), eq("DELETE"), eq(1L), eq("原材料"), any());
    }

    // ---------- 创建 ----------

    @Test
    void createShouldRejectDuplicatedCode() {
        when(categoryRepository.findByCodeAndDelFlag("RAW", 0))
                .thenReturn(Optional.of(category(9, "RAW", "其他分类", null)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.create(category(0, "RAW", "原材料", null)));
        assertEquals(ErrorCode.BAD_REQUEST.getCode(), ex.getErrorCode().getCode());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void createShouldPersistWithAuditFields() {
        when(categoryRepository.findByCodeAndDelFlag("RAW", 0)).thenReturn(Optional.empty());
        when(categoryRepository.save(any(CategoryEntity.class))).thenAnswer(invocation -> {
            CategoryEntity saved = invocation.getArgument(0);
            saved.setId(11L);
            return saved;
        });

        CategoryEntity saved = service.create(category(0, "RAW", "原材料", null));

        assertEquals(11L, saved.getId());
        assertEquals(0, saved.getDelFlag());
        assertEquals("system", saved.getCreatedBy());
        assertNotNull(saved.getCreatedTime());
        verify(logService).log(eq("CATEGORY"), eq("CREATE"), eq(11L), eq("原材料"), any());
    }

    // ---------- 防环 ----------

    @Test
    void updateShouldRejectMovingUnderDescendant() {
        CategoryEntity self = category(1, "RAW", "原材料", null);
        CategoryEntity descendant = category(2, "RAW_METAL", "金属材料", 1L);
        CategoryEntity request = category(0, "RAW", "原材料", 2L);
        when(categoryRepository.findByIdAndDelFlag(1L, 0)).thenReturn(Optional.of(self));
        when(categoryRepository.findByCodeAndDelFlag("RAW", 0)).thenReturn(Optional.of(self));
        when(categoryRepository.findByIdAndDelFlag(2L, 0)).thenReturn(Optional.of(descendant));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.update(1L, request));
        assertEquals(ErrorCode.BAD_REQUEST.getCode(), ex.getErrorCode().getCode());
        verify(categoryRepository, never()).save(any());
    }
}
