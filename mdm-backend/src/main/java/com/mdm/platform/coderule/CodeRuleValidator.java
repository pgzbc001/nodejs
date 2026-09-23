package com.mdm.platform.coderule;

import com.mdm.platform.common.BusinessException;
import com.mdm.platform.common.ErrorCode;
import com.mdm.platform.model.dto.CodeRuleSegment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 编码规则合法性校验（40910）：
 * MODEL_REF 仅允许出现在第一段；SEQ 每模型最多一段；段参数完整性。
 */
@Component
public class CodeRuleValidator {

    public void validate(List<CodeRuleSegment> rules) {
        if (rules == null || rules.isEmpty()) {
            throw new BusinessException(ErrorCode.CODE_RULE_INVALID, "编码规则不能为空");
        }
        int seqCount = 0;
        for (int i = 0; i < rules.size(); i++) {
            CodeRuleSegment seg = rules.get(i);
            String type = seg.getType() == null ? "" : seg.getType().trim().toUpperCase();
            switch (type) {
                case "FIXED" -> {
                    if (seg.getValue() == null || seg.getValue().isBlank()) {
                        throw new BusinessException(ErrorCode.CODE_RULE_INVALID, "第" + (i + 1) + "段（固定值）未配置文本");
                    }
                }
                case "FIELD_REF" -> {
                    if (seg.getValue() == null || seg.getValue().isBlank()) {
                        throw new BusinessException(ErrorCode.CODE_RULE_INVALID,
                                "第" + (i + 1) + "段（对象引用）未配置引用字段");
                    }
                }
                case "SEQ" -> {
                    seqCount++;
                    if (seqCount > 1) {
                        throw new BusinessException(ErrorCode.CODE_RULE_INVALID, "顺序值段最多只能出现一次");
                    }
                    if (seg.getLength() == null || seg.getLength() <= 0) {
                        throw new BusinessException(ErrorCode.CODE_RULE_INVALID, "第" + (i + 1) + "段（顺序值）未配置位数");
                    }
                    if (seg.getPadSide() != null && !"LEFT".equalsIgnoreCase(seg.getPadSide())
                            && !"RIGHT".equalsIgnoreCase(seg.getPadSide())) {
                        throw new BusinessException(ErrorCode.CODE_RULE_INVALID,
                                "第" + (i + 1) + "段（顺序值）补位方向仅支持 LEFT/RIGHT");
                    }
                }
                case "MODEL_REF" -> {
                    if (i != 0) {
                        throw new BusinessException(ErrorCode.CODE_RULE_INVALID);
                    }
                    if (seg.getRefField() == null || seg.getRefField().isBlank()) {
                        throw new BusinessException(ErrorCode.CODE_RULE_INVALID,
                                "第" + (i + 1) + "段（编码引用）未配置引用字段");
                    }
                    if (seg.getRefModelCode() == null || seg.getRefModelCode().isBlank()) {
                        throw new BusinessException(ErrorCode.CODE_RULE_INVALID,
                                "第" + (i + 1) + "段（编码引用）未配置被引用模型");
                    }
                }
                default -> throw new BusinessException(ErrorCode.CODE_RULE_INVALID,
                        "第" + (i + 1) + "段类型非法：" + seg.getType());
            }
        }
    }
}
