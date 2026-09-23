package com.mdm.platform.stats;

import com.mdm.platform.audit.OperationLogRepository;
import com.mdm.platform.category.CategoryRepository;
import com.mdm.platform.common.ApiResponse;
import com.mdm.platform.common.Times;
import com.mdm.platform.data.MasterDataRepository;
import com.mdm.platform.model.ModelRepository;
import com.mdm.platform.push.CollaborationRepository;
import com.mdm.platform.push.PushLogRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计接口（契约 3.8：/stats/overview，首页看板数据）。
 */
@RestController
@RequestMapping("/api/v1/stats")
public class StatsController {

    private final ModelRepository modelRepository;
    private final MasterDataRepository dataRepository;
    private final CategoryRepository categoryRepository;
    private final CollaborationRepository collaborationRepository;
    private final PushLogRepository pushLogRepository;
    private final OperationLogRepository logRepository;

    public StatsController(ModelRepository modelRepository,
                           MasterDataRepository dataRepository,
                           CategoryRepository categoryRepository,
                           CollaborationRepository collaborationRepository,
                           PushLogRepository pushLogRepository,
                           OperationLogRepository logRepository) {
        this.modelRepository = modelRepository;
        this.dataRepository = dataRepository;
        this.categoryRepository = categoryRepository;
        this.collaborationRepository = collaborationRepository;
        this.pushLogRepository = pushLogRepository;
        this.logRepository = logRepository;
    }

    /** 首页统计：模型/数据/协同/推送/日志总览。 */
    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
        Pageable unpaged = PageRequest.of(0, Integer.MAX_VALUE);
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("categoryCount", categoryRepository.findByDelFlagOrderBySortNoAscIdAsc(0).size());
        stats.put("modelTotal", modelRepository.findByDelFlag(0, unpaged).getTotalElements());
        stats.put("modelOnline", modelRepository.findByDelFlag(0, unpaged).stream()
                .filter(m -> "ONLINE".equals(m.getStatus())).count());
        stats.put("dataTotal", dataRepository.countByDelFlag(0));
        stats.put("dataValid", dataRepository.countByDelFlagAndStatus(0, "VALID"));
        stats.put("dataDisabled", dataRepository.countByDelFlagAndStatus(0, "DISABLED"));
        stats.put("todayNewData", dataRepository.count(todayNewSpec()));
        stats.put("pendingCollaboration", collaborationRepository.count(pendingCollabSpec()));
        stats.put("pushSuccess", pushLogRepository.count(pushResultSpec("SUCCESS")));
        stats.put("pushFail", pushLogRepository.count(pushResultSpec("FAIL")));
        stats.put("todayOperations", logRepository.count(todayOperationSpec()));
        return ApiResponse.ok(stats);
    }

    private Specification<com.mdm.platform.data.MasterDataEntity> todayNewSpec() {
        String today = Times.now().substring(0, 10);
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("delFlag"), 0));
            predicates.add(cb.like(root.get("createdTime"), today + "%"));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<com.mdm.platform.push.CollaborationEntity> pendingCollabSpec() {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("delFlag"), 0));
            predicates.add(cb.equal(root.get("status"), "PENDING"));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<com.mdm.platform.push.PushLogEntity> pushResultSpec(String result) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("delFlag"), 0));
            predicates.add(cb.equal(root.get("result"), result));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<com.mdm.platform.audit.OperationLogEntity> todayOperationSpec() {
        String today = Times.now().substring(0, 10);
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("delFlag"), 0));
            predicates.add(cb.like(root.get("operatedTime"), today + "%"));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
