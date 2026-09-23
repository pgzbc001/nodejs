package com.mdm.platform.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 字段定义：模型元数据核心（18 项业务属性，见需求 4.3.2）。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FieldDef {

    /** 字段名称（英文标识） */
    private String name;

    /** 显示名称（中文标签） */
    private String label;

    /** 数据类型：TEXT / LONG_TEXT / NUMBER / DATE */
    private String type;

    /** 是否必填 */
    private boolean required;

    /** 是否唯一 */
    private boolean unique;

    /** 是否在列表展示 */
    private boolean listShow;

    /** 是否下拉选项 */
    private boolean selectable;

    /** 值域来源：MANUAL（手动固定值）/ REF（引用其他模型） */
    private String domainSource;

    /** 手动维护的固定值列表 */
    private List<String> domainValues;

    /** 引用模型编码（domainSource=REF 时） */
    private String refModelCode;

    /** 引用过滤条件（如只显示已上线数据） */
    private String refFilter;

    /** 下拉是否多选 */
    private boolean multiSelect;

    /** 是否作为检索条件 */
    private boolean searchable;

    /** 数据量大时是否弹窗选择 */
    private boolean popup;

    /** 是否加密存储 */
    private boolean encrypted;

    /** 数据分级：TOP_SECRET/CONFIDENTIAL/SECRET/SENSITIVE/PUBLIC */
    private String securityLevel;

    /** 关联安全识别规则 */
    private String securityRule;

    /** 默认值 */
    private String defaultValue;

    /** 字段分组（信息分区） */
    private String group;

    /** 自定义校验：PHONE（手机号）/ ID_CARD（身份证）/ NUMBER_RANGE（数值范围） */
    private String customRule;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean isUnique() {
        return unique;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    public boolean isListShow() {
        return listShow;
    }

    public void setListShow(boolean listShow) {
        this.listShow = listShow;
    }

    public boolean isSelectable() {
        return selectable;
    }

    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
    }

    public String getDomainSource() {
        return domainSource;
    }

    public void setDomainSource(String domainSource) {
        this.domainSource = domainSource;
    }

    public List<String> getDomainValues() {
        return domainValues;
    }

    public void setDomainValues(List<String> domainValues) {
        this.domainValues = domainValues;
    }

    public String getRefModelCode() {
        return refModelCode;
    }

    public void setRefModelCode(String refModelCode) {
        this.refModelCode = refModelCode;
    }

    public String getRefFilter() {
        return refFilter;
    }

    public void setRefFilter(String refFilter) {
        this.refFilter = refFilter;
    }

    public boolean isMultiSelect() {
        return multiSelect;
    }

    public void setMultiSelect(boolean multiSelect) {
        this.multiSelect = multiSelect;
    }

    public boolean isSearchable() {
        return searchable;
    }

    public void setSearchable(boolean searchable) {
        this.searchable = searchable;
    }

    public boolean isPopup() {
        return popup;
    }

    public void setPopup(boolean popup) {
        this.popup = popup;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    public String getSecurityLevel() {
        return securityLevel;
    }

    public void setSecurityLevel(String securityLevel) {
        this.securityLevel = securityLevel;
    }

    public String getSecurityRule() {
        return securityRule;
    }

    public void setSecurityRule(String securityRule) {
        this.securityRule = securityRule;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getCustomRule() {
        return customRule;
    }

    public void setCustomRule(String customRule) {
        this.customRule = customRule;
    }
}
