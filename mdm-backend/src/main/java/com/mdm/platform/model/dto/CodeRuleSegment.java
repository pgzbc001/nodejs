package com.mdm.platform.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 编码规则段（见需求 4.3.4）。
 * 类型：FIXED 固定值 / FIELD_REF 对象引用 / SEQ 顺序值 / MODEL_REF 编码引用。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CodeRuleSegment {

    /** 段类型 */
    private String type;

    /** FIXED：固定文本；FIELD_REF：字段名；SEQ：无 */
    private String value;

    /** SEQ：顺序值位数 */
    private Integer length;

    /** SEQ：补位符 */
    private String padChar;

    /** SEQ：补位方向 LEFT / RIGHT */
    private String padSide;

    /** SEQ：起始值 */
    private Integer seqStart;

    /** SEQ：步长 */
    private Integer seqStep;

    /** MODEL_REF：被引用模型编码 */
    private String refModelCode;

    /** MODEL_REF：引用字段名（取其值作为编码） */
    private String refField;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public String getPadChar() {
        return padChar;
    }

    public void setPadChar(String padChar) {
        this.padChar = padChar;
    }

    public String getPadSide() {
        return padSide;
    }

    public void setPadSide(String padSide) {
        this.padSide = padSide;
    }

    public Integer getSeqStart() {
        return seqStart;
    }

    public void setSeqStart(Integer seqStart) {
        this.seqStart = seqStart;
    }

    public Integer getSeqStep() {
        return seqStep;
    }

    public void setSeqStep(Integer seqStep) {
        this.seqStep = seqStep;
    }

    public String getRefModelCode() {
        return refModelCode;
    }

    public void setRefModelCode(String refModelCode) {
        this.refModelCode = refModelCode;
    }

    public String getRefField() {
        return refField;
    }

    public void setRefField(String refField) {
        this.refField = refField;
    }
}
