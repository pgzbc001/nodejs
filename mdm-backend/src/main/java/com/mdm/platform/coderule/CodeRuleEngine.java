package com.mdm.platform.coderule;

import com.mdm.platform.common.BusinessException;
import com.mdm.platform.common.ErrorCode;
import com.mdm.platform.common.Times;
import com.mdm.platform.data.MasterDataRepository;
import com.mdm.platform.model.ModelEntity;
import com.mdm.platform.model.dto.CodeRuleSegment;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 编码生成引擎（REQ-BKD04）：
 * FIXED 常量 + FIELD_REF 对象引用 + SEQ 顺序值 + MODEL_REF 编码引用 顺序拼接；
 * 顺序值按模型独立计数、按补位符与方向补足位数；生成后做唯一性校验并重试。
 */
@Component
public class CodeRuleEngine {

    /** 唯一性冲突重试上限 */
    static final int MAX_RETRY = 10;

    private final CodeSequenceRepository sequenceRepository;
    private final CodeRuleValidator validator;
    private final MasterDataRepository dataRepository;
    private final ObjectMapper objectMapper;

    public CodeRuleEngine(CodeSequenceRepository sequenceRepository,
                          CodeRuleValidator validator,
                          MasterDataRepository dataRepository) {
        this.sequenceRepository = sequenceRepository;
        this.validator = validator;
        this.dataRepository = dataRepository;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 按模型编码规则生成全局唯一编码。
     *
     * @param model      数据模型（含 code_rules JSON）
     * @param attributes 本条数据动态属性（FIELD_REF/MODEL_REF 取值来源）
     * @return 唯一编码，如 MATCAT0101000007
     */
    public String generate(ModelEntity model, Map<String, Object> attributes) {
        List<CodeRuleSegment> rules = parseRules(model.getCodeRules());
        if (rules == null || rules.isEmpty()) {
            throw new BusinessException(ErrorCode.CODE_RULE_INVALID, "模型未配置编码规则，无法自动生成编码");
        }
        validator.validate(rules);
        for (int attempt = 0; attempt < MAX_RETRY; attempt++) {
            String code = concat(model.getId(), rules, attributes);
            if (!dataRepository.existsByModelIdAndCodeAndDelFlag(model.getId(), code, 0)) {
                return code;
            }
            // 编码冲突：SEQ 段在下一轮 concat 中再次取号
        }
        throw new BusinessException(ErrorCode.CODE_GENERATE_FAILED,
                "编码生成失败：" + MAX_RETRY + " 次重试后仍与已有数据冲突");
    }

    /** 仅供单测：拼接段（不查库，不重试）。 */
    String concat(Long modelId, List<CodeRuleSegment> rules, Map<String, Object> attributes) {
        StringBuilder sb = new StringBuilder();
        for (CodeRuleSegment seg : rules) {
            sb.append(evaluate(modelId, seg, attributes));
        }
        return sb.toString();
    }

    private String evaluate(Long modelId, CodeRuleSegment seg, Map<String, Object> attributes) {
        String type = seg.getType() == null ? "" : seg.getType().trim().toUpperCase();
        switch (type) {
            case "FIXED" -> {
                if (seg.getValue() == null || seg.getValue().isEmpty()) {
                    throw new BusinessException(ErrorCode.CODE_RULE_INVALID, "固定值段未配置文本");
                }
                return seg.getValue();
            }
            case "FIELD_REF" -> {
                Object val = attributes == null ? null : attributes.get(seg.getValue());
                if (val == null || String.valueOf(val).isBlank()) {
                    throw new BusinessException(ErrorCode.CODE_RULE_INVALID,
                            "对象引用段引用的字段未提供值：" + seg.getValue());
                }
                return String.valueOf(val);
            }
            case "SEQ" -> {
                int step = seg.getSeqStep() == null || seg.getSeqStep() <= 0 ? 1 : seg.getSeqStep();
                int start = seg.getSeqStart() == null || seg.getSeqStart() < 0 ? 1 : seg.getSeqStart();
                int value = nextVal(modelId, step, start);
                return pad(value, seg);
            }
            case "MODEL_REF" -> {
                Object val = attributes == null ? null : attributes.get(seg.getRefField());
                if (val == null || String.valueOf(val).isBlank()) {
                    throw new BusinessException(ErrorCode.CODE_RULE_INVALID,
                            "编码引用段未提供被引用模型数据编码：" + seg.getRefField());
                }
                return String.valueOf(val);
            }
            default -> throw new BusinessException(ErrorCode.CODE_RULE_INVALID, "未知编码规则段类型：" + seg.getType());
        }
    }

    /** 按模型独立计数的顺序值取号（进程内串行，SQLite 单写者场景足够）。 */
    private synchronized int nextVal(Long modelId, int step, int start) {
        CodeSequenceEntity seq = sequenceRepository.findByModelId(modelId)
                .orElseGet(() -> {
                    CodeSequenceEntity entity = new CodeSequenceEntity();
                    entity.setModelId(modelId);
                    entity.setCurrentValue(start - step);
                    return sequenceRepository.save(entity);
                });
        seq.setCurrentValue(seq.getCurrentValue() + step);
        seq.setUpdatedTime(Times.now());
        sequenceRepository.save(seq);
        return seq.getCurrentValue();
    }

    /** 按补位符与方向补足位数（超长原样返回）。 */
    static String pad(int value, CodeRuleSegment seg) {
        String s = String.valueOf(value);
        int len = seg.getLength() == null ? 0 : seg.getLength();
        if (s.length() >= len || len <= 0) {
            return s;
        }
        char padChar = (seg.getPadChar() == null || seg.getPadChar().isEmpty()) ? '0' : seg.getPadChar().charAt(0);
        int fill = len - s.length();
        String fills = String.valueOf(padChar).repeat(fill);
        return "RIGHT".equalsIgnoreCase(seg.getPadSide()) ? s + fills : fills + s;
    }

    /** 解析 code_rules JSON（null/空白返回 null）。 */
    public List<CodeRuleSegment> parseRules(String codeRulesJson) {
        if (codeRulesJson == null || codeRulesJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(codeRulesJson, new TypeReference<List<CodeRuleSegment>>() {
            });
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.CODE_RULE_INVALID, "编码规则 JSON 解析失败：" + ex.getMessage());
        }
    }
}
