package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.paper.mes.ai.process.compile.ProcessAiCompiledPlan;
import com.paper.mes.ai.process.compile.ProcessAiPackagingCandidate;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    @Transactional(rollbackFor = Exception.class)
    public ProcessAiDraftApplyResult apply(ProcessAiDraftApplyCommand command) {
        lockService.lockProcessOrders(List.of(command.orderUuid()));
        ProcessOrder order = requireDraft(command.orderUuid(), command.expectedVersion());
        List<ProcessAiCompiledPlan> selected = selectedPlans(command);
        Map<String, ProcessAiCompiledPlan> appliedPlans = planApplier.apply(order, command, selected);
        List<ProcessAiPackagingCandidate> packaging = selectedPackaging(command);
        updateOrder(order, command);
        versionGuard.advance(command.orderUuid(), command.expectedVersion());
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

    private List<ProcessAiPackagingCandidate> selectedPackaging(ProcessAiDraftApplyCommand command) {
        Set<String> accepted = Set.copyOf(command.acceptedFieldPaths());
        return command.compilation().packagingCandidates().stream()
                .filter(item -> accepted.contains("/assignments/" + item.ownerRollRef()
                        + "/ancillaryRequirements/packaging"))
                .toList();
    }
}
