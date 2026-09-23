package com.mdm.platform.coderule;

import com.mdm.platform.common.BusinessException;
import com.mdm.platform.model.dto.CodeRuleSegment;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 编码引擎单元测试（tasks.md T15）：四段式拼接、补位、唯一性重试上限、规则校验。
 */
class CodeRuleEngineTest {

    private final CodeRuleEngine engine = new CodeRuleEngine(null, new CodeRuleValidator(), null);

    private static CodeRuleSegment seg(String type, String value, Integer length,
                                       String padChar, String padSide, String refField, String refModelCode) {
        CodeRuleSegment segment = new CodeRuleSegment();
        segment.setType(type);
        segment.setValue(value);
        segment.setLength(length);
        segment.setPadChar(padChar);
        segment.setPadSide(padSide);
        segment.setRefField(refField);
        segment.setRefModelCode(refModelCode);
        return segment;
    }

    @Test
    void padShouldLeftFillByDefault() {
        CodeRuleSegment seq = new CodeRuleSegment();
        seq.setLength(6);
        assertEquals("000123", CodeRuleEngine.pad(123, seq));
    }

    @Test
    void padShouldRightFillWhenConfigured() {
        CodeRuleSegment seq = new CodeRuleSegment();
        seq.setLength(5);
        seq.setPadSide("RIGHT");
        seq.setPadChar("*");
        assertEquals("123**", CodeRuleEngine.pad(123, seq));
    }

    @Test
    void padShouldReturnAsIsWhenLonger() {
        CodeRuleSegment seq = new CodeRuleSegment();
        seq.setLength(3);
        assertEquals("12345", CodeRuleEngine.pad(12345, seq));
    }

    @Test
    void concatShouldJoinFixedFieldRefAndSeq() {
        List<CodeRuleSegment> rules = List.of(
                seg("FIXED", "MAT", null, null, null, null, null),
                seg("FIELD_REF", "materialCategory", null, null, null, null, null),
                seg("SEQ", null, 6, "0", "LEFT", null, null));
        String code = engine.concat(1L, rules, Map.of("materialCategory", "CAT0101"));
        assertTrue(code.startsWith("MATCAT0101"));
        assertEquals(16, code.length());
    }

    @Test
    void concatShouldSupportModelRefAsFirstSegment() {
        List<CodeRuleSegment> rules = List.of(
                seg("MODEL_REF", null, null, null, null, "sourceCode", "MAT_RAW"),
                seg("FIXED", "-EXT", null, null, null, null, null));
        String code = engine.concat(1L, rules, Map.of("sourceCode", "MATCAT0101000001"));
        assertEquals("MATCAT0101000001-EXT", code);
    }

    @Test
    void concatShouldFailWhenFieldRefValueMissing() {
        List<CodeRuleSegment> rules = List.of(
                seg("FIELD_REF", "notProvided", null, null, null, null, null));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> engine.concat(1L, rules, Map.of()));
        assertEquals(40910, ex.getErrorCode().getCode());
    }

    @Test
    void validatorShouldRejectModelRefNotInFirstSegment() {
        List<CodeRuleSegment> rules = List.of(
                seg("FIXED", "A", null, null, null, null, null),
                seg("MODEL_REF", null, null, null, null, "code", "MAT_RAW"));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> new CodeRuleValidator().validate(rules));
        assertEquals(40910, ex.getErrorCode().getCode());
    }

    @Test
    void validatorShouldRejectDuplicatedSeqSegment() {
        List<CodeRuleSegment> rules = List.of(
                seg("SEQ", null, 4, "0", "LEFT", null, null),
                seg("SEQ", null, 4, "0", "LEFT", null, null));
        assertThrows(BusinessException.class, () -> new CodeRuleValidator().validate(rules));
    }

    @Test
    void validatorShouldRejectSeqWithoutLength() {
        List<CodeRuleSegment> rules = List.of(seg("SEQ", null, null, "0", "LEFT", null, null));
        assertThrows(BusinessException.class, () -> new CodeRuleValidator().validate(rules));
    }

    @Test
    void parseRulesShouldReturnNullForBlank() {
        assertEquals(null, engine.parseRules(null));
        assertEquals(null, engine.parseRules("  "));
    }
}
