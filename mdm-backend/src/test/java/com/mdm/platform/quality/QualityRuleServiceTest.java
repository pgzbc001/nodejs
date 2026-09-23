package com.mdm.platform.quality;

import com.mdm.platform.audit.OperationLogService;
import com.mdm.platform.model.ModelEntity;
import com.mdm.platform.model.dto.FieldDef;
import com.mdm.platform.quality.dto.QualityViolationVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * 质量规则引擎单元测试（tasks.md T17）：三类规则求值、三级告警、CRITICAL 判定。
 */
@ExtendWith(MockitoExtension.class)
class QualityRuleServiceTest {

    @Mock
    private QualityRuleRepository ruleRepository;
    @Mock
    private QualityResultRepository resultRepository;
    @Mock
    private OperationLogService logService;

    private QualityRuleService service;
    private ModelEntity model;

    @BeforeEach
    void setUp() {
        service = new QualityRuleService(ruleRepository, resultRepository, logService);
        model = new ModelEntity();
        model.setId(1L);
        model.setFieldDefs("[{\"name\":\"materialName\",\"label\":\"物料名称\",\"type\":\"TEXT\"},"
                + "{\"name\":\"unit\",\"label\":\"计量单位\",\"type\":\"TEXT\",\"selectable\":true,"
                + "\"domainValues\":[\"吨\",\"千克\",\"米\",\"张\"]}]");
    }

    private static QualityRuleEntity rule(long id, String type, String field, String expression, String severity) {
        QualityRuleEntity entity = new QualityRuleEntity();
        entity.setId(id);
        entity.setModelId(1L);
        entity.setRuleType(type);
        entity.setFieldName(field);
        entity.setExpression(expression);
        entity.setSeverity(severity);
        entity.setMessage("规则" + id + "违规");
        return entity;
    }

    @Test
    void completenessShouldFailOnBlankValue() {
        QualityRuleEntity rule = rule(1, "COMPLETENESS", "materialName", "{}", "CRITICAL");
        assertFalse(service.evaluateRule(rule, model, Map.of("materialName", "")));
        assertFalse(service.evaluateRule(rule, model, Map.of()));
        assertTrue(service.evaluateRule(rule, model, Map.of("materialName", "不锈钢板304")));
    }

    @Test
    void complianceLengthMaxShouldCheckLength() {
        QualityRuleEntity rule = rule(2, "COMPLIANCE", "spec", "{\"op\":\"LENGTH_MAX\",\"value\":5}", "WARNING");
        assertTrue(service.evaluateRule(rule, model, Map.of("spec", "2.5mm")));
        assertFalse(service.evaluateRule(rule, model, Map.of("spec", "2.5*1250*2500")));
    }

    @Test
    void complianceRegexShouldMatchPattern() {
        QualityRuleEntity rule = rule(3, "COMPLIANCE", "phone",
                "{\"op\":\"REGEX\",\"value\":\"^1[3-9]\\\\d{9}$\"}", "INFO");
        assertTrue(service.evaluateRule(rule, model, Map.of("phone", "13800138000")));
        assertFalse(service.evaluateRule(rule, model, Map.of("phone", "12345")));
    }

    @Test
    void complianceNumRangeShouldCheckBounds() {
        QualityRuleEntity rule = rule(4, "COMPLIANCE", "price",
                "{\"op\":\"NUM_RANGE\",\"value\":[0,10000]}", "CRITICAL");
        assertTrue(service.evaluateRule(rule, model, Map.of("price", "5000")));
        assertFalse(service.evaluateRule(rule, model, Map.of("price", "20000")));
        assertFalse(service.evaluateRule(rule, model, Map.of("price", "abc")));
    }

    @Test
    void consistencyInDomainShouldCheckDomainValues() {
        QualityRuleEntity rule = rule(5, "CONSISTENCY", "unit",
                "{\"refField\":\"unit\",\"inDomain\":true}", "INFO");
        assertTrue(service.evaluateRule(rule, model, Map.of("unit", "吨")));
        assertFalse(service.evaluateRule(rule, model, Map.of("unit", "光年")));
        // 空值跳过（完整性由 COMPLETENESS 负责）
        assertTrue(service.evaluateRule(rule, model, Map.of()));
    }

    @Test
    void consistencyFieldCompareShouldCheckRelation() {
        QualityRuleEntity rule = rule(6, "CONSISTENCY", "price",
                "{\"fieldA\":\"price\",\"op\":\"LTE\",\"fieldB\":\"taxPrice\"}", "WARNING");
        assertTrue(service.evaluateRule(rule, model, Map.of("price", "100", "taxPrice", "113")));
        assertFalse(service.evaluateRule(rule, model, Map.of("price", "120", "taxPrice", "113")));
    }

    @Test
    void evaluateShouldCollectViolationsFromModelRules() {
        when(ruleRepository.findByModelIdAndEnabledAndDelFlag(1L, 1, 0))
                .thenReturn(List.of(
                        rule(1, "COMPLETENESS", "materialName", "{}", "CRITICAL"),
                        rule(2, "COMPLIANCE", "spec", "{\"op\":\"LENGTH_MAX\",\"value\":5}", "WARNING")));
        when(ruleRepository.findByModelIdIsNullAndEnabledAndDelFlag(1, 0)).thenReturn(List.of());

        List<QualityViolationVO> violations = service.evaluate(model, Map.of("materialName", ""));
        assertEquals(1, violations.size());
        assertEquals("CRITICAL", violations.get(0).getSeverity());
        assertTrue(service.hasCritical(violations));

        List<QualityViolationVO> clean = service.evaluate(model,
                Map.of("materialName", "钢板", "spec", "2.5mm"));
        assertFalse(service.hasCritical(clean));
    }

    @Test
    void hasCriticalShouldBeFalseForWarningOnly() {
        List<QualityViolationVO> violations = List.of(
                new QualityViolationVO(1L, "spec", "WARNING", "长度超限"));
        assertFalse(service.hasCritical(violations));
    }
}
