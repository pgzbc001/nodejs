package com.mdm.platform.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdm.platform.audit.OperationLogService;
import com.mdm.platform.category.CategoryRepository;
import com.mdm.platform.coderule.CodeRuleEngine;
import com.mdm.platform.coderule.CodeRuleValidator;
import com.mdm.platform.common.BusinessException;
import com.mdm.platform.common.ErrorCode;
import com.mdm.platform.common.PageResult;
import com.mdm.platform.common.Times;
import com.mdm.platform.data.MasterDataRepository;
import com.mdm.platform.model.dto.CodeRuleSegment;
import com.mdm.platform.model.dto.ExtConfig;
import com.mdm.platform.model.dto.FieldDef;
import com.mdm.platform.model.dto.ModelCreateRequest;
import com.mdm.platform.model.dto.ModelDiffVO;
import com.mdm.platform.model.dto.ModelUpdateRequest;
import com.mdm.platform.model.dto.ModelVO;
import com.mdm.platform.model.dto.ModelVersionVO;
import com.mdm.platform.security.UserContext;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 模型服务（REQ-BKD02/03）：空白/继承创建、上线锁定（40903）、
 * 版本快照（CREATE/UPDATE/ONLINE/OFFLINE/ROLLBACK）、差异对比与回滚。
 */
@Service
public class ModelService {

    /** 上线模型仅展示类字段属性可变更 */
    private static final List<String> DISPLAY_PROPS = List.of(
            "label", "group", "listShow", "searchable", "popup", "defaultValue");

    private final ModelRepository modelRepository;
    private final ModelVersionRepository versionRepository;
    private final CategoryRepository categoryRepository;
    private final MasterDataRepository dataRepository;
    private final CodeRuleValidator codeRuleValidator;
    private final CodeRuleEngine codeRuleEngine;
    private final OperationLogService logService;
    private final ObjectMapper objectMapper;

    public ModelService(ModelRepository modelRepository,
                        ModelVersionRepository versionRepository,
                        CategoryRepository categoryRepository,
                        MasterDataRepository dataRepository,
                        CodeRuleValidator codeRuleValidator,
                        CodeRuleEngine codeRuleEngine,
                        OperationLogService logService) {
        this.modelRepository = modelRepository;
        this.versionRepository = versionRepository;
        this.categoryRepository = categoryRepository;
        this.dataRepository = dataRepository;
        this.codeRuleValidator = codeRuleValidator;
        this.codeRuleEngine = codeRuleEngine;
        this.logService = logService;
        this.objectMapper = new ObjectMapper();
    }

    // ==================== 创建 ====================

