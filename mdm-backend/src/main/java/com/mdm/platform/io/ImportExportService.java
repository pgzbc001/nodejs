package com.mdm.platform.io;

import com.mdm.platform.audit.OperationLogService;
import com.mdm.platform.common.BusinessException;
import com.mdm.platform.common.ErrorCode;
import com.mdm.platform.common.PageResult;
import com.mdm.platform.data.MasterDataService;
import com.mdm.platform.data.dto.DataQueryRequest;
import com.mdm.platform.data.dto.DataUpsertRequest;
import com.mdm.platform.data.dto.DataViewVO;
import com.mdm.platform.io.dto.ImportErrorVO;
import com.mdm.platform.io.dto.ImportResultVO;
import com.mdm.platform.model.ModelEntity;
import com.mdm.platform.model.ModelRepository;
import com.mdm.platform.model.dto.FieldDef;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 导入导出服务（REQ-BKD09）：模板下载 / 筛选导出（脱敏一致）/ 批量导入 + 错误明细。
 */
@Service
public class ImportExportService {

    private static final String CODE_HEADER = "编码（留空自动生成）";

    private final ModelRepository modelRepository;
    private final MasterDataService masterDataService;
    private final OperationLogService logService;

    public ImportExportService(ModelRepository modelRepository,
                               MasterDataService masterDataService,
                               OperationLogService logService) {
        this.modelRepository = modelRepository;
        this.masterDataService = masterDataService;
        this.logService = logService;
    }

