package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.paper.mes.ai.process.compile.ProcessAiCompiledPlan;
import com.paper.mes.ai.process.compile.ProcessAiCompilationResult;
import com.paper.mes.ai.process.compile.ProcessAiPackagingCandidate;
import com.paper.mes.ai.process.compile.ProcessAiRollConfiguration;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.dto.DraftRollProcessDTO;
import com.paper.mes.processorder.dto.ProcessStepDTO;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProcessAiDraftApplicationService {

    private static final int DRAFT_STATUS = 0;

    private final BusinessLockService lockService;
    private final DraftOrderVersionGuard versionGuard;
    private final ProcessOrderMapper orderMapper;
    private final ProcessAiDraftPlanApplier planApplier;
    private final DraftRollProcessManager rollProcessManager;
    private final OriginalRollMapper rollMapper;
    private final ServiceStepBatchUpsertWriter serviceStepWriter;
    private final ProcessAiServiceStepRequestFactory serviceStepRequestFactory;
    private final ProcessOrderService processOrderService;

    @Transactional(rollbackFor = Exception.class)
    public ProcessAiDraftApplyResult apply(ProcessAiDraftApplyCommand command) {
        lockService.lockProcessOrders(List.of(command.orderUuid()));
        ProcessOrder order = requireDraft(command.orderUuid(), command.expectedVersion());
        rollProcessManager.applyLocked(order, selectedConfigurations(command));
        List<ProcessAiCompiledPlan> selected = selectedPlans(command);
        Map<String, ProcessAiCompiledPlan> appliedPlans = planApplier.apply(order, command, selected);
        List<ProcessAiPackagingCandidate> packaging = selectedPackaging(command);
        applyServiceSteps(command.orderUuid(), packaging);
        updateOrder(order, command);
        versionGuard.advance(command.orderUuid(), command.expectedVersion());
        if (!packaging.isEmpty()) processOrderService.calcFee(command.orderUuid());
        return new ProcessAiDraftApplyResult(
                command.expectedVersion() + 1, appliedPlans, packaging);
    }

    private void updateOrder(ProcessOrder order, ProcessAiDraftApplyCommand command) {
        LambdaUpdateWrapper<ProcessOrder> update = new LambdaUpdateWrapper<ProcessOrder>()
                .eq(ProcessOrder::getUuid, order.getUuid())
                .eq(ProcessOrder::getVersion, command.expectedVersion())
                .set(ProcessOrder::getAiRequirementJson, command.aiRequirementJson());
        if (command.finalCustomerRequirement() != null
                && !command.finalCustomerRequirement().isBlank()) {
            if (command.finalCustomerRequirement().length() > 2_000) {
                throw new BusinessException("确认后的客户加工要求不能超过2000字");
            }
            update.set(ProcessOrder::getRemarkLong, command.finalCustomerRequirement());
        }
        ConcurrencyGuard.requireRowUpdated(orderMapper.update(null, update));
    }

    private ProcessOrder requireDraft(String orderUuid, int expectedVersion) {
        ProcessOrder order = orderMapper.selectById(orderUuid);
        if (order == null) throw new BusinessException(ErrorCode.E002, "加工单不存在");
        if (!Integer.valueOf(DRAFT_STATUS).equals(order.getOrderStatus())) {
            throw new BusinessException(ErrorCode.E001, "只有草稿加工单可应用AI方案");
        }
        versionGuard.assertExpected(order, expectedVersion);
        versionGuard.assertLockedExpected(orderUuid, expectedVersion);
        return order;
    }

    private List<ProcessAiCompiledPlan> selectedPlans(ProcessAiDraftApplyCommand command) {
        Set<String> accepted = Set.copyOf(command.acceptedFieldPaths());
        return command.compilation().plans().stream()
                .filter(plan -> accepted.stream().anyMatch(path ->
                        path.startsWith("/assignments/" + plan.ownerRollRef() + "/")
                                && !path.contains("/ancillaryRequirements/")))
                .toList();
    }

    private List<DraftRollProcessDTO> selectedConfigurations(ProcessAiDraftApplyCommand command) {
        Set<String> accepted = Set.copyOf(command.acceptedFieldPaths());
        Map<String, OriginalRoll> currentRolls = currentRolls(command.compilation());
        return command.compilation().rollConfigurations().stream()
                .filter(configuration -> hasSelectedConfiguration(configuration, accepted))
                .flatMap(configuration -> configuration.originalUuids().stream().map(originalUuid -> {
                    OriginalRoll current = requireCurrentRoll(currentRolls, originalUuid);
                    DraftRollProcessDTO dto = new DraftRollProcessDTO();
                    dto.setOriginalUuid(originalUuid);
                    dto.setProcessMode(processModeFor(configuration, accepted, current));
                    dto.setMainStepType(mainStepFor(configuration, accepted, current));
                    dto.setMachineUuid(machineFor(configuration, originalUuid, accepted, currentRolls,
                            command.compilation()));
                    return dto;
                }))
                .toList();
    }

    private boolean hasSelectedConfiguration(ProcessAiRollConfiguration configuration,
                                              Set<String> accepted) {
        String base = "/assignments/" + configuration.ownerRollRef() + "/";
        return Set.of("processType", "processMode", "machineUuid")
                .stream().map(base::concat).anyMatch(accepted::contains);
    }

    private Map<String, OriginalRoll> currentRolls(ProcessAiCompilationResult compilation) {
        List<String> ids = compilation.rollConfigurations().stream()
                .flatMap(configuration -> configuration.originalUuids().stream())
                .distinct().toList();
        Map<String, OriginalRoll> rollsByUuid = new LinkedHashMap<>();
        if (ids.isEmpty()) return rollsByUuid;
        List<OriginalRoll> rolls = rollMapper.selectBatchIds(ids);
        if (rolls == null) return rollsByUuid;
        rolls.forEach(roll -> rollsByUuid.put(roll.getUuid(), roll));
        return rollsByUuid;
    }

    private Integer processModeFor(ProcessAiRollConfiguration configuration, Set<String> accepted,
                                   OriginalRoll current) {
        return accepted.contains(path(configuration, "processMode"))
                ? configuration.processMode() : current.getProcessMode();
    }

    private Integer mainStepFor(ProcessAiRollConfiguration configuration, Set<String> accepted,
                                OriginalRoll current) {
        return accepted.contains(path(configuration, "processType"))
                ? configuration.mainStepType() : current.getMainStepType();
    }

    private String machineFor(ProcessAiRollConfiguration configuration, String originalUuid,
                               Set<String> accepted, Map<String, OriginalRoll> currentRolls,
                               ProcessAiCompilationResult compilation) {
        String path = path(configuration, "machineUuid");
        OriginalRoll current = currentRolls.get(originalUuid);
        if (accepted.contains(path)) {
            return compilation.plans().stream()
                    .filter(plan -> configuration.ownerRollRef().equals(plan.ownerRollRef()))
                    .map(ProcessAiCompiledPlan::plan)
                    .filter(plan -> plan != null && plan.getMachineUuid() != null
                            && !plan.getMachineUuid().isBlank())
                    .map(com.paper.mes.processorder.dto.ProcessPlanDTO::getMachineUuid)
                    .findFirst()
                    .orElse(current.getMachineUuid());
        }
        // Machine selection is a separate AI field.  A plan that does not accept it
        // must retain the manually selected machine already stored on the roll.
        return current.getMachineUuid();
    }

    private String path(ProcessAiRollConfiguration configuration, String field) {
        return "/assignments/" + configuration.ownerRollRef() + "/" + field;
    }

    private OriginalRoll requireCurrentRoll(Map<String, OriginalRoll> currentRolls,
                                             String originalUuid) {
        OriginalRoll roll = currentRolls.get(originalUuid);
        if (roll == null) throw new BusinessException(ErrorCode.E002, "AI方案引用的母卷不存在");
        return roll;
    }

    private List<ProcessAiPackagingCandidate> selectedPackaging(ProcessAiDraftApplyCommand command) {
        Set<String> accepted = Set.copyOf(command.acceptedFieldPaths());
        return command.compilation().packagingCandidates().stream()
                .filter(item -> accepted.contains("/assignments/" + item.ownerRollRef()
                        + "/ancillaryRequirements/packaging"))
                .toList();
    }

    private void applyServiceSteps(String orderUuid, List<ProcessAiPackagingCandidate> candidates) {
        if (candidates.isEmpty()) return;
        Map<String, OriginalRoll> rolls = loadServiceRolls(orderUuid, candidates);
        List<ProcessStepDTO> requests = candidates.stream()
                .map(candidate -> serviceStepRequestFactory.create(candidate,
                        requireServiceRoll(rolls, candidate.originalUuid())))
                .toList();
        serviceStepWriter.upsert(orderUuid, requests, rolls);
    }

    private Map<String, OriginalRoll> loadServiceRolls(String orderUuid,
                                                        List<ProcessAiPackagingCandidate> candidates) {
        List<String> ids = candidates.stream().map(ProcessAiPackagingCandidate::originalUuid).toList();
        Map<String, OriginalRoll> rolls = new LinkedHashMap<>();
        rollMapper.selectBatchIds(ids).forEach(roll -> {
            if (orderUuid.equals(roll.getOrderUuid())) rolls.put(roll.getUuid(), roll);
        });
        return rolls;
    }

    private OriginalRoll requireServiceRoll(Map<String, OriginalRoll> rolls, String originalUuid) {
        OriginalRoll roll = rolls.get(originalUuid);
        if (roll == null) throw new BusinessException(ErrorCode.E002, "附加工艺引用的母卷不存在");
        if (!ProcessModePolicy.supportsServiceSteps(roll.getProcessMode())) {
            throw new BusinessException("当前加工方式不允许附加工艺");
        }
        return roll;
    }
}
