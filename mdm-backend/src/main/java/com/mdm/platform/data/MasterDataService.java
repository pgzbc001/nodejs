package com.mdm.platform.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdm.platform.audit.OperationLogService;
import com.mdm.platform.common.BusinessException;
import com.mdm.platform.common.ErrorCode;
import com.mdm.platform.common.PageResult;
import com.mdm.platform.common.Times;
import com.mdm.platform.coderule.CodeRuleEngine;
import com.mdm.platform.data.dto.DataDetailVO;
import com.mdm.platform.data.dto.DataQueryRequest;
import com.mdm.platform.data.dto.DataUpsertRequest;
import com.mdm.platform.data.dto.DataViewSchemaVO;
import com.mdm.platform.data.dto.DataViewVO;
import com.mdm.platform.model.ModelEntity;
import com.mdm.platform.model.ModelRepository;
import com.mdm.platform.model.dto.FieldDef;
import com.mdm.platform.push.CollaborationEntity;
import com.mdm.platform.push.CollaborationRepository;
import com.mdm.platform.push.PushService;
import com.mdm.platform.quality.QualityResultEntity;
import com.mdm.platform.quality.QualityRuleService;
import com.mdm.platform.quality.dto.QualityViolationVO;
import com.mdm.platform.security.CryptoService;
import com.mdm.platform.security.UserContext;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 主数据服务（REQ-BKD04~07）：动态 CRUD、编码生成、唯一/自定义校验、
 * 加密入库、协同触发、禁用/删除下游保护、动态列表。
 */
@Service
public class MasterDataService {

    private static final Pattern PHONE = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern ID_CARD = Pattern.compile("^\\d{17}[\\dXx]$");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final MasterDataRepository dataRepository;
    private final ModelRepository modelRepository;
    private final CodeRuleEngine codeRuleEngine;
    private final QualityRuleService qualityRuleService;
    private final VersionService versionService;
    private final PushService pushService;
    private final CollaborationRepository collaborationRepository;
    private final CryptoService cryptoService;
    private final OperationLogService logService;
    private final ObjectMapper objectMapper;

    public MasterDataService(MasterDataRepository dataRepository,
                             ModelRepository modelRepository,
                             CodeRuleEngine codeRuleEngine,
                             QualityRuleService qualityRuleService,
                             VersionService versionService,
                             PushService pushService,
                             CollaborationRepository collaborationRepository,
                             CryptoService cryptoService,
                             OperationLogService logService) {
        this.dataRepository = dataRepository;
        this.modelRepository = modelRepository;
        this.codeRuleEngine = codeRuleEngine;
        this.qualityRuleService = qualityRuleService;
        this.versionService = versionService;
        this.pushService = pushService;
        this.collaborationRepository = collaborationRepository;
        this.cryptoService = cryptoService;
        this.logService = logService;
        this.objectMapper = new ObjectMapper();
    }

    // ==================== 动态视图 ====================

    /**
     * 动态列表视图定义：listShow 列 + searchable 筛选 + 全字段。
     */
    public DataViewSchemaVO view(Long modelId) {
        ModelEntity model = requireModel(modelId);
        List<FieldDef> all = fieldDefs(model);
        DataViewSchemaVO vo = new DataViewSchemaVO();
        vo.setModelId(model.getId());
        vo.setModelCode(model.getCode());
        vo.setModelName(model.getName());
        vo.setAllFields(all);
        vo.setListFields(all.stream().filter(FieldDef::isListShow).toList());
        vo.setSearchFields(all.stream().filter(FieldDef::isSearchable).toList());
        return vo;
    }

