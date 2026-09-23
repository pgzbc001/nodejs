package com.mdm.platform.push;

import com.mdm.platform.audit.OperationLogService;
import com.mdm.platform.common.BusinessException;
import com.mdm.platform.common.ErrorCode;
import com.mdm.platform.common.PageResult;
import com.mdm.platform.common.Times;
import com.mdm.platform.data.MasterDataEntity;
import com.mdm.platform.data.MasterDataRepository;
import com.mdm.platform.data.VersionService;
import com.mdm.platform.push.dto.PrecheckResultVO;
import com.mdm.platform.push.dto.PushExecuteRequest;
import com.mdm.platform.push.dto.PushItemResult;
import com.mdm.platform.push.dto.PushResultVO;
import com.mdm.platform.security.UserContext;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 推送服务（REQ-BKD08）：下游预检 + 推送日志 + 协同确认流（确认/驳回状态机）。
 */
@Service
public class PushService {

    private final PushLogRepository pushLogRepository;
    private final DownstreamSystemRepository systemRepository;
    private final CollaborationRepository collaborationRepository;
    private final MasterDataRepository dataRepository;
    private final DownstreamAdapter downstreamAdapter;
    private final VersionService versionService;
    private final OperationLogService logService;

    public PushService(PushLogRepository pushLogRepository,
                       DownstreamSystemRepository systemRepository,
                       CollaborationRepository collaborationRepository,
                       MasterDataRepository dataRepository,
                       DownstreamAdapter downstreamAdapter,
                       VersionService versionService,
                       OperationLogService logService) {
        this.pushLogRepository = pushLogRepository;
        this.systemRepository = systemRepository;
        this.collaborationRepository = collaborationRepository;
        this.dataRepository = dataRepository;
        this.downstreamAdapter = downstreamAdapter;
        this.versionService = versionService;
        this.logService = logService;
    }

    // ==================== 下游系统 ====================

    public List<DownstreamSystemEntity> systems() {
        return systemRepository.findByDelFlagOrderByIdAsc(0);
    }

    /**
     * 数据预检：对全部启用系统逐个校验（禁用/删除数据前的下游影响评估）。
     */
    public List<PrecheckResultVO> precheckData(Long dataId) {
        MasterDataEntity data = requireData(dataId);
        List<PrecheckResultVO> results = new ArrayList<>();
        for (DownstreamSystemEntity system : systemRepository.findByEnabledAndDelFlag(1, 0)) {
            results.add(buildPrecheck(data, system));
        }
        return results;
    }

