package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.processorder.dto.FinishConfigSaveDTO;
import com.paper.mes.processorder.dto.OriginalRollDTO;
import com.paper.mes.processorder.dto.PlanPreviewVO;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.dto.ProcessOrderAppendDTO;
import com.paper.mes.processorder.dto.ProcessOrderAppendVO;
import com.paper.mes.processorder.dto.ProcessRoutePreviewDTO;
import com.paper.mes.processorder.dto.ProcessStepBatchDTO;
import com.paper.mes.processorder.dto.ProcessStepDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessConfigDraft;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.entity.ProcessOrderAppendRoll;
import com.paper.mes.processorder.entity.ProcessOrderAppendSession;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import com.paper.mes.processorder.mapper.ProcessConfigDraftMapper;
import com.paper.mes.processorder.mapper.ProcessOrderAppendRollMapper;
import com.paper.mes.processorder.mapper.ProcessOrderAppendSessionMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProcessOrderAppendService {

    private static final String DRAFT = "DRAFT";
    private static final String READY = "READY";
    private static final String APPLIED = "APPLIED";
    private static final int STATUS_DRAFT = 0;
    private static final int ROLL_STATUS_PENDING = 1;

    private final ProcessOrderMapper orderMapper;
    private final ProcessOrderAppendSessionMapper sessionMapper;
    private final ProcessOrderAppendRollMapper rollMapper;
    private final OriginalRollMapper originalRollMapper;
    private final ProcessConfigDraftMapper configDraftMapper;
    private final ProcessStepMapper processStepMapper;
    private final ProcessOrderService processOrderService;
    private final ProcessPlanDraftPreviewer planPreviewer;
    private final ProcessPlanMapper planMapper;
    private final ProcessRouteSaveService routeSaveService;
    private final ProcessOrderAppendJson json;
    private final BusinessLockService businessLockService;

    @Transactional(rollbackFor = Exception.class)
    public ProcessOrderAppendVO start(String orderUuid, ProcessOrderAppendDTO.Create request) {
        businessLockService.lockProcessOrders(List.of(orderUuid));
        ProcessOrder order = requireOrder(orderUuid);
        ProcessOrderAppendSession activeSession = findActiveSession(orderUuid);
        if (activeSession != null) {
            ProcessOrderAppendVersionPolicy.requireAppendableStatus(order.getOrderStatus());
            return toView(activeSession, order, listRolls(activeSession.getUuid()));
        }
        requireAppendable(order, request.getExpectedOrderVersion());
        ProcessOrderAppendSession session = new ProcessOrderAppendSession();
        session.setOrderUuid(orderUuid);
        session.setSessionNo("APPEND-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        session.setBaseOrderVersion(order.getVersion());
        session.setStatus(DRAFT);
        session.setReason(request.getReason());
        session.setOperator(AuthContextHolder.currentDisplayName());
        sessionMapper.insert(session);
        return toView(session, order, List.of());
    }

    public ProcessOrderAppendVO get(String orderUuid, String sessionUuid) {
        ProcessOrderAppendSession session = requireSession(orderUuid, sessionUuid);
        return toView(session, requireOrder(orderUuid), listRolls(sessionUuid));
    }

    @Transactional(rollbackFor = Exception.class)
    public ProcessOrderAppendVO saveRolls(String orderUuid, String sessionUuid,
                                          ProcessOrderAppendDTO.RollBatch request) {
        ProcessOrderAppendSession session = lockMutableSession(orderUuid, sessionUuid,
                request.getExpectedSessionVersion());
        if (request.getRolls().isEmpty()) throw new BusinessException("追加至少需要一条母卷");
        List<ProcessOrderAppendRoll> existingRolls = listRolls(sessionUuid);
        Map<String, ProcessOrderAppendRoll> existingByUuid = new LinkedHashMap<>();
        existingRolls.forEach(roll -> existingByUuid.put(roll.getUuid(), roll));
        Set<String> retained = new HashSet<>();
        Set<String> directlyInvalidated = new HashSet<>();
        Set<String> changedOrRemovedSources = new HashSet<>();
        int sort = 1;
        for (ProcessOrderAppendDTO.AppendRoll dto : request.getRolls()) {
            ProcessModePolicy.requireValid(dto.getProcessMode(), dto.getMainStepType());
            ProcessOrderAppendRoll existing = dto.getUuid() == null ? null : existingByUuid.get(dto.getUuid());
            if (dto.getUuid() != null && existing == null) {
                throw new BusinessException(ErrorCode.E002, "追加母卷不存在，请刷新后重试");
            }
            if (existing == null) {
                insertAppendRoll(sessionUuid, dto, sort++);
                continue;
            }
            if (!retained.add(existing.getUuid())) {
                throw new BusinessException(ErrorCode.E003, "同一追加母卷不能重复提交");
            }
            if (ProcessOrderAppendRollChangePolicy.sourceChanged(existing, dto)) {
                directlyInvalidated.add(existing.getUuid());
                changedOrRemovedSources.add(existing.getUuid());
            } else if (ProcessOrderAppendRollChangePolicy.processChanged(existing, dto)) {
                directlyInvalidated.add(existing.getUuid());
            }
            updateAppendRoll(existing, dto, sort++);
        }
        for (ProcessOrderAppendRoll existing : existingRolls) {
            if (retained.contains(existing.getUuid())) continue;
            changedOrRemovedSources.add(existing.getUuid());
            ConcurrencyGuard.requireRowUpdated(rollMapper.deleteById(existing.getUuid()));
        }
        invalidateAffectedConfigs(sessionUuid, directlyInvalidated, changedOrRemovedSources);
        session.setStatus(DRAFT);
        updateSession(session, request.getExpectedSessionVersion());
        return get(orderUuid, sessionUuid);
    }

    private void insertAppendRoll(String sessionUuid, ProcessOrderAppendDTO.AppendRoll dto, int sort) {
        ProcessOrderAppendRoll roll = new ProcessOrderAppendRoll();
        BeanUtils.copyProperties(dto, roll);
        roll.setUuid(null);
        roll.setSessionUuid(sessionUuid);
        roll.setRowSort(sort);
        roll.setPieceNum(dto.getPieceNum() == null ? 1 : dto.getPieceNum());
        roll.setConfigStatus(0);
        roll.setConfigType("singlePlan");
        rollMapper.insert(roll);
        applyServiceSteps(roll, dto);
        if (roll.getServiceStepsJson() != null || Integer.valueOf(1).equals(roll.getConfigStatus())) {
            ConcurrencyGuard.requireRowUpdated(rollMapper.updateById(roll));
        }
    }

    private void updateAppendRoll(ProcessOrderAppendRoll roll, ProcessOrderAppendDTO.AppendRoll dto, int sort) {
        BeanUtils.copyProperties(dto, roll, "uuid");
        roll.setRowSort(sort);
        roll.setPieceNum(dto.getPieceNum() == null ? 1 : dto.getPieceNum());
        applyServiceSteps(roll, dto);
        ConcurrencyGuard.requireRowUpdated(rollMapper.updateById(roll));
    }

    private void applyServiceSteps(ProcessOrderAppendRoll roll, ProcessOrderAppendDTO.AppendRoll dto) {
        if (dto.getServiceSteps() == null) {
            refreshServiceOnlyStatus(roll);
            return;
        }
        List<ProcessStepDTO> normalized = normalizeServiceSteps(
                dto.getServiceSteps(), roll.getServiceStepsJson(), roll.getUuid(), roll.getProcessMode());
        roll.setServiceStepsJson(normalized.isEmpty() ? null : json.write(normalized));
        refreshServiceOnlyStatus(roll);
    }

    private List<ProcessStepDTO> normalizeServiceSteps(List<ProcessStepDTO> requested, String currentJson,
                                                        String originalUuid, Integer processMode) {
        if (!requested.isEmpty() && !ProcessModePolicy.supportsServiceSteps(processMode)) {
            throw new BusinessException("Direct-ship append rolls cannot contain service steps");
        }
        Map<Integer, String> existingIds = new LinkedHashMap<>();
        for (ProcessStepDTO step : json.readServiceSteps(currentJson)) {
            if (step.getStepType() != null) existingIds.put(step.getStepType(), step.getUuid());
        }
        Set<Integer> types = new LinkedHashSet<>();
        List<ProcessStepDTO> normalized = new ArrayList<>();
        for (ProcessStepDTO source : requested) {
            if (!Integer.valueOf(3).equals(source.getStepType())
                    && !Integer.valueOf(4).equals(source.getStepType())) {
                throw new BusinessException("追加附加工艺仅支持剥损整理或重新包装");
            }
            if (Integer.valueOf(1).equals(source.getIsMain())
                    || !types.add(source.getStepType())) {
                throw new BusinessException("同一母卷不能重复配置相同附加工艺");
            }
            ProcessStepDTO step = new ProcessStepDTO();
            BeanUtils.copyProperties(source, step);
            step.setUuid(existingIds.getOrDefault(source.getStepType(), UUID.randomUUID().toString()));
            step.setOriginalUuid(originalUuid);
            step.setIsMain(0);
            normalized.add(step);
        }
        return normalized;
    }

    private void refreshServiceOnlyStatus(ProcessOrderAppendRoll roll) {
        if (!ProcessModePolicy.isServiceOnly(roll.getProcessMode())) return;
        roll.setConfigStatus(json.readServiceSteps(roll.getServiceStepsJson()).isEmpty() ? 0 : 1);
    }

    private void invalidateAffectedConfigs(String sessionUuid, Set<String> directlyInvalidated,
                                           Set<String> changedOrRemovedSources) {
        for (ProcessOrderAppendRoll roll : listRolls(sessionUuid)) {
            if (!Integer.valueOf(1).equals(roll.getConfigStatus())) continue;
            boolean directlyAffected = directlyInvalidated.contains(roll.getUuid());
            boolean dependencyAffected = json.referencesAnyOriginalUuid(
                    roll.getConfigJson(), changedOrRemovedSources);
            if (!directlyAffected && !dependencyAffected) continue;
            clearConfig(roll);
            ConcurrencyGuard.requireRowUpdated(rollMapper.updateById(roll));
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ProcessOrderAppendVO saveProcessSettings(String orderUuid, String sessionUuid,
                                                    ProcessOrderAppendDTO.ProcessSettings request) {
        ProcessOrderAppendSession session = lockMutableSession(orderUuid, sessionUuid,
                request.getExpectedSessionVersion());
        for (ProcessOrderAppendDTO.RollProcess setting : request.getRolls()) {
            ProcessOrderAppendRoll roll = requireRoll(sessionUuid, setting.getRollUuid());
            boolean changed = !java.util.Objects.equals(roll.getProcessMode(), setting.getProcessMode())
                    || !java.util.Objects.equals(roll.getMainStepType(), setting.getMainStepType());
            roll.setProcessMode(setting.getProcessMode());
            roll.setMainStepType(setting.getMainStepType());
            roll.setMachineUuid(setting.getMachineUuid());
            if (changed) clearConfig(roll);
            ConcurrencyGuard.requireRowUpdated(rollMapper.updateById(roll));
        }
        updateSession(session, request.getExpectedSessionVersion());
        return get(orderUuid, sessionUuid);
    }

    @Transactional(rollbackFor = Exception.class)
    public ProcessOrderAppendVO savePlan(String orderUuid, String sessionUuid,
                                         ProcessOrderAppendDTO.PlanSave request) {
        ProcessOrderAppendSession session = lockMutableSession(orderUuid, sessionUuid,
                request.getExpectedSessionVersion());
        ProcessOrderAppendRoll roll = requireRoll(sessionUuid, request.getRollUuid());
        if (!java.util.Objects.equals(roll.getProcessMode(), request.getConfig().getProcessMode())) {
            throw new BusinessException("工艺配置与母卷加工方式不一致");
        }
        ProcessPlanDTO plan = planMapper.fromSaveDto(request.getConfig());
        PlanPreviewVO preview = previewPlan(orderUuid, sessionUuid, roll, plan, session.getVersion());
        requireReadyPreview(preview);
        roll.setConfigJson(json.write(request.getConfig()));
        roll.setPreviewJson(json.write(preview));
        roll.setConfigType(request.getConfigType() == null ? "singlePlan" : request.getConfigType());
        roll.setConfigStatus(1);
        roll.setLastError(null);
        ConcurrencyGuard.requireRowUpdated(rollMapper.updateById(roll));
        updateSession(session, request.getExpectedSessionVersion());
        return get(orderUuid, sessionUuid);
    }

    public PlanPreviewVO previewPlan(String orderUuid, String sessionUuid, String rollUuid,
                                     ProcessOrderAppendDTO.PlanPreview request) {
        ProcessOrderAppendSession session = requireMutableSession(orderUuid, sessionUuid,
                request.getExpectedSessionVersion());
        ProcessOrderAppendRoll roll = requireRoll(sessionUuid, rollUuid);
        return previewPlan(orderUuid, sessionUuid, roll, request.getPlan(), session.getVersion());
    }

    @Transactional(rollbackFor = Exception.class)
    public ProcessOrderAppendVO preview(String orderUuid, String sessionUuid,
                                        ProcessOrderAppendDTO.Preview request) {
        ProcessOrderAppendSession session = lockMutableSession(orderUuid, sessionUuid,
                request.getExpectedSessionVersion());
        validateReadyRolls(listRolls(sessionUuid));
        session.setStatus(READY);
        updateSession(session, request.getExpectedSessionVersion());
        return get(orderUuid, sessionUuid);
    }

    @Transactional(rollbackFor = Exception.class)
    public ProcessOrderAppendVO.CommitResult commit(String orderUuid, String sessionUuid,
                                                    ProcessOrderAppendDTO.Commit request) {
        businessLockService.lockProcessOrders(List.of(orderUuid));
        ProcessOrderAppendSession session = requireSession(orderUuid, sessionUuid);
        if (APPLIED.equals(session.getStatus())) {
            if (!request.getRequestId().equals(session.getCommitRequestId()))
                throw new BusinessException(ErrorCode.E003, "追加会话已提交，请勿重复提交");
            return commitResult(orderUuid, session);
        }
        ProcessOrder order = requireOrder(orderUuid);
        requireAppendable(order, request.getExpectedOrderVersion());
        List<ProcessOrderAppendRoll> rolls = listRolls(sessionUuid);
        validateReadyRolls(rolls);
        Map<String, String> ids = new LinkedHashMap<>();
        List<ProcessStepDTO> serviceSteps = new ArrayList<>();
        for (ProcessOrderAppendRoll draft : rolls) {
            String actualUuid = insertRoll(order, draft);
            ids.put(draft.getUuid(), actualUuid);
            serviceSteps.addAll(readServiceSteps(draft, actualUuid));
        }
        for (ProcessOrderAppendRoll draft : rolls) applyConfig(orderUuid, order, draft, ids);
        if (!serviceSteps.isEmpty()) {
            ProcessStepBatchDTO batch = new ProcessStepBatchDTO();
            batch.setExpectedVersion(order.getVersion());
            batch.setSteps(serviceSteps);
            processOrderService.addProcessSteps(orderUuid, batch);
        }
        processOrderService.calcFee(orderUuid);
        session.setBaseOrderVersion(request.getExpectedOrderVersion());
        session.setStatus(APPLIED);
        session.setCommitRequestId(request.getRequestId());
        session.setApplyTime(LocalDateTime.now());
        session.setOperator(AuthContextHolder.currentDisplayName());
        ConcurrencyGuard.requireRowUpdated(sessionMapper.updateById(session));
        return commitResult(orderUuid, session);
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(String orderUuid, String sessionUuid) {
        ProcessOrderAppendSession session = requireSession(orderUuid, sessionUuid);
        if (!DRAFT.equals(session.getStatus()) && !READY.equals(session.getStatus()))
            throw new BusinessException("当前追加会话不可取消");
        session.setStatus("CANCELLED");
        ConcurrencyGuard.requireRowUpdated(sessionMapper.updateById(session));
    }

    private void applyConfig(String orderUuid, ProcessOrder order, ProcessOrderAppendRoll draft,
                             Map<String, String> ids) {
        if (ProcessModePolicy.isDirectShip(draft.getProcessMode())
                || ProcessModePolicy.isServiceOnly(draft.getProcessMode())) return;
        String actualUuid = ids.get(draft.getUuid());
        String configJson = json.replaceOriginalUuids(draft.getConfigJson(), ids);
        if (order.getOrderStatus() == STATUS_DRAFT) {
            saveDraftConfig(order, draft, actualUuid, configJson);
            return;
        }
        if ("routePlan".equals(draft.getConfigType())) {
            ProcessRoutePreviewDTO route = json.readRoute(configJson);
            route.setOriginalUuid(actualUuid);
            route.setExpectedVersion(currentOrderVersion(orderUuid));
            routeSaveService.save(orderUuid, route);
            return;
        }
        FinishConfigSaveDTO config = json.readPlan(configJson);
        config.setProcessMode(draft.getProcessMode());
        processOrderService.saveFinishConfig(orderUuid, actualUuid, config);
    }

    private void saveDraftConfig(ProcessOrder order, ProcessOrderAppendRoll draft,
                                  String actualUuid, String configJson) {
        ProcessConfigDraft entity = new ProcessConfigDraft();
        entity.setOrderUuid(order.getUuid());
        entity.setOriginalUuid(actualUuid);
        entity.setProcessMode(draft.getProcessMode());
        entity.setMainStepType(draft.getMainStepType());
        entity.setConfigJson(configJson);
        entity.setPreviewJson(draft.getPreviewJson());
        entity.setConfigStatus(1);
        configDraftMapper.insert(entity);
    }

    private String insertRoll(ProcessOrder order, ProcessOrderAppendRoll draft) {
        OriginalRoll roll = new OriginalRoll();
        BeanUtils.copyProperties(draft, roll);
        roll.setUuid(draft.getUuid());
        roll.setOrderUuid(order.getUuid());
        roll.setOrderNo(order.getOrderNo());
        roll.setCustomerName(order.getCustomerName());
        roll.setRollStatus(ROLL_STATUS_PENDING);
        roll.setTotalWeight(draft.getRollWeight().multiply(java.math.BigDecimal.valueOf(draft.getPieceNum())));
        originalRollMapper.insert(roll);
        if (ProcessModePolicy.requiresMainProcess(draft.getProcessMode())) {
            ProcessStep step = new ProcessStep();
            step.setOrderUuid(order.getUuid());
            step.setOriginalUuid(roll.getUuid());
            step.setStepSort(1);
            step.setStepType(draft.getMainStepType());
            step.setIsMain(1);
            processStepMapper.insert(step);
        }
        return roll.getUuid();
    }

    private List<ProcessStepDTO> readServiceSteps(ProcessOrderAppendRoll draft, String actualUuid) {
        List<ProcessStepDTO> steps = json.readServiceSteps(draft.getServiceStepsJson());
        steps.forEach(step -> {
            step.setOriginalUuid(actualUuid);
            step.setIsMain(0);
        });
        return steps;
    }

    private void validateReadyRolls(List<ProcessOrderAppendRoll> rolls) {
        if (rolls.isEmpty()) throw new BusinessException("追加至少需要一条母卷");
        for (ProcessOrderAppendRoll roll : rolls) {
            ProcessModePolicy.requireValid(roll.getProcessMode(), roll.getMainStepType());
            if (ProcessModePolicy.isServiceOnly(roll.getProcessMode())
                    && json.readServiceSteps(roll.getServiceStepsJson()).isEmpty()) {
                throw new BusinessException("仅附加工艺母卷至少需要配置一条附加工艺");
            }
            if (!ProcessModePolicy.isDirectShip(roll.getProcessMode())
                    && !ProcessModePolicy.isServiceOnly(roll.getProcessMode())
                    && !Integer.valueOf(1).equals(roll.getConfigStatus())) {
                throw new BusinessException("新增母卷尚未完成工艺配置");
            }
        }
    }

    private void clearConfig(ProcessOrderAppendRoll roll) {
        roll.setConfigJson(null);
        roll.setPreviewJson(null);
        roll.setConfigStatus(0);
        roll.setLastError(null);
        refreshServiceOnlyStatus(roll);
    }

    private PlanPreviewVO previewPlan(String orderUuid, String sessionUuid,
                                      ProcessOrderAppendRoll roll, ProcessPlanDTO plan,
                                      Integer expectedSessionVersion) {
        if (!java.util.Objects.equals(roll.getProcessMode(), plan.getProcessMode())) {
            throw new BusinessException("工艺配置与母卷加工方式不一致");
        }
        ProcessModePolicy.requireValid(plan.getProcessMode(), plan.getMainStepType());
        FinishConfigQuantityValidator.requireWithinLimit(plan);
        ProcessOrder order = requireOrder(orderUuid);
        Map<String, OriginalRoll> sourceRolls = new LinkedHashMap<>();
        originalRollMapper.selectList(new LambdaQueryWrapper<OriginalRoll>()
                        .eq(OriginalRoll::getOrderUuid, orderUuid))
                .forEach(source -> sourceRolls.put(source.getUuid(), source));
        for (ProcessOrderAppendRoll draft : listRolls(sessionUuid)) {
            sourceRolls.put(draft.getUuid(), toOriginalRoll(order, draft));
        }
        OriginalRoll target = sourceRolls.get(roll.getUuid());
        if (target == null) {
            throw new BusinessException(ErrorCode.E002, "追加母卷不存在");
        }
        ProcessPlanSaveCandidate candidate = new ProcessPlanSaveCandidate(target, plan, null);
        ProcessPlanDraftPreviewContext context = planPreviewer.createContext(order, sourceRolls,
                List.of(candidate));
        return planPreviewer.preview(context, target, plan);
    }

    private void requireReadyPreview(PlanPreviewVO preview) {
        if (preview.isReady()) return;
        String message = preview.getErrors().isEmpty()
                ? "新增母卷工艺预览未通过" : String.join("；", preview.getErrors());
        throw new BusinessException(message);
    }

    private OriginalRoll toOriginalRoll(ProcessOrder order, ProcessOrderAppendRoll source) {
        OriginalRoll target = new OriginalRoll();
        BeanUtils.copyProperties(source, target);
        target.setOrderUuid(order.getUuid());
        target.setOrderNo(order.getOrderNo());
        target.setCustomerName(order.getCustomerName());
        target.setTotalWeight(source.getRollWeight()
                .multiply(java.math.BigDecimal.valueOf(source.getPieceNum())));
        return target;
    }

    private ProcessOrderAppendSession lockMutableSession(String orderUuid, String sessionUuid, Integer version) {
        ProcessOrderAppendSession session = requireSession(orderUuid, sessionUuid);
        if (!DRAFT.equals(session.getStatus()) && !READY.equals(session.getStatus()))
            throw new BusinessException("当前追加会话不可编辑");
        if (!java.util.Objects.equals(session.getVersion(), version))
            throw new BusinessException(ErrorCode.E006, "追加会话已被其他页面修改，请刷新后重试");
        return session;
    }

    private ProcessOrderAppendSession requireMutableSession(String orderUuid, String sessionUuid,
                                                             Integer version) {
        ProcessOrderAppendSession session = requireSession(orderUuid, sessionUuid);
        if (!DRAFT.equals(session.getStatus()) && !READY.equals(session.getStatus())) {
            throw new BusinessException("当前追加会话不可编辑");
        }
        if (!java.util.Objects.equals(session.getVersion(), version)) {
            throw new BusinessException(ErrorCode.E006, "追加会话已被其他页面修改，请刷新后重试");
        }
        return session;
    }

    private void updateSession(ProcessOrderAppendSession session, Integer expectedVersion) {
        session.setVersion(expectedVersion);
        ConcurrencyGuard.requireRowUpdated(sessionMapper.updateById(session));
    }

    private ProcessOrder requireOrder(String uuid) {
        ProcessOrder order = orderMapper.selectById(uuid);
        if (order == null) throw new BusinessException(ErrorCode.E002, "加工单不存在");
        return order;
    }

    private ProcessOrderAppendSession requireSession(String orderUuid, String sessionUuid) {
        ProcessOrderAppendSession session = sessionMapper.selectById(sessionUuid);
        if (session == null || !orderUuid.equals(session.getOrderUuid()))
            throw new BusinessException(ErrorCode.E002, "追加会话不存在");
        return session;
    }

    private ProcessOrderAppendRoll requireRoll(String sessionUuid, String rollUuid) {
        ProcessOrderAppendRoll roll = rollMapper.selectById(rollUuid);
        if (roll == null || !sessionUuid.equals(roll.getSessionUuid()))
            throw new BusinessException(ErrorCode.E002, "追加母卷不存在");
        return roll;
    }

    private List<ProcessOrderAppendRoll> listRolls(String sessionUuid) {
        return rollMapper.selectList(new LambdaQueryWrapper<ProcessOrderAppendRoll>()
                .eq(ProcessOrderAppendRoll::getSessionUuid, sessionUuid)
                .orderByAsc(ProcessOrderAppendRoll::getRowSort));
    }

    private void requireAppendable(ProcessOrder order, Integer expectedVersion) {
        ProcessOrderAppendVersionPolicy.requireCurrentVersion(order.getVersion(), expectedVersion);
        ProcessOrderAppendVersionPolicy.requireAppendableStatus(order.getOrderStatus());
    }

    private ProcessOrderAppendSession findActiveSession(String orderUuid) {
        List<ProcessOrderAppendSession> sessions = sessionMapper.selectList(new LambdaQueryWrapper<ProcessOrderAppendSession>()
                .eq(ProcessOrderAppendSession::getOrderUuid, orderUuid)
                .in(ProcessOrderAppendSession::getStatus, DRAFT, READY)
                .orderByDesc(ProcessOrderAppendSession::getCreateTime));
        return sessions.isEmpty() ? null : sessions.get(0);
    }

    private Integer currentOrderVersion(String orderUuid) {
        ProcessOrder order = requireOrder(orderUuid);
        return order.getVersion();
    }

    private ProcessOrderAppendVO.CommitResult commitResult(String orderUuid, ProcessOrderAppendSession session) {
        ProcessOrderAppendVO.CommitResult result = new ProcessOrderAppendVO.CommitResult();
        result.setSessionUuid(session.getUuid());
        result.setOrderUuid(orderUuid);
        result.setOrderVersion(currentOrderVersion(orderUuid));
        result.setRollUuids(listRolls(session.getUuid()).stream().map(ProcessOrderAppendRoll::getUuid).toList());
        return result;
    }

    private ProcessOrderAppendVO toView(ProcessOrderAppendSession session, ProcessOrder order,
                                        List<ProcessOrderAppendRoll> rolls) {
        ProcessOrderAppendVO view = new ProcessOrderAppendVO();
        view.setSessionUuid(session.getUuid());
        view.setOrderUuid(order.getUuid());
        view.setOrderNo(order.getOrderNo());
        view.setBaseOrderVersion(session.getBaseOrderVersion());
        view.setCurrentOrderVersion(order.getVersion());
        view.setSessionVersion(session.getVersion());
        view.setStatus(session.getStatus());
        view.setReason(session.getReason());
        view.setRolls(rolls.stream().map(this::toRoll).toList());
        return view;
    }

    private ProcessOrderAppendVO.Roll toRoll(ProcessOrderAppendRoll source) {
        ProcessOrderAppendVO.Roll target = new ProcessOrderAppendVO.Roll();
        BeanUtils.copyProperties(source, target);
        target.setServiceSteps(json.readServiceSteps(source.getServiceStepsJson()));
        if (source.getConfigJson() != null && !source.getConfigJson().isBlank()
                && !"routePlan".equals(source.getConfigType())) {
            target.setConfig(json.readPlan(source.getConfigJson()));
        }
        if (source.getPreviewJson() != null && !source.getPreviewJson().isBlank()) {
            target.setPreview(json.readPreview(source.getPreviewJson()));
        }
        if ("routePlan".equals(source.getConfigType())
                && source.getConfigJson() != null && !source.getConfigJson().isBlank()) {
            target.setRoute(json.readRoute(source.getConfigJson()));
        }
        return target;
    }
}
