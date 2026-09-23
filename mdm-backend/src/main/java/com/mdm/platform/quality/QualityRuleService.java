package com.mdm.platform.quality;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdm.platform.audit.OperationLogService;
import com.mdm.platform.common.BusinessException;
import com.mdm.platform.common.ErrorCode;
import com.mdm.platform.common.PageResult;
import com.mdm.platform.common.Times;
import com.mdm.platform.model.ModelEntity;
import com.mdm.platform.model.dto.FieldDef;
import com.mdm.platform.quality.dto.QualityRuleRequest;
import com.mdm.platform.quality.dto.QualityViolationVO;
import com.mdm.platform.security.UserContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 质量规则引擎（REQ-BKD10 / design.md 3.5）：
 * COMPLIANCE 合规（LENGTH_MAX/REGEX/NUM_RANGE）、CONSISTENCY 一致（值域/字段间比较）、
 * COMPLETENESS 完整（非空）；三级告警 CRITICAL/WARNING/INFO，CRITICAL 阻止提交。
 */
@Service
public class QualityRuleService {

    private final QualityRuleRepository ruleRepository;
    private final QualityResultRepository resultRepository;
    private final OperationLogService logService;
    private final ObjectMapper objectMapper;

    public QualityRuleService(QualityRuleRepository ruleRepository,
                              QualityResultRepository resultRepository,
                              OperationLogService logService) {
        this.ruleRepository = ruleRepository;
        this.resultRepository = resultRepository;
        this.logService = logService;
        this.objectMapper = new ObjectMapper();
    }

    // ==================== 规则求值 ====================

    /**
     * 数据提交前求值：模型规则 + 全局规则逐条检查。
     *
     * @param model      数据模型（提供字段定义，供值域类规则取 domainValues）
     * @param attributes 明文动态属性
     * @return 违规项列表（无违规返回空列表）
     */
    public List<QualityViolationVO> evaluate(ModelEntity model, Map<String, Object> attributes) {
        List<QualityRuleEntity> rules = rulesOf(model.getId());
        List<QualityViolationVO> violations = new ArrayList<>();
        for (QualityRuleEntity rule : rules) {
            if (!evaluateRule(rule, model, attributes)) {
                violations.add(new QualityViolationVO(rule.getId(), rule.getFieldName(),
                        rule.getSeverity(), rule.getMessage()));
            }
        }
        return violations;
    }

    /** 是否存在 CRITICAL 违规（阻止提交，40907）。 */
    public boolean hasCritical(List<QualityViolationVO> violations) {
        return violations.stream().anyMatch(v -> "CRITICAL".equals(v.getSeverity()));
    }

    /** 模型适用的启用规则：模型专属 + 全局。 */
    public List<QualityRuleEntity> rulesOf(Long modelId) {
        List<QualityRuleEntity> rules = new ArrayList<>(ruleRepository.findByModelIdAndEnabledAndDelFlag(modelId, 1, 0));
        rules.addAll(ruleRepository.findByModelIdIsNullAndEnabledAndDelFlag(1, 0));
        return rules;
    }