    /**
     * 批量预检（契约 POST /push/precheck）：指定数据×指定系统的组合结果。
     */
    public List<PrecheckResultVO> precheck(List<Long> dataIds, List<Long> systemIds) {
        if (dataIds == null || dataIds.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择待预检数据");
        }
        List<DownstreamSystemEntity> systems;
        if (systemIds == null || systemIds.isEmpty()) {
            systems = systemRepository.findByEnabledAndDelFlag(1, 0);
        } else {
            systems = new ArrayList<>();
            for (Long systemId : systemIds) {
                systems.add(systemRepository.findByIdAndDelFlag(systemId, 0)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "下游系统不存在：" + systemId)));
            }
        }
        List<PrecheckResultVO> results = new ArrayList<>();
        for (Long dataId : dataIds) {
            MasterDataEntity data = requireData(dataId);
            for (DownstreamSystemEntity system : systems) {
                results.add(buildPrecheck(data, system));
            }
        }
        return results;
    }

    private PrecheckResultVO buildPrecheck(MasterDataEntity data, DownstreamSystemEntity system) {
        PrecheckResultVO vo = new PrecheckResultVO();
        vo.setDataId(data.getId());
        vo.setDataCode(data.getCode());
        vo.setSystemId(system.getId());
        vo.setSystemCode(system.getCode());
        vo.setSystemName(system.getName());
        vo.setResult(downstreamAdapter.precheck(data, system));
        return vo;
    }

    /**
     * 数据是否被任一启用系统判定 FAIL（供禁用/删除保护）。
     */
    public boolean hasDownstreamFail(Long dataId) {
        MasterDataEntity data = requireData(dataId);
        for (DownstreamSystemEntity system : systemRepository.findByEnabledAndDelFlag(1, 0)) {
            if (DownstreamAdapter.FAIL.equals(downstreamAdapter.precheck(data, system))) {
                return true;
            }
        }
        return false;
    }

    // ==================== 推送执行 ====================

    /**
     * 执行推送：待协同拦截（40908）→ 下游校验拦截（40909，force 可越过）→ 逐条落推送日志。
     */
    @Transactional
    public PushResultVO execute(PushExecuteRequest request) {
        if (request.getDataIds() == null || request.getDataIds().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择待推送数据");
        }
        if (request.getSystemIds() == null || request.getSystemIds().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择目标下游系统");
        }
        List<MasterDataEntity> dataList = new ArrayList<>();
        for (Long dataId : request.getDataIds()) {
            MasterDataEntity data = requireData(dataId);
            if ("PENDING_CONFIRM".equals(data.getCollabStatus())) {
                throw new BusinessException(ErrorCode.COLLAB_PENDING,
                        "数据【" + data.getCode() + "】处于待协同确认状态，禁止推送");
            }
            dataList.add(data);
        }
        List<DownstreamSystemEntity> systemList = new ArrayList<>();
        for (Long systemId : request.getSystemIds()) {
            systemList.add(systemRepository.findByIdAndDelFlag(systemId, 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "下游系统不存在：" + systemId)));
        }

        // 预检（force=false 时 FAIL 拦截）
        if (!request.isForce()) {
            for (MasterDataEntity data : dataList) {
                for (DownstreamSystemEntity system : systemList) {
                    if (DownstreamAdapter.FAIL.equals(downstreamAdapter.precheck(data, system))) {
                        throw new BusinessException(ErrorCode.DOWNSTREAM_CHECK_FAILED,
                                "数据【" + data.getCode() + "】下游系统【" + system.getName() + "】校验未通过");
                    }
                }
            }
        }

        PushResultVO result = new PushResultVO();
        List<PushItemResult> items = new ArrayList<>();
        int success = 0;
        int fail = 0;
        for (MasterDataEntity data : dataList) {
            for (DownstreamSystemEntity system : systemList) {
                String precheck = downstreamAdapter.precheck(data, system);
                boolean ok = !DownstreamAdapter.FAIL.equals(precheck);
                PushItemResult item = new PushItemResult();
                item.setDataId(data.getId());
                item.setDataCode(data.getCode());
                item.setSystemId(system.getId());
                item.setSystemCode(system.getCode());
                item.setSystemName(system.getName());
                item.setResult(ok ? "SUCCESS" : "FAIL");
                item.setDetail(ok
                        ? ("预检 " + precheck + "，推送完成")
                        : "预检 FAIL，强制执行推送但下游未接收");
                items.add(item);
                if (ok) {
                    success++;
                } else {
                    fail++;
                }
                savePushLog(data, system, ok ? "SUCCESS" : "FAIL", item.getDetail());
            }
        }
        result.setSuccessCount(success);
        result.setFailCount(fail);
        result.setItems(items);
        logService.log("PUSH", "PUSH", null, "推送 " + dataList.size() + " 条数据至 " + systemList.size() + " 个系统",
                Map.of("dataIds", request.getDataIds(), "systemIds", request.getSystemIds(),
                        "force", request.isForce(), "success", success, "fail", fail));
        return result;
    }

    private void savePushLog(MasterDataEntity data, DownstreamSystemEntity system, String result, String detail) {
        PushLogEntity log = new PushLogEntity();
        log.setDataId(data.getId());
        log.setDataCode(data.getCode());
        log.setModelId(data.getModelId());
        log.setSystemId(system.getId());
        log.setSystemCode(system.getCode());
        log.setResult(result);
        log.setDetail(detail);
        log.setOperator(operator());
        log.setPushedTime(Times.now());
        pushLogRepository.save(log);
    }

    // ==================== 协同确认 ====================

    /**
     * 协同单分页（status 可筛选 PENDING/CONFIRMED/REJECTED）。
     */
    public PageResult<CollaborationEntity> collaborations(String status, int page, int size) {
        Specification<CollaborationEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("delFlag"), 0));
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status.trim()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, size),
                Sort.by(Sort.Direction.DESC, "id"));
        Page<CollaborationEntity> result = collaborationRepository.findAll(spec, pageable);
        return new PageResult<>(result.getTotalElements(), page, size, result.getContent());
    }

    /**
     * 确认协同：数据转为 CONFIRMED（可推送）。
     */
    @Transactional
    public void confirm(Long collabId, String comment) {
        CollaborationEntity collab = requirePending(collabId);
        MasterDataEntity data = requireData(collab.getDataId());
        collab.setStatus("CONFIRMED");
        collab.setCollaborator(operator());
        collab.setConfirmTime(Times.now());
        collab.setConfirmComment(comment);
        collaborationRepository.save(collab);
        data.setCollabStatus("CONFIRMED");
        data.setUpdatedBy(operator());
        data.setUpdatedTime(Times.now());
        dataRepository.save(data);
        logService.log("COLLABORATION", "CONFIRM", collabId, "协同确认：" + data.getCode(),
                Map.of("comment", comment == null ? "" : comment));
    }

    /**
     * 驳回协同：数据回滚到修改前版本（v-1），状态转 REJECTED。
     */
    @Transactional
    public void reject(Long collabId, String comment) {
        CollaborationEntity collab = requirePending(collabId);
        MasterDataEntity data = requireData(collab.getDataId());
        collab.setStatus("REJECTED");
        collab.setCollaborator(operator());
        collab.setConfirmTime(Times.now());
        collab.setConfirmComment(comment);
        collaborationRepository.save(collab);
        if (data.getVersionNo() > 1) {
            versionService.rollback(data.getId(), data.getVersionNo() - 1);
            data = requireData(data.getId());
        }
        data.setCollabStatus("REJECTED");
        data.setUpdatedBy(operator());
        data.setUpdatedTime(Times.now());
        dataRepository.save(data);
        logService.log("COLLABORATION", "REJECT", collabId, "协同驳回（已恢复原值）：" + data.getCode(),
                Map.of("comment", comment == null ? "" : comment));
    }

    // ==================== 推送日志 ====================

    /**
     * 推送日志分页（可按数据过滤）。
     */
    public PageResult<PushLogEntity> pushLogs(Long dataId, int page, int size) {
        Specification<PushLogEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("delFlag"), 0));
            if (dataId != null) {
                predicates.add(cb.equal(root.get("dataId"), dataId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, size),
                Sort.by(Sort.Direction.DESC, "pushedTime").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<PushLogEntity> result = pushLogRepository.findAll(spec, pageable);
        return new PageResult<>(result.getTotalElements(), page, size, result.getContent());
    }

    /**
     * 批量数据的推送历史（按数据 ID 集合）。
     */
    public List<PushLogEntity> logsOfData(Set<Long> dataIds) {
        List<PushLogEntity> logs = new ArrayList<>();
        for (Long dataId : dataIds) {
            logs.addAll(pushLogRepository.findByDataIdAndDelFlagOrderByPushedTimeDesc(dataId, 0));
        }
        return logs;
    }

    private CollaborationEntity requirePending(Long collabId) {
        CollaborationEntity collab = collaborationRepository.findById(collabId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "协同单不存在：" + collabId));
        if (collab.getDelFlag() != 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "协同单不存在：" + collabId);
        }
        if (!collab.isPending()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "协同单已处理（" + collab.getStatus() + "），不可重复操作");
        }
        return collab;
    }

    private MasterDataEntity requireData(Long dataId) {
        return dataRepository.findByIdAndDelFlag(dataId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "主数据不存在：" + dataId));
    }

    private static String operator() {
        UserContext ctx = UserContext.get();
        return ctx == null ? "system" : ctx.operator();
    }
}
