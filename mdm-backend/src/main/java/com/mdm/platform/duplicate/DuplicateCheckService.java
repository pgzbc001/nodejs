package com.mdm.platform.duplicate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdm.platform.common.BusinessException;
import com.mdm.platform.common.ErrorCode;
import com.mdm.platform.data.MasterDataEntity;
import com.mdm.platform.data.MasterDataRepository;
import com.mdm.platform.duplicate.dto.DuplicateCheckRequest;
import com.mdm.platform.duplicate.dto.DuplicateCheckResultVO;
import com.mdm.platform.duplicate.dto.SimilarItemVO;
import com.mdm.platform.model.ModelEntity;
import com.mdm.platform.model.ModelRepository;
import com.mdm.platform.model.dto.FieldDef;
import com.mdm.platform.security.CryptoService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 查重服务（REQ-BKD07）：以"名称/规格/型号"类关键字段与同模型已有数据比对，
 * 返回 Top 5 相似数据；≥80 标记高度相似。算法经适配器注入，可替换。
 */
@Service
public class DuplicateCheckService {

    /** 入选阈值 */
    static final double INCLUDE_THRESHOLD = 60;
    /** 高度相似阈值 */
    static final double HIGH_THRESHOLD = 80;
    /** 返回条数上限 */
    private static final int TOP_N = 5;

    private final MasterDataRepository dataRepository;
    private final ModelRepository modelRepository;
    private final DuplicateCheckAdapter adapter;
    private final CryptoService cryptoService;
    private final ObjectMapper objectMapper;

    public DuplicateCheckService(MasterDataRepository dataRepository,
                                 ModelRepository modelRepository,
                                 DuplicateCheckAdapter adapter,
                                 CryptoService cryptoService) {
        this.dataRepository = dataRepository;
        this.modelRepository = modelRepository;
        this.adapter = adapter;
        this.cryptoService = cryptoService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 草稿查重：比对同模型全部未删除数据（编辑场景排除自身）。
     */
    public DuplicateCheckResultVO check(DuplicateCheckRequest request) {
        ModelEntity model = modelRepository.findByIdAndDelFlag(request.getModelId(), 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "模型不存在：" + request.getModelId()));

        List<FieldDef> keyFields = keyFields(model);
        DuplicateCheckResultVO result = new DuplicateCheckResultVO();
        result.setMaxSimilarity(0);
        result.setHasHighSimilar(false);
        result.setItems(new ArrayList<>());
        if (keyFields.isEmpty() || request.getAttributes() == null || request.getAttributes().isEmpty()) {
            return result;
        }

        List<MasterDataEntity> existing = dataRepository.findByModelIdAndDelFlag(model.getId(), 0);
        List<SimilarItemVO> candidates = new ArrayList<>();
        double maxSimilarity = 0;
        for (MasterDataEntity data : existing) {
            if (request.getExcludeDataId() != null && request.getExcludeDataId().equals(data.getId())) {
                continue;
            }
            Map<String, Object> stored = parseAttributes(data.getAttributes());
            double similarity = recordSimilarity(keyFields, request.getAttributes(), stored);
            if (similarity > maxSimilarity) {
                maxSimilarity = similarity;
            }
            if (similarity >= INCLUDE_THRESHOLD) {
                candidates.add(buildItem(data, stored, similarity));
            }
        }
        candidates.sort(Comparator.comparingDouble(SimilarItemVO::getSimilarity).reversed());
        if (candidates.size() > TOP_N) {
            candidates = new ArrayList<>(candidates.subList(0, TOP_N));
        }
        result.setMaxSimilarity(Math.round(maxSimilarity));
        result.setHasHighSimilar(maxSimilarity >= HIGH_THRESHOLD);
        result.setItems(candidates);
        return result;
    }

    /** 单条记录相似度：关键字段逐对计算后平均（权重均 1）。 */
    private double recordSimilarity(List<FieldDef> keyFields, Map<String, Object> draft, Map<String, Object> stored) {
        double total = 0;
        int count = 0;
        for (FieldDef field : keyFields) {
            String a = toText(draft.get(field.getName()));
            String b = toText(stored.get(field.getName()));
            if (field.isEncrypted() && b != null) {
                b = cryptoService.decrypt(b);
            }
            total += adapter.similarity(a, b);
            count++;
        }
        return count == 0 ? 0 : total / count;
    }

    private SimilarItemVO buildItem(MasterDataEntity data, Map<String, Object> stored, double similarity) {
        SimilarItemVO item = new SimilarItemVO();
        item.setDataId(data.getId());
        item.setCode(data.getCode());
        item.setName(data.getName());
        item.setSimilarity(Math.round(similarity));
        item.setHighSimilar(similarity >= HIGH_THRESHOLD);
        item.setAttributes(plainAttributes(stored));
        return item;
    }

    /** 解密后返回明文属性（供前端对比展示）。 */
    public Map<String, Object> plainAttributes(Map<String, Object> stored) {
        Map<String, Object> plain = new HashMap<>(stored);
        for (Map.Entry<String, Object> entry : plain.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String text && text.startsWith(CryptoService.CIPHER_PREFIX)) {
                entry.setValue(cryptoService.decrypt(text));
            }
        }
        return plain;
    }

    /**
     * 关键字段挑选（design.md 3.4）：
     * 名称类（label 含 名称/name）或 TEXT 且 label 含 规格/型号/spec/model。
     */
    public List<FieldDef> keyFields(ModelEntity model) {
        List<FieldDef> all = parseFieldDefs(model.getFieldDefs());
        List<FieldDef> keys = new ArrayList<>();
        for (FieldDef field : all) {
            String label = field.getLabel() == null ? "" : field.getLabel().toLowerCase(Locale.ROOT);
            String name = field.getName() == null ? "" : field.getName().toLowerCase(Locale.ROOT);
            boolean nameLike = label.contains("名称") || label.contains("name") || name.contains("name");
            boolean specLike = "TEXT".equalsIgnoreCase(field.getType())
                    && (label.contains("规格") || label.contains("型号")
                    || label.contains("spec") || label.contains("model")
                    || name.contains("spec") || name.contains("model"));
            if (nameLike || specLike) {
                keys.add(field);
            }
        }
        return keys;
    }

    private List<FieldDef> parseFieldDefs(String fieldDefsJson) {
        try {
            List<FieldDef> defs = objectMapper.readValue(fieldDefsJson, new TypeReference<List<FieldDef>>() {
            });
            return defs == null ? new ArrayList<>() : defs;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "字段定义 JSON 解析失败：" + ex.getMessage());
        }
    }

    private Map<String, Object> parseAttributes(String attributesJson) {
        try {
            Map<String, Object> map = objectMapper.readValue(attributesJson, new TypeReference<Map<String, Object>>() {
            });
            return map == null ? new HashMap<>() : map;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "数据属性 JSON 解析失败：" + ex.getMessage());
        }
    }

    private static String toText(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