    /** 单条规则求值：true=通过。空值仅由 COMPLETENESS 判定，其余规则跳过空值。 */
    boolean evaluateRule(QualityRuleEntity rule, ModelEntity model, Map<String, Object> attributes) {
        String type = rule.getRuleType() == null ? "" : rule.getRuleType().trim().toUpperCase(Locale.ROOT);
        JsonNode expr = parseExpression(rule.getExpression());
        String value = textOf(attributes, rule.getFieldName());
        return switch (type) {
            case "COMPLIANCE" -> checkCompliance(expr, value);
            case "CONSISTENCY" -> checkConsistency(expr, model, attributes, value);
            case "COMPLETENESS" -> value != null && !value.isBlank();
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "未知质量规则类型：" + rule.getRuleType());
        };
    }

    /** 合规：LENGTH_MAX / REGEX / NUM_RANGE。 */
    private boolean checkCompliance(JsonNode expr, String value) {
        String op = text(expr, "op");
        JsonNode param = expr.get("value");
        if (value == null || value.isBlank()) {
            return true;
        }
        switch (op == null ? "" : op) {
            case "LENGTH_MAX" -> {
                return value.length() <= param.asInt();
            }
            case "REGEX" -> {
                return value.matches(param.asText());
            }
            case "NUM_RANGE" -> {
                try {
                    double num = Double.parseDouble(value.trim());
                    return num >= param.get(0).asDouble() && num <= param.get(1).asDouble();
                } catch (NumberFormatException ex) {
                    return false;
                }
            }
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "未知合规操作符：" + op);
        }
    }

    /** 一致：inDomain 值域归属 / fieldA op fieldB 字段间比较。 */
    private boolean checkConsistency(JsonNode expr, ModelEntity model, Map<String, Object> attributes, String value) {
        if (expr.hasNonNull("fieldA") && expr.hasNonNull("fieldB")) {
            String a = textOf(attributes, text(expr, "fieldA"));
            String b = textOf(attributes, text(expr, "fieldB"));
            if (a == null || b == null || a.isBlank() || b.isBlank()) {
                return true;
            }
            String op = text(expr, "op");
            try {
                double na = Double.parseDouble(a.trim());
                double nb = Double.parseDouble(b.trim());
                return switch (op == null ? "" : op) {
                    case "EQ" -> na == nb;
                    case "NEQ" -> na != nb;
                    case "LT" -> na < nb;
                    case "LTE" -> na <= nb;
                    case "GT" -> na > nb;
                    case "GTE" -> na >= nb;
                    default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "未知比较操作符：" + op);
                };
            } catch (NumberFormatException ex) {
                return false;
            }
        }
        if (expr.path("inDomain").asBoolean(false)) {
            if (value == null || value.isBlank()) {
                return true;
            }
            List<String> domain = domainValues(model, text(expr, "refField"));
            if (domain == null || domain.isEmpty()) {
                return true;
            }
            return domain.contains(value);
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "一致性规则表达式非法，需 inDomain 或 fieldA/fieldB");
    }

    /** 从模型字段定义取值域（domainValues）。 */
    private List<String> domainValues(ModelEntity model, String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            return null;
        }
        try {
            List<FieldDef> defs = objectMapper.readValue(model.getFieldDefs(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, FieldDef.class));
            for (FieldDef def : defs) {
                if (fieldName.equals(def.getName())) {
                    return def.getDomainValues();
                }
            }
            return null;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "字段定义 JSON 解析失败：" + ex.getMessage());
        }
    }

    // ==================== 结果保存与忽略 ====================

    /**
     * 保存质量检查结果（重新检查前归档旧结果）；
     * ignoredRuleIds 命中的 WARNING/INFO 项标记为已忽略并记录原因。
     */
    @Transactional
    public void saveResults(Long dataId, Long modelId, List<QualityViolationVO> violations,
                            java.util.Set<Long> ignoredRuleIds, String ignoreReason) {
        resultRepository.archiveByDataId(dataId);
        for (QualityViolationVO violation : violations) {
            QualityResultEntity result = new QualityResultEntity();
            result.setDataId(dataId);
            result.setModelId(modelId);
            result.setRuleId(violation.getRuleId());
            result.setSeverity(violation.getSeverity());
            result.setMessage(violation.getMessage());
            boolean ignored = ignoredRuleIds != null && ignoredRuleIds.contains(violation.getRuleId())
                    && !"CRITICAL".equals(violation.getSeverity());
            result.setIgnored(ignored ? 1 : 0);
            result.setIgnoreReason(ignored ? ignoreReason : null);
            result.setCheckedTime(Times.now());
            resultRepository.save(result);
        }
    }

    /**
     * 忽略告警（仅 WARNING/INFO；CRITICAL 不可忽略）。
     */
    @Transactional
    public void ignoreResult(Long resultId, String reason) {
        QualityResultEntity result = resultRepository.findById(resultId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "质量结果不存在：" + resultId));
        if ("CRITICAL".equals(result.getSeverity())) {
            throw new BusinessException(ErrorCode.QUALITY_CRITICAL_BLOCKED, "严重级告警不可忽略，必须修正后提交");
        }
        result.setIgnored(1);
        result.setIgnoreReason(reason);
        resultRepository.save(result);
        logService.log("QUALITY", "IGNORE", resultId, "忽略告警：" + result.getMessage(),
                Map.of("dataId", result.getDataId(), "reason", reason == null ? "" : reason));
    }

    /**
     * 查询某数据的当前有效质量结果。
     */
    public List<QualityResultEntity> listResults(Long dataId) {
        return resultRepository.findByDataIdAndDelFlag(dataId, 0);
    }

    // ==================== 规则 CRUD ====================

    public QualityRuleEntity create(QualityRuleRequest request) {
        validate(request);
        QualityRuleEntity entity = new QualityRuleEntity();
        entity.setModelId(request.getModelId());
        entity.setRuleType(request.getRuleType().trim().toUpperCase(Locale.ROOT));
        entity.setFieldName(request.getFieldName());
        entity.setExpression(toJson(request.getExpression()));
        entity.setSeverity(request.getSeverity().trim().toUpperCase(Locale.ROOT));
        entity.setMessage(request.getMessage());
        entity.setEnabled(request.getEnabled() == null || request.getEnabled());
        entity.setCreatedBy(UserContext.get() == null ? "system" : UserContext.get().operator());
        entity.setCreatedTime(Times.now());
        QualityRuleEntity saved = ruleRepository.save(entity);
        logService.log("QUALITY", "CREATE", saved.getId(), "新建质量规则：" + saved.getMessage(),
                Map.of("ruleType", saved.getRuleType(), "severity", saved.getSeverity()));
        return saved;
    }

    public QualityRuleEntity update(Long id, QualityRuleRequest request) {
        validate(request);
        QualityRuleEntity entity = ruleRepository.findByIdAndDelFlag(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "质量规则不存在：" + id));
        entity.setModelId(request.getModelId());
        entity.setRuleType(request.getRuleType().trim().toUpperCase(Locale.ROOT));
        entity.setFieldName(request.getFieldName());
        entity.setExpression(toJson(request.getExpression()));
        entity.setSeverity(request.getSeverity().trim().toUpperCase(Locale.ROOT));
        entity.setMessage(request.getMessage());
        entity.setEnabled(request.getEnabled() == null || request.getEnabled());
        entity.setUpdatedBy(UserContext.get() == null ? "system" : UserContext.get().operator());
        entity.setUpdatedTime(Times.now());
        QualityRuleEntity saved = ruleRepository.save(entity);
        logService.log("QUALITY", "UPDATE", saved.getId(), "修改质量规则：" + saved.getMessage(),
                Map.of("ruleType", saved.getRuleType(), "severity", saved.getSeverity()));
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        QualityRuleEntity entity = ruleRepository.findByIdAndDelFlag(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "质量规则不存在：" + id));
        entity.setDelFlag(1);
        entity.setUpdatedTime(Times.now());
        ruleRepository.save(entity);
        logService.log("QUALITY", "DELETE", id, "删除质量规则：" + entity.getMessage(), null);
    }

    /** 模型规则列表（含未启用）。 */
    public List<QualityRuleEntity> listByModel(Long modelId) {
        List<QualityRuleEntity> rules = new ArrayList<>(ruleRepository.findByModelIdAndDelFlag(modelId, 0));
        rules.addAll(ruleRepository.findByModelIdIsNullAndDelFlag(0));
        return rules;
    }

    /** 全局规则列表（model_id 为空）。 */
    public List<QualityRuleEntity> listGlobal() {
        return new ArrayList<>(ruleRepository.findByModelIdIsNullAndDelFlag(0));
    }

    public PageResult<QualityRuleEntity> page(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, size), Sort.by(Sort.Direction.DESC, "id"));
        Page<QualityRuleEntity> result = ruleRepository.findByDelFlag(0, pageable);
        return new PageResult<>(result.getTotalElements(), page, size, result.getContent());
    }

    private void validate(QualityRuleRequest request) {
        if (request.getRuleType() == null || request.getRuleType().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "规则类型不能为空");
        }
        if (request.getSeverity() == null || request.getSeverity().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "告警等级不能为空");
        }
        if (!"CRITICAL".equalsIgnoreCase(request.getSeverity()) && !"WARNING".equalsIgnoreCase(request.getSeverity())
                && !"INFO".equalsIgnoreCase(request.getSeverity())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "告警等级仅支持 CRITICAL/WARNING/INFO");
        }
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "告警消息不能为空");
        }
        if (request.getExpression() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "规则表达式不能为空");
        }
    }

    private JsonNode parseExpression(String expression) {
        try {
            return objectMapper.readTree(expression == null || expression.isBlank() ? "{}" : expression);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "规则表达式 JSON 解析失败：" + ex.getMessage());
        }
    }

    private String toJson(Object expression) {
        try {
            return objectMapper.writeValueAsString(expression == null ? Map.of() : expression);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "规则表达式序列化失败");
        }
    }

    private static String textOf(Map<String, Object> attributes, String fieldName) {
        if (attributes == null || fieldName == null) {
            return null;
        }
        Object value = attributes.get(fieldName);
        return value == null ? null : String.valueOf(value);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