    /**
     * 动态分页查询：SQL 层过滤（模型/状态/关键字）+ searchable 字段内存筛选。
     */
    public PageResult<DataViewVO> page(DataQueryRequest request) {
        ModelEntity model = requireModel(request.getModelId());
        Specification<MasterDataEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("delFlag"), 0));
            predicates.add(cb.equal(root.get("modelId"), model.getId()));
            if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
                String like = "%" + request.getKeyword().trim() + "%";
                predicates.add(cb.or(cb.like(root.get("code"), like), cb.like(root.get("name"), like)));
            }
            if (request.getStatus() != null && !request.getStatus().isBlank()) {
                predicates.add(cb.equal(root.get("status"), request.getStatus().trim()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE,
                Sort.by(Sort.Direction.DESC, "updatedTime").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<MasterDataEntity> sqlResult = dataRepository.findAll(spec, pageable);
        List<MasterDataEntity> filtered = memoryFilter(model, sqlResult.getContent(), request.getFilters());

        int page = Math.max(1, request.getPage());
        int size = Math.max(1, request.getSize());
        int fromIndex = Math.min((page - 1) * size, filtered.size());
        int toIndex = Math.min(fromIndex + size, filtered.size());
        List<DataViewVO> vos = filtered.subList(fromIndex, toIndex).stream()
                .map(entity -> buildViewVO(entity, true))
                .toList();
        return new PageResult<>(filtered.size(), page, size, vos);
    }

    /** searchable 字段内存筛选（值包含匹配，多选字段逗号拼接后匹配）。 */
    private List<MasterDataEntity> memoryFilter(ModelEntity model,
                                                List<MasterDataEntity> content,
                                                Map<String, String> filters) {
        if (filters == null || filters.isEmpty()) {
            return content;
        }
        List<FieldDef> searchFields = fieldDefs(model).stream().filter(FieldDef::isSearchable).toList();
        List<MasterDataEntity> result = new ArrayList<>();
        for (MasterDataEntity entity : content) {
            Map<String, Object> attrs = parse(entity.getAttributes());
            boolean match = true;
            for (Map.Entry<String, String> filter : filters.entrySet()) {
                if (filter.getValue() == null || filter.getValue().isBlank()) {
                    continue;
                }
                if (searchFields.stream().noneMatch(f -> f.getName().equals(filter.getKey()))) {
                    continue;
                }
                Object value = attrs.get(filter.getKey());
                String text = value == null ? "" : toText(value);
                if (!text.contains(filter.getValue().trim())) {
                    match = false;
                    break;
                }
            }
            if (match) {
                result.add(entity);
            }
        }
        return result;
    }

    /**
     * 详情：明文属性（供编辑回显）+ 质量结果 + 版本摘要。
     */
    public DataDetailVO detail(Long id) {
        MasterDataEntity entity = requireData(id);
        DataDetailVO vo = new DataDetailVO();
        vo.setData(buildViewVO(entity, false));
        vo.setQualityResults(qualityRuleService.listResults(id));
        vo.setVersions(versionService.versions(id));
        return vo;
    }

    // ==================== 新增 ====================

    /**
     * 新增主数据：模型须上线（40904）→ 动态校验 → 质量求值（40907）→
     * 编码生成/手填唯一校验 → 加密入库 → 快照 + 日志。
     */
    @Transactional
    public DataDetailVO create(Long modelId, DataUpsertRequest request) {
        ModelEntity model = requireModel(modelId);
        if (!model.isOnline()) {
            throw new BusinessException(ErrorCode.MODEL_NOT_ONLINE, "模型未上线，不允许录入数据");
        }
        Map<String, Object> attributes = request.getAttributes() == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(request.getAttributes());
        List<FieldDef> defs = fieldDefs(model);
        applyDefaults(defs, attributes);
        validateAttributes(defs, attributes);
        checkUnique(model, defs, attributes, null);
        List<QualityViolationVO> violations = checkQuality(model, attributes);

        String code;
        if (request.getCode() != null && !request.getCode().isBlank()) {
            code = request.getCode().trim();
            if (dataRepository.existsByModelIdAndCodeAndDelFlag(model.getId(), code, 0)) {
                throw new BusinessException(ErrorCode.DATA_CODE_DUPLICATED, "数据编码已存在：" + code);
            }
        } else {
            code = codeRuleEngine.generate(model, attributes);
        }

        MasterDataEntity entity = new MasterDataEntity();
        entity.setModelId(model.getId());
        entity.setCode(code);
        entity.setName(resolveName(defs, attributes));
        entity.setAttributes(toJson(encryptAttributes(defs, attributes)));
        entity.setStatus("VALID");
        entity.setVersionNo(1);
        entity.setCreatedBy(operator());
        entity.setCreatedTime(Times.now());
        MasterDataEntity saved = dataRepository.save(entity);

        persistAfterWrite(model, saved, violations, request.getIgnoredWarnings(), request.getIgnoreReason(), "CREATE", null);
        return detail(saved.getId());
    }

    // ==================== 修改 ====================

    /**
     * 修改主数据：待协同中禁止修改；敏感（绝密/机密）字段变更触发协同确认。
     */
    @Transactional
    public DataDetailVO update(Long id, DataUpsertRequest request) {
        MasterDataEntity entity = requireData(id);
        ModelEntity model = requireModel(entity.getModelId());
        if (!model.isOnline()) {
            throw new BusinessException(ErrorCode.MODEL_NOT_ONLINE, "模型未上线，不允许修改数据");
        }
        if ("PENDING_CONFIRM".equals(entity.getCollabStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "数据处于待协同确认状态，禁止修改");
        }
        Map<String, Object> attributes = request.getAttributes() == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(request.getAttributes());
        List<FieldDef> defs = fieldDefs(model);
        applyDefaults(defs, attributes);
        validateAttributes(defs, attributes);
        checkUnique(model, defs, attributes, id);
        List<QualityViolationVO> violations = checkQuality(model, attributes);

        Map<String, Object> oldAttrs = parse(entity.getAttributes());
        List<String> sensitiveChanges = sensitiveChanges(defs, oldAttrs, attributes);

        entity.setName(resolveName(defs, attributes));
        entity.setAttributes(toJson(encryptAttributes(defs, attributes)));
        entity.setVersionNo(entity.getVersionNo() + 1);
        entity.setUpdatedBy(operator());
        entity.setUpdatedTime(Times.now());
        if (!sensitiveChanges.isEmpty()) {
            entity.setCollabStatus("PENDING_CONFIRM");
        }
        MasterDataEntity saved = dataRepository.save(entity);

        if (!sensitiveChanges.isEmpty()) {
            createCollaboration(model, saved, sensitiveChanges);
        }
        persistAfterWrite(model, saved, violations, request.getIgnoredWarnings(), request.getIgnoreReason(), "UPDATE", null);
        return detail(saved.getId());
    }

    /** 绝密/机密字段值变更清单（Q&A Q3 协同判定）。 */
    private List<String> sensitiveChanges(List<FieldDef> defs, Map<String, Object> oldAttrs,
                                          Map<String, Object> newAttrs) {
        List<String> changes = new ArrayList<>();
        for (FieldDef def : defs) {
            String level = def.getSecurityLevel() == null ? "" : def.getSecurityLevel().toUpperCase(Locale.ROOT);
            if (!"TOP_SECRET".equals(level) && !"CONFIDENTIAL".equals(level)) {
                continue;
            }
            String oldVal = oldAttrs.containsKey(def.getName())
                    ? plainValue(def, oldAttrs.get(def.getName())) : null;
            String newVal = newAttrs.containsKey(def.getName())
                    ? plainValue(def, newAttrs.get(def.getName())) : null;
            if (!Objects.equals(oldVal, newVal)) {
                changes.add(def.getLabel() + "（" + def.getName() + "）");
            }
        }
        return changes;
    }

    private void createCollaboration(ModelEntity model, MasterDataEntity data, List<String> sensitiveChanges) {
        CollaborationEntity collab = new CollaborationEntity();
        collab.setDataId(data.getId());
        collab.setModelId(model.getId());
        collab.setReason("敏感字段发生变更：" + String.join("、", sensitiveChanges));
        collab.setStatus("PENDING");
        collab.setApplicant(operator());
        collab.setApplyTime(Times.now());
        collaborationRepository.save(collab);
        logService.log("COLLABORATION", "APPLY", collab.getId(), "发起协同确认：" + data.getCode(),
                Map.of("reason", collab.getReason()));
    }

    // ==================== 禁用/启用/删除 ====================

    /**
     * 禁用：下游预检 FAIL 且未强制时拦截（40909）。
     */
    @Transactional
    public DataDetailVO disable(Long id, boolean force) {
        MasterDataEntity entity = requireData(id);
        if (!"VALID".equals(entity.getStatus()) && !"DRAFT".equals(entity.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅有效/草稿数据可禁用");
        }
        if (!force && pushService.hasDownstreamFail(id)) {
            throw new BusinessException(ErrorCode.DOWNSTREAM_CHECK_FAILED,
                    "数据【" + entity.getCode() + "】存在下游系统校验未通过，未获强制执行确认");
        }
        entity.setStatus("DISABLED");
        entity.setVersionNo(entity.getVersionNo() + 1);
        entity.setUpdatedBy(operator());
        entity.setUpdatedTime(Times.now());
        MasterDataEntity saved = dataRepository.save(entity);
        versionService.snapshot(saved, "DISABLE", force ? "强制禁用" : null);
        logService.log("DATA", "DISABLE", id, saved.getCode(), Map.of("force", force));
        return detail(saved.getId());
    }

    /**
     * 启用：DISABLED → VALID。
     */
    @Transactional
    public DataDetailVO enable(Long id) {
        MasterDataEntity entity = requireData(id);
        if (!"DISABLED".equals(entity.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅禁用数据可启用");
        }
        entity.setStatus("VALID");
        entity.setVersionNo(entity.getVersionNo() + 1);
        entity.setUpdatedBy(operator());
        entity.setUpdatedTime(Times.now());
        MasterDataEntity saved = dataRepository.save(entity);
        versionService.snapshot(saved, "ENABLE", null);
        logService.log("DATA", "ENABLE", id, saved.getCode(), null);
        return detail(saved.getId());
    }

    /**
     * 逻辑删除：下游预检 FAIL 且未强制时拦截（40909）。
     */
    @Transactional
    public void delete(Long id, boolean force) {
        MasterDataEntity entity = requireData(id);
        if ("PENDING_CONFIRM".equals(entity.getCollabStatus())) {
            throw new BusinessException(ErrorCode.COLLAB_PENDING, "数据处于待协同确认状态，禁止删除");
        }
        if (!force && pushService.hasDownstreamFail(id)) {
            throw new BusinessException(ErrorCode.DOWNSTREAM_CHECK_FAILED,
                    "数据【" + entity.getCode() + "】存在下游系统校验未通过，未获强制执行确认");
        }
        entity.setVersionNo(entity.getVersionNo() + 1);
        versionService.snapshot(entity, "DELETE", force ? "强制删除" : null);
        entity.setDelFlag(1);
        entity.setUpdatedBy(operator());
        entity.setUpdatedTime(Times.now());
        dataRepository.save(entity);
        logService.log("DATA", "DELETE", id, entity.getCode(), Map.of("force", force));
    }

    // ==================== 校验 ====================

    /** 动态字段校验：必填/类型/值域/自定义规则。 */
    void validateAttributes(List<FieldDef> defs, Map<String, Object> attributes) {
        for (FieldDef def : defs) {
            Object raw = attributes.get(def.getName());
            String text = raw == null ? null : toText(raw);
            if (def.isRequired() && (text == null || text.isBlank())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "字段【" + def.getLabel() + "】不能为空");
            }
            if (text == null || text.isBlank()) {
                continue;
            }
            String type = def.getType() == null ? "TEXT" : def.getType().toUpperCase(Locale.ROOT);
            switch (type) {
                case "NUMBER" -> requireNumber(def, text);
                case "DATE" -> requireDate(def, text);
                default -> {
                    // TEXT / LONG_TEXT 无格式约束
                }
            }
            if (def.isSelectable() && "MANUAL".equalsIgnoreCase(nullToEmpty(def.getDomainSource()))) {
                checkDomain(def, raw);
            }
            if (def.getCustomRule() != null && !def.getCustomRule().isBlank()) {
                checkCustomRule(def, text);
            }
        }
    }

    private void requireNumber(FieldDef def, String text) {
        try {
            new BigDecimal(text.trim());
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "字段【" + def.getLabel() + "】必须为数字");
        }
    }

    private void requireDate(FieldDef def, String text) {
        try {
            LocalDate.parse(text.trim(), DATE);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "字段【" + def.getLabel() + "】日期格式应为 yyyy-MM-dd");
        }
    }

    private void checkDomain(FieldDef def, Object raw) {
        List<String> domain = def.getDomainValues();
        if (domain == null || domain.isEmpty()) {
            return;
        }
        List<String> values = new ArrayList<>();
        if (def.isMultiSelect() && raw instanceof List<?> list) {
            for (Object item : list) {
                values.add(String.valueOf(item));
            }
        } else {
            values.add(String.valueOf(raw));
        }
        for (String value : values) {
            if (!domain.contains(value)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "字段【" + def.getLabel() + "】取值超出值域：" + value);
            }
        }
    }

    private void checkCustomRule(FieldDef def, String text) {
        switch (def.getCustomRule().toUpperCase(Locale.ROOT)) {
            case "PHONE" -> {
                if (!PHONE.matcher(text.trim()).matches()) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "字段【" + def.getLabel() + "】手机号格式不正确");
                }
            }
            case "ID_CARD" -> {
                if (!ID_CARD.matcher(text.trim()).matches()) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "字段【" + def.getLabel() + "】身份证号格式不正确");
                }
            }
            case "NUMBER_RANGE" -> requireNumber(def, text);
            default -> {
                // 未知自定义规则不阻断（向前兼容）
            }
        }
    }

    /** 唯一字段值校验（加密字段解密后比较，40906）。 */
    void checkUnique(ModelEntity model, List<FieldDef> defs, Map<String, Object> attributes, Long excludeDataId) {
        List<FieldDef> uniqueFields = defs.stream().filter(FieldDef::isUnique).toList();
        if (uniqueFields.isEmpty()) {
            return;
        }
        List<MasterDataEntity> existing = dataRepository.findByModelIdAndDelFlag(model.getId(), 0);
        for (FieldDef def : uniqueFields) {
            Object raw = attributes.get(def.getName());
            if (raw == null || String.valueOf(raw).isBlank()) {
                continue;
            }
            String value = String.valueOf(raw);
            for (MasterDataEntity other : existing) {
                if (excludeDataId != null && excludeDataId.equals(other.getId())) {
                    continue;
                }
                Map<String, Object> otherAttrs = parse(other.getAttributes());
                Object otherRaw = otherAttrs.get(def.getName());
                if (otherRaw == null) {
                    continue;
                }
                String otherValue = def.isEncrypted()
                        ? cryptoService.decrypt(String.valueOf(otherRaw))
                        : String.valueOf(otherRaw);
                if (value.equals(otherValue)) {
                    throw new BusinessException(ErrorCode.FIELD_VALUE_DUPLICATED,
                            "字段【" + def.getLabel() + "】值已存在：" + value);
                }
            }
        }
    }

    /** 质量求值：CRITICAL 阻止提交（40907），返回违规清单供入库。 */
    private List<QualityViolationVO> checkQuality(ModelEntity model, Map<String, Object> attributes) {
        List<QualityViolationVO> violations = qualityRuleService.evaluate(model, attributes);
        if (qualityRuleService.hasCritical(violations)) {
            throw new BusinessException(ErrorCode.QUALITY_CRITICAL_BLOCKED, "存在严重级质量问题，必须修正后才能提交");
        }
        return violations;
    }

    // ==================== 工具 ====================

    /** 写库后统一落质量结果与操作日志。 */
    private void persistAfterWrite(ModelEntity model, MasterDataEntity saved,
                                   List<QualityViolationVO> violations,
                                   List<Long> ignoredWarnings, String ignoreReason,
                                   String operation, String remark) {
        java.util.Set<Long> ignored = ignoredWarnings == null ? java.util.Set.of() : new java.util.HashSet<>(ignoredWarnings);
        qualityRuleService.saveResults(saved.getId(), model.getId(), violations, ignored, ignoreReason);
        versionService.snapshot(saved, operation, remark);
        logService.log("DATA", operation, saved.getId(), saved.getCode(),
                Map.of("modelId", model.getId(), "versionNo", saved.getVersionNo()));
    }

    /** 默认值填充（未提供且配置了默认值）。 */
    private void applyDefaults(List<FieldDef> defs, Map<String, Object> attributes) {
        for (FieldDef def : defs) {
            if (!attributes.containsKey(def.getName()) || attributes.get(def.getName()) == null) {
                if (def.getDefaultValue() != null && !def.getDefaultValue().isBlank()) {
                    attributes.put(def.getName(), def.getDefaultValue());
                }
            }
        }
    }

    /** 加密字段值 AES 入库。 */
    private Map<String, Object> encryptAttributes(List<FieldDef> defs, Map<String, Object> attributes) {
        Map<String, Object> stored = new LinkedHashMap<>(attributes);
        for (FieldDef def : defs) {
            if (def.isEncrypted() && stored.get(def.getName()) != null) {
                stored.put(def.getName(), cryptoService.encrypt(String.valueOf(stored.get(def.getName()))));
            }
        }
        return stored;
    }

    /** 冗余名称：第一个必填 TEXT 字段，否则第一个 TEXT 字段。 */
    private String resolveName(List<FieldDef> defs, Map<String, Object> attributes) {
        FieldDef target = defs.stream()
                .filter(d -> d.isRequired() && "TEXT".equalsIgnoreCase(nullToEmpty(d.getType())))
                .findFirst()
                .orElseGet(() -> defs.stream()
                        .filter(d -> "TEXT".equalsIgnoreCase(nullToEmpty(d.getType())))
                        .findFirst()
                        .orElse(defs.isEmpty() ? null : defs.get(0)));
        if (target == null) {
            return "未命名";
        }
        Object value = attributes.get(target.getName());
        return value == null || String.valueOf(value).isBlank() ? "未命名" : String.valueOf(value);
    }

    /** 列表/详情视图：maskSensitive=true 时加密字段脱敏。 */
    public DataViewVO buildViewVO(MasterDataEntity entity, boolean maskSensitive) {
        ModelEntity model = modelRepository.findByIdAndDelFlag(entity.getModelId(), 0).orElse(null);
        Map<String, Object> attrs = parse(entity.getAttributes());
        Map<String, Object> view = new LinkedHashMap<>();
        for (FieldDef def : model == null ? List.<FieldDef>of() : fieldDefs(model)) {
            Object value = attrs.get(def.getName());
            if (value == null) {
                continue;
            }
            if (def.isEncrypted()) {
                String plain = cryptoService.decrypt(String.valueOf(value));
                view.put(def.getName(), maskSensitive ? CryptoService.mask(plain) : plain);
            } else {
                view.put(def.getName(), value);
            }
        }
        DataViewVO vo = new DataViewVO();
        vo.setId(entity.getId());
        vo.setModelId(entity.getModelId());
        vo.setCode(entity.getCode());
        vo.setName(entity.getName());
        vo.setStatus(entity.getStatus());
        vo.setCollabStatus(entity.getCollabStatus());
        vo.setVersionNo(entity.getVersionNo());
        vo.setUpdatedBy(entity.getUpdatedBy());
        vo.setUpdatedTime(entity.getUpdatedTime());
        vo.setAttributes(view);
        return vo;
    }

    private String plainValue(FieldDef def, Object stored) {
        if (stored == null) {
            return null;
        }
        String text = String.valueOf(stored);
        return def.isEncrypted() ? cryptoService.decrypt(text) : text;
    }

    public List<FieldDef> fieldDefs(ModelEntity model) {
        try {
            List<FieldDef> defs = objectMapper.readValue(model.getFieldDefs(),
                    new TypeReference<List<FieldDef>>() {
                    });
            return defs == null ? new ArrayList<>() : defs;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "字段定义 JSON 解析失败：" + ex.getMessage());
        }
    }

    private Map<String, Object> parse(String json) {
        try {
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
            return map == null ? new LinkedHashMap<>() : map;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "数据属性 JSON 解析失败：" + ex.getMessage());
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "数据属性 JSON 序列化失败");
        }
    }

    private String toText(Object value) {
        if (value instanceof List<?> list) {
            return String.join(",", list.stream().map(String::valueOf).toList());
        }
        return String.valueOf(value);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public MasterDataEntity requireData(Long id) {
        return dataRepository.findByIdAndDelFlag(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "主数据不存在：" + id));
    }

    public ModelEntity requireModel(Long modelId) {
        return modelRepository.findByIdAndDelFlag(modelId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "模型不存在：" + modelId));
    }

    private static String operator() {
        UserContext ctx = UserContext.get();
        return ctx == null ? "system" : ctx.operator();
    }
}
