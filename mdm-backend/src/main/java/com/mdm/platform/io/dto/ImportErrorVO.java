package com.mdm.platform.io.dto;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 导入错误行明细。
 */
public class ImportErrorVO {

    /** Excel 行号（1 基，含表头偏移） */
    private int row;

    /** 出错字段（空=行级错误） */
    private String field;

    private String message;

    /** 原始行数据（label → 单元格文本） */
    private Map<String, String> rowData = new LinkedHashMap<>();

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, String> getRowData() {
        return rowData;
    }

    public void setRowData(Map<String, String> rowData) {
        this.rowData = rowData;
    }
}
