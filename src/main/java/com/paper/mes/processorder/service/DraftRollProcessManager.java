package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.processorder.calc.FeeCalculator;
import com.paper.mes.processorder.dto.DraftRollProcessBatchSaveDTO;
import com.paper.mes.processorder.dto.DraftRollProcessDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessConfigDraft;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import com.paper.mes.processorder.mapper.ProcessConfigDraftMapper;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/** 原纸写入与工艺方案写入之间的草稿加工方式保存边界。 */
@Component
@RequiredArgsConstructor
public class DraftRollProcessManager {

    private static final int STATUS_DRAFT = 0;

    private final ProcessOrderMapper orderMapper;
    private final OriginalRollMapper rollMapper;
    private final ProcessConfigDraftMapper draftMapper;
    private final ProcessStepMapper processStepMapper;
    private final BusinessLockService businessLockService;
    private final DraftOrderVersionGuard versionGuard;

    @Transactional(rollbackFor = Exception.class)
    public void save(String orderUuid, DraftRollProcessBatchSaveDTO dto) {
        businessLockService.lockProcessOrders(List.of(orderUuid));
        ProcessOrder order = requireDraft(orderUuid);
        versionGuard.assertExpected(order, dto.getExpectedVersion());
        versionGuard.assertLockedExpected(orderUuid, dto.getExpectedVersion());
        Map<String, OriginalRoll> rolls = requireRolls(orderUuid, dto.getRolls());
        versionGuard.advance(orderUuid, dto.getExpectedVersion());
        for (DraftRollProcessDTO item : dto.getRolls()) {
            updateRoll(rolls.get(item.getOriginalUuid()), item);
        }
    }

    /**
     * Applies validated Step 3 selections while the caller already holds the order lock and
     * owns the draft-version transition. AI confirmation uses this to share manual-save rules
     * without advancing the draft version more than once.
     */
    public void applyLocked(ProcessOrder order, List<DraftRollProcessDTO> items) {
        if (items.isEmpty()) return;
        Map<String, OriginalRoll> rolls = requireRolls(order.getUuid(), items);
        for (DraftRollProcessDTO item : items) {
            updateRoll(rolls.get(item.getOriginalUuid()), item);
        }
    }

    private ProcessOrder requireDraft(String orderUuid) {
        ProcessOrder order = orderMapper.selectById(orderUuid);
        if (order == null) {
            throw new BusinessException(ErrorCode.E002, "加工单不存在");
        }
        if (order.getOrderStatus() == null || order.getOrderStatus() != STATUS_DRAFT) {
            throw new BusinessException(ErrorCode.E001, "只有草稿加工单可编辑");
        }
        return order;
    }

    private Map<String, OriginalRoll> requireRolls(String orderUuid, List<DraftRollProcessDTO> items) {
        List<String> ids = items.stream().map(DraftRollProcessDTO::getOriginalUuid).toList();
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new BusinessException(ErrorCode.E003, "母卷工艺选择存在重复项");
        }
        Map<String, OriginalRoll> result = new HashMap<>();
        for (OriginalRoll roll : rollMapper.selectBatchIds(ids)) {
            if (orderUuid.equals(roll.getOrderUuid())) {
                result.put(roll.getUuid(), roll);
            }
        }
        if (result.size() != ids.size()) {
            throw new BusinessException(ErrorCode.E002, "部分原纸明细不存在或不属于当前加工单");
        }
        return result;
    }

    private void updateRoll(OriginalRoll roll, DraftRollProcessDTO item) {
        ProcessModePolicy.requireValid(item.getProcessMode(), item.getMainStepType());
        if (processChanged(roll, item)) {
            draftMapper.delete(new LambdaQueryWrapper<ProcessConfigDraft>()
                    .eq(ProcessConfigDraft::getOrderUuid, roll.getOrderUuid())
                    .eq(ProcessConfigDraft::getOriginalUuid, roll.getUuid()));
        }
        if (ProcessModePolicy.isDirectShip(item.getProcessMode())) {
            processStepMapper.delete(new LambdaQueryWrapper<ProcessStep>()
                    .eq(ProcessStep::getOrderUuid, roll.getOrderUuid())
                    .eq(ProcessStep::getOriginalUuid, roll.getUuid())
                    .eq(ProcessStep::getIsMain, 0)
                    .in(ProcessStep::getStepType,
                            FeeCalculator.STEP_TYPE_STRIP_SORT,
                            FeeCalculator.STEP_TYPE_REPACKAGE));
        }
        roll.setProcessMode(item.getProcessMode());
        roll.setMainStepType(item.getMainStepType());
        roll.setMachineUuid(item.getMachineUuid());
        roll.setUpdateBy(null);
        roll.setUpdateTime(null);
        ConcurrencyGuard.requireRowUpdated(rollMapper.updateById(roll));
    }

    private boolean processChanged(OriginalRoll roll, DraftRollProcessDTO item) {
        return !java.util.Objects.equals(roll.getProcessMode(), item.getProcessMode())
                || !java.util.Objects.equals(roll.getMainStepType(), item.getMainStepType())
                || !java.util.Objects.equals(roll.getMachineUuid(), item.getMachineUuid());
    }

}
