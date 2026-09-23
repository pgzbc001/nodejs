package com.mdm.platform.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdm.platform.audit.OperationLogService;
import com.mdm.platform.common.BusinessException;
import com.mdm.platform.common.ErrorCode;
import com.mdm.platform.common.Times;
import com.mdm.platform.data.dto.DataDiffVO;
import com.mdm.platform.data.dto.DataVersionVO;
import com.mdm.platform.model.ModelEntity;
import com.mdm.platform.model.ModelRepository;
import com.mdm.platform.model.dto.FieldDef;
import com.mdm.platform.security.CryptoService;
import com.mdm.platform.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 主数据版本服务（REQ-BKD06）：变更快照、版本列表、逐字段差异、回滚。
 */
@Service
public class VersionService {

    private final DataVersionRepository versionRepository;
    private final MasterDataRepository dataRepository;
    private final ModelRepository modelRepository;
    private final CryptoService cryptoService;
    private final OperationLogService logService;
    private final ObjectMapper objectMapper;

    public VersionService(DataVersionRepository versionRepository,
                          MasterDataRepository dataRepository,
                          ModelRepository modelRepository,
                          CryptoService cryptoService,
                          OperationLogService logService) {
        this.versionRepository = versionRepository;
        this.dataRepository = dataRepository;
        this.modelRepository = modelRepository;
        this.cryptoService = cryptoService;
        this.logService = logService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 落数据版本快照（attributes 为含密文 JSON）。
     */
    public void snapshot(MasterDataEntity data, String operation, String remark) {
        DataVersionEntity version = new DataVersionEntity();
        version.setDataId(data.getId());
        version.setModelId(data.getModelId());
        version.setVersionNo(data.getVersionNo());
        version.setCode(data.getCode());
        version.setSnapshot(data.getAttributes());
        version.setOperation(operation);
        version.setOperator(operator());
        version.setOperatedTime(Times.now());
        version.setRemark(remark);
        versionRepository.save(version);
    }

    /**
     * 版本列表（新版本在前）。
     */
    public List<DataVersionVO> versions(Long dataId) {
        requireData(dataId);
        return versionRepository.findByDataIdAndDelFlagOrderByVersionNoDesc(dataId, 0).stream()
                .map(this::toVO)
                .toList();
    }

    /**
     * 版本差异：逐字段新旧值（解密后明文展示，供前端高亮）。
     */
    public DataDiffVO diff(Long dataId, Integer fromVersion, Integer toVersion) {
        MasterDataEntity data = requireData(dataId);
        DataVersionEntity from = requireVersion(dataId, fromVersion);
        DataVersionEntity to = requireVersion(dataId, toVersion);
        Map<String, Object> fromAttrs = decryptAll(parse(from.getSnapshot()));
        Map<String, Object> toAttrs = decryptAll(parse(to.getSnapshot()));

        DataDiffVO vo = new DataDiffVO();
        vo.setFromVersion(fromVersion);
        vo.setToVersion(toVersion);
        List<DataDiffVO.FieldChange> changes = new ArrayList<>();
        Map<String, String> labels = fieldLabels(data.getModelId());
        if (!Objects.equals(from.getCode(), to.getCode())) {
            changes.add(new DataDiffVO.FieldChange("code", "编码", from.getCode(), to.getCode()));
        }
        Map<String, Object> union = new LinkedHashMap<>(fromAttrs);
        union.putAll(toAttrs);
        for (String field : union.keySet()) {
            String oldVal = textOf(fromAttrs.get(field));
            String newVal = textOf(toAttrs.get(field));
            if (!Objects.equals(oldVal, newVal)) {
                changes.add(new DataDiffVO.FieldChange(field, labels.getOrDefault(field, field), oldVal, newVal));
            }
        }
        vo.setChanges(changes);
        return vo;
    }

    /**
     * 回滚到指定版本：覆写属性与编码，版本号 +1，落 ROLLBACK 快照。
     */
    @Transactional
    public MasterDataEntity rollback(Long dataId, Integer versionNo) {
        MasterDataEntity data = requireData(dataId);
        if (data.getVersionNo().equals(versionNo)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前版本即 v" + versionNo + "，无需回滚");
        }
        DataVersionEntity target = requireVersion(dataId, versionNo);
        data.setCode(target.getCode());
        data.setAttributes(target.getSnapshot());
        data.setVersionNo(data.getVersionNo() + 1);
        data.setUpdatedBy(operator());
        data.setUpdatedTime(Times.now());
        MasterDataEntity saved = dataRepository.save(data);
        snapshot(saved, "ROLLBACK", "回滚到 v" + versionNo);
        logService.log("DATA", "ROLLBACK", dataId, data.getCode(),
                Map.of("rollbackTo", versionNo, "newVersion", saved.getVersionNo()));
        return saved;
    }

    /**
     * 版本详情（脱敏）：快照解析后加密字段掩码展示。
     */
    public Map<String, Object> maskedVersionDetail(Long dataId, Integer versionNo) {
        DataVersionEntity version = requireVersion(dataId, versionNo);
        Map<String, Object> attrs = parse(version.getSnapshot());
        Map<String, Object> masked = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : attrs.entrySet()) {
            if (entry.getValue() instanceof String text && text.startsWith(CryptoService.CIPHER_PREFIX)) {
                try {
                    masked.put(entry.getKey(), CryptoService.mask(cryptoService.decrypt(text)));
                } catch (Exception ignored) {
                    masked.put(entry.getKey(), "****");
                }
            } else {
                masked.put(entry.getKey(), entry.getValue());
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("versionNo", versionNo);
        result.put("code", version.getCode());
        result.put("operation", version.getOperation());
        result.put("operator", version.getOperator());
        result.put("operatedTime", version.getOperatedTime());
        result.put("remark", version.getRemark());
        result.put("attributes", masked);
        return result;
    }

    private Map<String, Object> decryptAll(Map<String, Object> attrs) {
        Map<String, Object> plain = new LinkedHashMap<>(attrs);
        for (Map.Entry<String, Object> entry : plain.entrySet()) {
            if (entry.getValue() instanceof String text && text.startsWith(CryptoService.CIPHER_PREFIX)) {
                // 解密失败不阻断 diff（密钥轮换场景降级为密文展示）
                try {
                    entry.setValue(cryptoService.decrypt(text));
                } catch (Exception ignored) {
                    // 保持密文
                }
            }
        }
        return plain;
    }

    private Map<String, String> fieldLabels(Long modelId) {
        Map<String, String> labels = new LinkedHashMap<>();
        modelRepository.findByIdAndDelFlag(modelId, 0).ifPresent(model -> {
            for (FieldDef def : parseFieldDefs(model.getFieldDefs())) {
                labels.put(def.getName(), def.getLabel());
            }
        });
        return labels;
    }

    private DataVersionVO toVO(DataVersionEntity entity) {
        DataVersionVO vo = new DataVersionVO();
        vo.setId(entity.getId());
        vo.setDataId(entity.getDataId());
        vo.setVersionNo(entity.getVersionNo());
        vo.setOperation(entity.getOperation());
        vo.setOperator(entity.getOperator());
        vo.setOperatedTime(entity.getOperatedTime());
        vo.setRemark(entity.getRemark());
        return vo;
    }

    private MasterDataEntity requireData(Long dataId) {
        return dataRepository.findByIdAndDelFlag(dataId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "主数据不存在：" + dataId));
    }

    private DataVersionEntity requireVersion(Long dataId, Integer versionNo) {
        return versionRepository.findByDataIdAndVersionNoAndDelFlag(dataId, versionNo, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "数据版本不存在：v" + versionNo));
    }

    private Map<String, Object> parse(String json) {
        try {
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
            return map == null ? new LinkedHashMap<>() : map;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "版本快照 JSON 解析失败：" + ex.getMessage());
        }
    }

    private List<FieldDef> parseFieldDefs(String json) {
        try {
            List<FieldDef> defs = objectMapper.readValue(json, new TypeReference<List<FieldDef>>() {
            });
            return defs == null ? new ArrayList<>() : defs;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "字段定义 JSON 解析失败：" + ex.getMessage());
        }
    }

    private static String textOf(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String operator() {
        UserContext ctx = UserContext.get();
        return ctx == null ? "system" : ctx.operator();
    }
}