    /**
     * 创建模型：空白创建，或继承既有模型（复制字段/编码规则/扩展配置）。
     */
    @Transactional
    public ModelVO create(ModelCreateRequest request) {
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "模型编码不能为空");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "模型名称不能为空");
        }
        if (modelRepository.existsByCodeAndDelFlag(request.getCode().trim(), 0)) {
            throw new BusinessException(ErrorCode.MODEL_CODE_DUPLICATED, "模型编码已存在：" + request.getCode());
        }
        categoryRepository.findByIdAndDelFlag(request.getCategoryId(), 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "分类不存在：" + request.getCategoryId()));

        List<FieldDef> fieldDefs = request.getFieldDefs();
        List<CodeRuleSegment> codeRules = request.getCodeRules();
        ExtConfig extConfig = request.getExtConfig();
        if (request.getInheritFromId() != null) {
            ModelEntity source = requireModel(request.getInheritFromId());
            if (fieldDefs == null || fieldDefs.isEmpty()) {
                fieldDefs = parseList(source.getFieldDefs(), new TypeReference<List<FieldDef>>() {
                });
            }
            if (codeRules == null) {
                codeRules = codeRuleEngine.parseRules(source.getCodeRules());
            }
            if (extConfig == null) {
                extConfig = parse(source.getExtConfig(), ExtConfig.class);
            }
        }
        if (fieldDefs == null) {
            fieldDefs = new ArrayList<>();
        }
        validateFieldDefs(fieldDefs);
        if (codeRules != null && !codeRules.isEmpty()) {
            codeRuleValidator.validate(codeRules);
        }

        ModelEntity entity = new ModelEntity();
        entity.setCode(request.getCode().trim());
        entity.setName(request.getName());
        entity.setCategoryId(request.getCategoryId());
        entity.setDept(request.getDept());
        entity.setStatus("OFFLINE");
        entity.setVersionNo(1);
        entity.setFieldDefs(toJson(fieldDefs));
        entity.setCodeRules(codeRules == null || codeRules.isEmpty() ? null : toJson(codeRules));
        entity.setExtConfig(extConfig == null ? null : toJson(extConfig));
        entity.setDescription(request.getDescription());
        entity.setCreatedBy(operator());
        entity.setCreatedTime(Times.now());
        ModelEntity saved = modelRepository.save(entity);

        snapshot(saved, "CREATE");
        logService.log("MODEL", "CREATE", saved.getId(), saved.getName(),
                Map.of("code", saved.getCode(), "fieldCount", fieldDefs.size()));
        return toVO(saved);
    }

    // ==================== 更新（上线锁定） ====================

    /**
     * 更新模型：OFFLINE 自由修改；ONLINE 仅允许基础信息、展示类属性与新增字段（40903）。
     */
    @Transactional
    public ModelVO update(Long id, ModelUpdateRequest request) {
        ModelEntity exist = requireModel(id);
        List<FieldDef> newDefs = request.getFieldDefs() == null ? new ArrayList<>() : request.getFieldDefs();
        validateFieldDefs(newDefs);

        if (exist.isOnline()) {
            List<FieldDef> oldDefs = parseList(exist.getFieldDefs(), new TypeReference<List<FieldDef>>() {
            });
            applyLockedUpdate(exist, oldDefs, newDefs);
            if (request.getCodeRules() != null) {
                List<CodeRuleSegment> oldRules = codeRuleEngine.parseRules(exist.getCodeRules());
                if (!sameJson(oldRules, request.getCodeRules())) {
                    throw new BusinessException(ErrorCode.MODEL_STRUCTURE_LOCKED, "模型已上线，编码规则不可变更");
                }
            }
            if (request.getCategoryId() != null && !request.getCategoryId().equals(exist.getCategoryId())) {
                throw new BusinessException(ErrorCode.MODEL_STRUCTURE_LOCKED, "模型已上线，所属分类不可变更");
            }
        } else {
            if (request.getCategoryId() != null) {
                categoryRepository.findByIdAndDelFlag(request.getCategoryId(), 0)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "分类不存在：" + request.getCategoryId()));
                exist.setCategoryId(request.getCategoryId());
            }
            exist.setFieldDefs(toJson(newDefs));
            if (request.getCodeRules() != null) {
                if (!request.getCodeRules().isEmpty()) {
                    codeRuleValidator.validate(request.getCodeRules());
                }
                exist.setCodeRules(request.getCodeRules().isEmpty() ? null : toJson(request.getCodeRules()));
            }
            if (request.getExtConfig() != null) {
                exist.setExtConfig(toJson(request.getExtConfig()));
            }
        }
        if (request.getName() != null && !request.getName().isBlank()) {
            exist.setName(request.getName());
        }
        if (request.getDept() != null) {
            exist.setDept(request.getDept());
        }
        if (request.getDescription() != null) {
            exist.setDescription(request.getDescription());
        }
        exist.setVersionNo(exist.getVersionNo() + 1);
        exist.setUpdatedBy(operator());
        exist.setUpdatedTime(Times.now());
        ModelEntity saved = modelRepository.save(exist);

        snapshot(saved, "UPDATE");
        logService.log("MODEL", "UPDATE", saved.getId(), saved.getName(),
                Map.of("versionNo", saved.getVersionNo(), "status", saved.getStatus()));
        return toVO(saved);
    }

    /**
     * 上线锁定下的字段合并：允许新增字段与展示类属性修改，结构属性变更/删除字段一律 40903。
     */
    private void applyLockedUpdate(ModelEntity exist, List<FieldDef> oldDefs, List<FieldDef> newDefs) {
        Map<String, FieldDef> oldMap = byName(oldDefs);
        Map<String, FieldDef> newMap = byName(newDefs);
        for (String name : oldMap.keySet()) {
            if (!newMap.containsKey(name)) {
                throw new BusinessException(ErrorCode.MODEL_STRUCTURE_LOCKED,
                        "模型已上线，不允许删除字段：" + name);
            }
        }
        List<FieldDef> merged = new ArrayList<>();
        for (FieldDef def : newDefs) {
            FieldDef old = oldMap.get(def.getName());
            if (old == null) {
                merged.add(def);
                continue;
            }
            assertStructuralUnchanged(def, old);
            // 展示类属性取新值，结构属性保持原值
            old.setLabel(def.getLabel());
            old.setGroup(def.getGroup());
            old.setListShow(def.isListShow());
            old.setSearchable(def.isSearchable());
            old.setPopup(def.isPopup());
            old.setDefaultValue(def.getDefaultValue());
            merged.add(old);
        }
        exist.setFieldDefs(toJson(merged));
    }

    /** 逐属性比较结构属性，任一差异抛 40903。 */
    private void assertStructuralUnchanged(FieldDef next, FieldDef old) {
        Map<String, Object> oldProps = objectMapper.convertValue(old,
                objectMapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Object.class));
        Map<String, Object> newProps = objectMapper.convertValue(next,
                objectMapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Object.class));
        for (String prop : oldProps.keySet()) {
            if (DISPLAY_PROPS.contains(prop)) {
                continue;
            }
            if (!java.util.Objects.equals(oldProps.get(prop), newProps.get(prop))) {
                throw new BusinessException(ErrorCode.MODEL_STRUCTURE_LOCKED,
                        "模型已上线，字段【" + old.getName() + "】结构属性【" + prop + "】已锁定");
            }
        }
    }

    private void validateFieldDefs(List<FieldDef> fieldDefs) {
        Map<String, FieldDef> names = new HashMap<>();
        for (FieldDef def : fieldDefs) {
            if (def.getName() == null || def.getName().isBlank()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "字段名称（英文标识）不能为空");
            }
            if (def.getLabel() == null || def.getLabel().isBlank()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "字段【" + def.getName() + "】显示名称不能为空");
            }
            if (def.getType() == null || def.getType().isBlank()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "字段【" + def.getName() + "】数据类型不能为空");
            }
            if (names.put(def.getName(), def) != null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "字段名称重复：" + def.getName());
            }
        }
    }

    // ==================== 上线/下线 ====================

    /**
     * 上线：≥1 字段且编码规则合法；成功后 ONLINE + 快照 + 日志。
     */
    @Transactional
    public ModelVO online(Long id) {
        ModelEntity exist = requireModel(id);
        if (exist.isOnline()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "模型已上线，无需重复操作");
        }
        List<FieldDef> defs = parseList(exist.getFieldDefs(), new TypeReference<List<FieldDef>>() {
        });
        if (defs.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "模型至少需要一个字段才能上线");
        }
        List<CodeRuleSegment> rules = codeRuleEngine.parseRules(exist.getCodeRules());
        if (rules != null && !rules.isEmpty()) {
            codeRuleValidator.validate(rules);
        }
        exist.setStatus("ONLINE");
        exist.setVersionNo(exist.getVersionNo() + 1);
        exist.setUpdatedBy(operator());
        exist.setUpdatedTime(Times.now());
        ModelEntity saved = modelRepository.save(exist);
        snapshot(saved, "ONLINE");
        logService.log("MODEL", "ONLINE", saved.getId(), saved.getName(),
                Map.of("versionNo", saved.getVersionNo()));
        return toVO(saved);
    }

    /**
     * 下线：直接 OFFLINE + 快照 + 日志。
     */
    @Transactional
    public ModelVO offline(Long id) {
        ModelEntity exist = requireModel(id);
        if (!exist.isOnline()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "模型未上线，无需下线");
        }
        exist.setStatus("OFFLINE");
        exist.setVersionNo(exist.getVersionNo() + 1);
        exist.setUpdatedBy(operator());
        exist.setUpdatedTime(Times.now());
        ModelEntity saved = modelRepository.save(exist);
        snapshot(saved, "OFFLINE");
        logService.log("MODEL", "OFFLINE", saved.getId(), saved.getName(),
                Map.of("versionNo", saved.getVersionNo()));
        return toVO(saved);
    }

    /**
     * 删除模型：上线中或有主数据时拒绝。
     */
    @Transactional
    public void delete(Long id) {
        ModelEntity exist = requireModel(id);
        if (exist.isOnline()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "模型上线中，请先下线再删除");
        }
        if (dataRepository.countByModelIdAndDelFlag(id, 0) > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "模型下存在主数据，不允许删除");
        }
        exist.setDelFlag(1);
        exist.setUpdatedBy(operator());
        exist.setUpdatedTime(Times.now());
        modelRepository.save(exist);
        logService.log("MODEL", "DELETE", id, exist.getName(), Map.of("code", exist.getCode()));
    }

    // ==================== 查询 ====================

    public ModelVO detail(Long id) {
        return toVO(requireModel(id));
    }

    /**
     * 分页查询：关键字（编码/名称模糊）+ 分类 + 状态。
     */
    public PageResult<ModelVO> page(String keyword, Long categoryId, String status, int page, int size) {
        Specification<ModelEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("delFlag"), 0));
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(cb.like(root.get("code"), like), cb.like(root.get("name"), like)));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("categoryId"), categoryId));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status.trim()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, size),
                Sort.by(Sort.Direction.DESC, "updatedTime").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<ModelEntity> result = modelRepository.findAll(spec, pageable);
        List<ModelVO> vos = result.getContent().stream().map(this::toVO).toList();
        return new PageResult<>(result.getTotalElements(), page, size, vos);
    }

    // ==================== 版本 ====================

    /**
     * 版本列表（新版本在前）。
     */
    public List<ModelVersionVO> versions(Long modelId) {
        requireModel(modelId);
        return versionRepository.findByModelIdAndDelFlagOrderByVersionNoDesc(modelId, 0).stream()
                .map(this::toVersionVO)
                .toList();
    }

    /**
     * 版本差异：基础信息/字段/编码规则/扩展配置逐项对比。
     */
    public ModelDiffVO diff(Long modelId, Integer fromVersion, Integer toVersion) {
        requireModel(modelId);
        ModelEntity from = loadSnapshot(modelId, fromVersion);
        ModelEntity to = loadSnapshot(modelId, toVersion);
        ModelDiffVO vo = new ModelDiffVO();
        vo.setFromVersion(fromVersion);
        vo.setToVersion(toVersion);
        List<ModelDiffVO.ItemChange> changes = new ArrayList<>();

        addBaseChange(changes, "name", from.getName(), to.getName());
        addBaseChange(changes, "dept", from.getDept(), to.getDept());
        addBaseChange(changes, "description", from.getDescription(), to.getDescription());
        addBaseChange(changes, "status", from.getStatus(), to.getStatus());

        diffFieldDefs(changes,
                parseList(from.getFieldDefs(), new TypeReference<List<FieldDef>>() {
                }),
                parseList(to.getFieldDefs(), new TypeReference<List<FieldDef>>() {
                }));
        addBaseChange(changes, "codeRules", from.getCodeRules(), to.getCodeRules());
        addBaseChange(changes, "extConfig", from.getExtConfig(), to.getExtConfig());

        vo.setChanges(changes);
        return vo;
    }

    /**
     * 回滚到指定版本：以快照覆写当前定义，版本号 +1 并落 ROLLBACK 快照。
     */
    @Transactional
    public ModelVO rollback(Long modelId, Integer versionNo) {
        ModelEntity exist = requireModel(modelId);
        if (exist.getVersionNo().equals(versionNo)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前版本即 v" + versionNo + "，无需回滚");
        }
        ModelEntity snapshot = loadSnapshot(modelId, versionNo);
        exist.setName(snapshot.getName());
        exist.setCategoryId(snapshot.getCategoryId());
        exist.setDept(snapshot.getDept());
        exist.setStatus(snapshot.getStatus());
        exist.setFieldDefs(snapshot.getFieldDefs());
        exist.setCodeRules(snapshot.getCodeRules());
        exist.setExtConfig(snapshot.getExtConfig());
        exist.setDescription(snapshot.getDescription());
        exist.setVersionNo(exist.getVersionNo() + 1);
        exist.setUpdatedBy(operator());
        exist.setUpdatedTime(Times.now());
        ModelEntity saved = modelRepository.save(exist);
        snapshot(saved, "ROLLBACK");
        logService.log("MODEL", "ROLLBACK", saved.getId(), saved.getName(),
                Map.of("rollbackTo", versionNo, "newVersion", saved.getVersionNo()));
        return toVO(saved);
    }

    // ==================== 内部工具 ====================

    private void diffFieldDefs(List<ModelDiffVO.ItemChange> changes, List<FieldDef> fromDefs, List<FieldDef> toDefs) {
        Map<String, FieldDef> fromMap = byName(fromDefs);
        Map<String, FieldDef> toMap = byName(toDefs);
        for (String name : toMap.keySet()) {
            if (!fromMap.containsKey(name)) {
                changes.add(new ModelDiffVO.ItemChange("FIELD_ADD", name, null, summary(toMap.get(name))));
            }
        }
        for (String name : fromMap.keySet()) {
            if (!toMap.containsKey(name)) {
                changes.add(new ModelDiffVO.ItemChange("FIELD_REMOVE", name, summary(fromMap.get(name)), null));
            }
        }
        for (String name : fromMap.keySet()) {
            FieldDef from = fromMap.get(name);
            FieldDef to = toMap.get(name);
            if (to == null) {
                continue;
            }
            Map<String, Object> fromProps = objectMapper.convertValue(from,
                    objectMapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Object.class));
            Map<String, Object> toProps = objectMapper.convertValue(to,
                    objectMapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Object.class));
            for (String prop : fromProps.keySet()) {
                if (!java.util.Objects.equals(fromProps.get(prop), toProps.get(prop))) {
                    changes.add(new ModelDiffVO.ItemChange("FIELD_MODIFY", name + "." + prop,
                            textOf(fromProps.get(prop)), textOf(toProps.get(prop))));
                }
            }
        }
    }

    private void addBaseChange(List<ModelDiffVO.ItemChange> changes, String field, String oldVal, String newVal) {
        if (!java.util.Objects.equals(oldVal, newVal)) {
            changes.add(new ModelDiffVO.ItemChange("BASE", field, oldVal, newVal));
        }
    }

    private String summary(FieldDef def) {
        return def.getLabel() + "（" + def.getType() + "）";
    }

    private String textOf(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /** 加载指定版本快照并还原为实体形态（不落库）。 */
    private ModelEntity loadSnapshot(Long modelId, Integer versionNo) {
        ModelVersionEntity version = versionRepository
                .findByModelIdAndVersionNoAndDelFlag(modelId, versionNo, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "版本不存在：v" + versionNo));
        ModelVO vo = parse(version.getSnapshot(), ModelVO.class);
        ModelEntity entity = new ModelEntity();
        entity.setCode(vo.getCode());
        entity.setName(vo.getName());
        entity.setCategoryId(vo.getCategoryId());
        entity.setDept(vo.getDept());
        entity.setStatus(vo.getStatus());
        entity.setFieldDefs(toJson(vo.getFieldDefs() == null ? new ArrayList<>() : vo.getFieldDefs()));
        entity.setCodeRules(vo.getCodeRules() == null ? null : toJson(vo.getCodeRules()));
        entity.setExtConfig(vo.getExtConfig() == null ? null : toJson(vo.getExtConfig()));
        entity.setDescription(vo.getDescription());
        return entity;
    }

    /** 落版本快照（快照为 ModelVO JSON）。 */
    private void snapshot(ModelEntity entity, String operation) {
        ModelVersionEntity version = new ModelVersionEntity();
        version.setModelId(entity.getId());
        version.setVersionNo(entity.getVersionNo());
        version.setStatus(entity.getStatus());
        version.setSnapshot(toJson(toVO(entity)));
        version.setOperation(operation);
        version.setOperator(operator());
        version.setOperatedTime(Times.now());
        versionRepository.save(version);
    }

    private ModelVersionVO toVersionVO(ModelVersionEntity entity) {
        ModelVersionVO vo = new ModelVersionVO();
        vo.setId(entity.getId());
        vo.setModelId(entity.getModelId());
        vo.setVersionNo(entity.getVersionNo());
        vo.setStatus(entity.getStatus());
        vo.setOperation(entity.getOperation());
        vo.setOperator(entity.getOperator());
        vo.setOperatedTime(entity.getOperatedTime());
        return vo;
    }

    public ModelVO toVO(ModelEntity entity) {
        ModelVO vo = new ModelVO();
        vo.setId(entity.getId());
        vo.setCode(entity.getCode());
        vo.setName(entity.getName());
        vo.setCategoryId(entity.getCategoryId());
        vo.setCategoryName(categoryRepository.findByIdAndDelFlag(entity.getCategoryId(), 0)
                .map(c -> c.getName()).orElse(null));
        vo.setDept(entity.getDept());
        vo.setStatus(entity.getStatus());
        vo.setVersionNo(entity.getVersionNo());
        vo.setFieldDefs(parseList(entity.getFieldDefs(), new TypeReference<List<FieldDef>>() {
        }));
        vo.setCodeRules(codeRuleEngine.parseRules(entity.getCodeRules()));
        vo.setExtConfig(parse(entity.getExtConfig(), ExtConfig.class));
        vo.setDescription(entity.getDescription());
        vo.setCreatedBy(entity.getCreatedBy());
        vo.setCreatedTime(entity.getCreatedTime());
        vo.setUpdatedBy(entity.getUpdatedBy());
        vo.setUpdatedTime(entity.getUpdatedTime());
        return vo;
    }

    public ModelEntity requireModel(Long id) {
        return modelRepository.findByIdAndDelFlag(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "模型不存在：" + id));
    }

    private static Map<String, FieldDef> byName(List<FieldDef> defs) {
        Map<String, FieldDef> map = new LinkedHashMap<>();
        for (FieldDef def : defs) {
            map.put(def.getName(), def);
        }
        return map;
    }

    private boolean sameJson(Object oldRules, Object newRules) {
        return toJson(oldRules).equals(toJson(newRules));
    }

    private <T> List<T> parseList(String json, TypeReference<List<T>> type) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<T> list = objectMapper.readValue(json, type);
            return list == null ? new ArrayList<>() : list;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "模型 JSON 解析失败：" + ex.getMessage());
        }
    }

    private <T> T parse(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "模型 JSON 解析失败：" + ex.getMessage());
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "模型 JSON 序列化失败");
        }
    }

    private static String operator() {
        UserContext ctx = UserContext.get();
        return ctx == null ? "system" : ctx.operator();
    }
}
