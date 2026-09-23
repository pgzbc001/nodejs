package com.mdm.platform.io;

import com.mdm.platform.common.ApiResponse;
import com.mdm.platform.io.dto.ImportErrorVO;
import com.mdm.platform.io.dto.ImportResultVO;
import com.mdm.platform.security.NoRepeatSubmit;
import com.mdm.platform.security.RequirePermission;
import com.mdm.platform.security.Role;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 导入导出接口（契约 3.5：/data/{modelId}/template|export|import）。
 */
@RestController
@RequestMapping("/api/v1/data")
public class ImportExportController {

    private final ImportExportService importExportService;

    public ImportExportController(ImportExportService importExportService) {
        this.importExportService = importExportService;
    }

    /** 下载导入模板 xlsx（字段 label 表头 + 填写说明）。 */
    @GetMapping("/{modelId}/template")
    @RequirePermission({Role.DATA_STAFF, Role.MODEL_ADMIN, Role.SYS_ADMIN})
    public ResponseEntity<byte[]> template(@PathVariable Long modelId) throws Exception {
        byte[] content = importExportService.template(modelId);
        return file(content, "import-template-" + modelId + ".xlsx");
    }

    /** 按当前筛选条件导出 xlsx（脱敏规则与列表一致）。 */
    @GetMapping("/{modelId}/export")
    @RequirePermission({Role.DATA_STAFF, Role.MODEL_ADMIN, Role.SYS_ADMIN})
    public ResponseEntity<byte[]> export(@PathVariable Long modelId,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) String status,
                                         @RequestParam(required = false) String filters) throws Exception {
        com.mdm.platform.data.dto.DataQueryRequest request = new com.mdm.platform.data.dto.DataQueryRequest();
        request.setModelId(modelId);
        request.setKeyword(keyword);
        request.setStatus(status);
        request.setFilters(parseFilters(filters));
        byte[] content = importExportService.export(modelId, request);
        return file(content, "export-" + modelId + ".xlsx");
    }

    /** 上传导入（multipart file）：逐行校验，失败行返回明细。 */
    @PostMapping("/{modelId}/import")
    @NoRepeatSubmit
    @RequirePermission({Role.DATA_STAFF, Role.MODEL_ADMIN, Role.SYS_ADMIN})
    public ApiResponse<ImportResultVO> importExcel(@PathVariable Long modelId,
                                                   @RequestParam("file") MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            return ApiResponse.error(com.mdm.platform.common.ErrorCode.IMPORT_FILE_INVALID, "请选择要导入的 Excel 文件");
        }
        return ApiResponse.ok(importExportService.importExcel(modelId, file.getBytes()));
    }

    /** 导出错误明细 xlsx（回传失败行 JSON）。 */
    @PostMapping("/{modelId}/import/errors")
    public ResponseEntity<byte[]> exportErrors(@PathVariable Long modelId,
                                               @RequestBody List<ImportErrorVO> errors) throws Exception {
        byte[] content = importExportService.exportErrors(errors);
        return file(content, "import-errors-" + modelId + ".xlsx");
    }

    private static ResponseEntity<byte[]> file(byte[] content, String filename) {
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    private static java.util.Map<String, String> parseFilters(String filters) {
        if (filters == null || filters.isBlank()) {
            return null;
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(filters,
                    new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, String>>() {
                    });
        } catch (Exception ex) {
            return null;
        }
    }
}