    /**
     * 模板：字段 label 表头（必填加 *）+ 填写说明 sheet。
     */
    public byte[] template(Long modelId) throws IOException {
        ModelEntity model = requireOnlineModel(modelId);
        List<FieldDef> defs = masterDataService.fieldDefs(model);
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(model.getName());
            Row header = sheet.createRow(0);
            int col = 0;
            header.createCell(col++).setCellValue(CODE_HEADER);
            for (FieldDef def : defs) {
                header.createCell(col++).setCellValue(def.getLabel() + (def.isRequired() ? "*" : ""));
            }
            Sheet guide = workbook.createSheet("填写说明");
            guide.createRow(0).createCell(0).setCellValue("1. 编码列留空时按模型编码规则自动生成");
            guide.createRow(1).createCell(0).setCellValue("2. 带 * 列为必填；日期格式 yyyy-MM-dd；数字列不要带单位");
            StringBuilder domain = new StringBuilder("3. 下拉字段值域：");
            for (FieldDef def : defs) {
                if (def.isSelectable() && def.getDomainValues() != null && !def.getDomainValues().isEmpty()) {
                    domain.append(def.getLabel()).append("（").append(String.join("/", def.getDomainValues()))
                            .append("）");
                }
            }
            guide.createRow(2).createCell(0).setCellValue(domain.toString());
            return toBytes(workbook);
        }
    }

    /**
     * 导出：按当前筛选条件，脱敏规则与列表一致（加密字段掩码）。
     */
    public byte[] export(Long modelId, DataQueryRequest request) throws IOException {
        ModelEntity model = requireOnlineModel(modelId);
        List<FieldDef> defs = masterDataService.fieldDefs(model);
        DataQueryRequest query = request == null ? new DataQueryRequest() : request;
        query.setModelId(modelId);
        query.setPage(1);
        query.setSize(Integer.MAX_VALUE);
        PageResult<DataViewVO> page = masterDataService.page(query);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(model.getName());
            Row header = sheet.createRow(0);
            int col = 0;
            header.createCell(col++).setCellValue("编码");
            header.createCell(col++).setCellValue("名称");
            header.createCell(col++).setCellValue("状态");
            for (FieldDef def : defs) {
                header.createCell(col++).setCellValue(def.getLabel());
            }
            int rowIdx = 1;
            for (DataViewVO vo : page.getList()) {
                Row row = sheet.createRow(rowIdx++);
                int c = 0;
                row.createCell(c++).setCellValue(vo.getCode());
                row.createCell(c++).setCellValue(vo.getName());
                row.createCell(c++).setCellValue(vo.getStatus());
                Map<String, Object> attrs = vo.getAttributes() == null ? Map.of() : vo.getAttributes();
                for (FieldDef def : defs) {
                    Object value = attrs.get(def.getName());
                    row.createCell(c++).setCellValue(value == null ? "" : String.valueOf(value));
                }
            }
            logService.log("IMPORT_EXPORT", "EXPORT", modelId, model.getName(),
                    Map.of("count", page.getList().size()));
            return toBytes(workbook);
        }
    }

    /**
     * 导入：逐行执行必填/格式/唯一/质量校验，通过行提交，失败行记录明细。
     */
    public ImportResultVO importExcel(Long modelId, byte[] content) throws IOException {
        ModelEntity model = requireOnlineModel(modelId);
        List<FieldDef> defs = masterDataService.fieldDefs(model);
        ImportResultVO result = new ImportResultVO();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            if (header == null) {
                throw new BusinessException(ErrorCode.IMPORT_FILE_INVALID, "导入文件无表头行");
            }
            Map<Integer, String> labelToName = new LinkedHashMap<>();
            Map<Integer, Boolean> codeCol = new LinkedHashMap<>();
            for (Cell cell : header) {
                if (cell.getCellType() == CellType.STRING) {
                    String text = cell.getStringCellValue().trim();
                    if (text.endsWith("*")) {
                        text = text.substring(0, text.length() - 1);
                    }
                    if (CODE_HEADER.startsWith("编码") && text.startsWith("编码")) {
                        codeCol.put(cell.getColumnIndex(), true);
                    } else {
                        for (FieldDef def : defs) {
                            if (def.getLabel().equals(text)) {
                                labelToName.put(cell.getColumnIndex(), def.getName());
                                break;
                            }
                        }
                    }
                }
            }
            if (labelToName.isEmpty() && codeCol.isEmpty()) {
                throw new BusinessException(ErrorCode.IMPORT_FILE_INVALID,
                        "表头与模型字段不匹配，请下载最新模板");
            }

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isBlankRow(row)) {
                    continue;
                }
                result.setTotal(result.getTotal() + 1);
                Map<String, Object> attributes = new LinkedHashMap<>();
                Map<String, String> rowData = new LinkedHashMap<>();
                String code = null;
                for (Map.Entry<Integer, String> entry : labelToName.entrySet()) {
                    String value = cellText(row, entry.getKey());
                    rowData.put(labelOf(defs, entry.getValue()), value);
                    if (value != null && !value.isBlank()) {
                        attributes.put(entry.getValue(), value);
                    }
                }
                for (Map.Entry<Integer, Boolean> entry : codeCol.entrySet()) {
                    code = cellText(row, entry.getKey());
                    rowData.put(CODE_HEADER, code);
                }
                try {
                    DataUpsertRequest request = new DataUpsertRequest();
                    request.setAttributes(attributes);
                    request.setCode(code == null || code.isBlank() ? null : code);
                    masterDataService.create(modelId, request);
                    result.setSuccessCount(result.getSuccessCount() + 1);
                } catch (BusinessException ex) {
                    result.setFailCount(result.getFailCount() + 1);
                    ImportErrorVO error = new ImportErrorVO();
                    error.setRow(r + 1);
                    error.setMessage(ex.getMessage());
                    error.setRowData(rowData);
                    result.getErrors().add(error);
                }
            }
        }
        logService.log("IMPORT_EXPORT", "IMPORT", modelId, model.getName(),
                Map.of("total", result.getTotal(), "success", result.getSuccessCount(),
                        "fail", result.getFailCount()));
        return result;
    }

    /**
     * 错误明细 Excel：原始数据列 + 行号 + 错误说明。
     */
    public byte[] exportErrors(List<ImportErrorVO> errors) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("导入错误明细");
            Map<String, Integer> headers = new LinkedHashMap<>();
            headers.put("行号", 0);
            int col = 1;
            for (ImportErrorVO error : errors) {
                for (String label : error.getRowData().keySet()) {
                    if (!headers.containsKey(label)) {
                        headers.put(label, col++);
                    }
                }
            }
            headers.put("错误说明", col);
            Row header = sheet.createRow(0);
            for (Map.Entry<String, Integer> entry : headers.entrySet()) {
                header.createCell(entry.getValue()).setCellValue(entry.getKey());
            }
            int rowIdx = 1;
            for (ImportErrorVO error : errors) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(error.getRow());
                for (Map.Entry<String, String> data : error.getRowData().entrySet()) {
                    Integer index = headers.get(data.getKey());
                    if (index != null) {
                        row.createCell(index).setCellValue(data.getValue() == null ? "" : data.getValue());
                    }
                }
                Integer msgIndex = headers.get("错误说明");
                row.createCell(msgIndex).setCellValue(error.getMessage());
            }
            return toBytes(workbook);
        }
    }

    // ==================== 内部工具 ====================

    private ModelEntity requireOnlineModel(Long modelId) {
        ModelEntity model = modelRepository.findByIdAndDelFlag(modelId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "模型不存在：" + modelId));
        if (!model.isOnline()) {
            throw new BusinessException(ErrorCode.MODEL_NOT_ONLINE, "模型未上线，不允许导入导出数据");
        }
        return model;
    }

    private static String labelOf(List<FieldDef> defs, String name) {
        for (FieldDef def : defs) {
            if (def.getName().equals(name)) {
                return def.getLabel();
            }
        }
        return name;
    }

    private static String cellText(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double num = cell.getNumericCellValue();
                if (num == Math.floor(num) && !Double.isInfinite(num)) {
                    yield String.valueOf((long) num);
                }
                yield String.valueOf(num);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private static boolean isBlankRow(Row row) {
        for (Cell cell : row) {
            String text = cellText(row, cell.getColumnIndex());
            if (text != null && !text.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private static byte[] toBytes(Workbook workbook) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
