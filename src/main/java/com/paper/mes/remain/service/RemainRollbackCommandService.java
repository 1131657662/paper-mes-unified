package com.paper.mes.remain.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.remain.dto.RemainRollbackDTO;
import com.paper.mes.remain.dto.RemainRollbackLineDTO;
import com.paper.mes.remain.entity.RemainInventoryLedger;
import com.paper.mes.remain.entity.RemainInventoryLot;
import com.paper.mes.remain.entity.RemainRegistration;
import com.paper.mes.remain.entity.RemainRegistrationLine;
import com.paper.mes.remain.mapper.RemainInventoryLedgerMapper;
import com.paper.mes.remain.mapper.RemainInventoryLotMapper;
import com.paper.mes.remain.mapper.RemainRegistrationLineMapper;
import com.paper.mes.remain.mapper.RemainRegistrationMapper;
import com.paper.mes.remain.mapper.RemainApplicationMapper;
import com.paper.mes.remain.mapper.RemainAdjustmentMapper;
import com.paper.mes.remain.entity.RemainAdjustment;
import com.paper.mes.remain.entity.RemainApplication;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RemainRollbackCommandService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(3);
    private final RemainRegistrationMapper registrationMapper;
    private final RemainRegistrationLineMapper lineMapper;
    private final RemainInventoryLotMapper lotMapper;
    private final RemainInventoryLedgerMapper ledgerMapper;
    private final FinishRollMapper finishRollMapper;
    private final BusinessLockService businessLockService;
    private final RemainLockService remainLockService;
    private final RemainApplicationMapper applicationMapper;
    private final RemainAdjustmentMapper adjustmentMapper;
    private final RemainRegistrationTotalsService registrationTotals;

    @Transactional(rollbackFor = Exception.class)
    public RemainRegistration rollback(String registrationUuid, RemainRollbackDTO request) {
        RemainRegistration registration = registrationMapper.selectById(registrationUuid);
        if (registration == null) {
            throw new BusinessException("登记单不存在");
        }
        businessLockService.lockProcessOrders(List.of(registration.getOrderUuid()));
        List<RemainRegistrationLine> allLines = lineMapper.selectList(new LambdaQueryWrapper<RemainRegistrationLine>()
                .eq(RemainRegistrationLine::getRegistrationUuid, registrationUuid));
        businessLockService.lockFinishRolls(allLines.stream()
                .map(RemainRegistrationLine::getSourceFinishRollUuid).toList());
        remainLockService.lockRegistration(registrationUuid);
        remainLockService.lockLines(allLines.stream().map(RemainRegistrationLine::getUuid).toList());
        registration = registrationMapper.selectById(registrationUuid);
        if (applicationMapper.selectCount(new LambdaQueryWrapper<RemainApplication>()
                .eq(RemainApplication::getRegistrationUuid, registrationUuid)
                .eq(RemainApplication::getApplicationType, "APPLY")
                .eq(RemainApplication::getStatus, "ACTIVE")) > 0) {
            throw new BusinessException("登记单已有有效财务分配，请先完成抵扣应用反向");
        }
        if (adjustmentMapper.selectCount(new LambdaQueryWrapper<RemainAdjustment>()
                .eq(RemainAdjustment::getRegistrationUuid, registrationUuid)
                .in(RemainAdjustment::getStatus, "PENDING", "APPLIED")) > 0) {
            throw new BusinessException("登记单已有待处理或有效结算调整，请先完成调整反向或取消");
        }
        allLines = lineMapper.selectList(new LambdaQueryWrapper<RemainRegistrationLine>()
                .eq(RemainRegistrationLine::getRegistrationUuid, registrationUuid));
        Map<String, RemainRegistrationLine> lines = allLines.stream()
                .collect(Collectors.toMap(RemainRegistrationLine::getUuid, Function.identity()));
        Map<String, FinishRoll> rolls = finishRollMapper.selectBatchIds(allLines.stream()
                        .map(RemainRegistrationLine::getSourceFinishRollUuid).toList()).stream()
                .collect(Collectors.toMap(FinishRoll::getUuid, Function.identity()));
        for (RemainRollbackLineDTO item : request.getLines()) {
            rollbackLine(registration, item, lines, rolls, request);
        }
        registrationTotals.refresh(List.of(registrationUuid));
        return registrationMapper.selectById(registrationUuid);
    }

    private void rollbackLine(RemainRegistration registration, RemainRollbackLineDTO request,
                              Map<String, RemainRegistrationLine> lines, Map<String, FinishRoll> rolls,
                              RemainRollbackDTO command) {
        RemainRegistrationLine line = lines.get(request.getRegistrationLineUuid());
        if (line == null) {
            throw new BusinessException("回滚明细不属于当前登记单");
        }
        BigDecimal requested = request.getRollbackWeight() == null
                ? line.getCurrentOwnWeight() : request.getRollbackWeight();
        BigDecimal current = value(line.getCurrentOwnWeight());
        if (requested.signum() <= 0 || requested.compareTo(current) > 0) {
            throw new BusinessException("回滚重量超过当前我方库存");
        }
        String ledgerRequest = RemainRequestFingerprint.ledgerRequest(
                command.getRequestId() + "|" + requested.toPlainString(), line.getUuid(), "ROLLBACK");
        RemainInventoryLedger repeated = ledgerMapper.selectOne(new LambdaQueryWrapper<RemainInventoryLedger>()
                .eq(RemainInventoryLedger::getRequestId, ledgerRequest));
        if (repeated != null) {
            return;
        }
        RemainInventoryLot lot = lotMapper.selectOne(new LambdaQueryWrapper<RemainInventoryLot>()
                .eq(RemainInventoryLot::getRegistrationLineUuid, line.getUuid()));
        if (lot == null) {
            throw new BusinessException("我方库存批次不存在");
        }
        FinishRoll roll = rolls.get(line.getSourceFinishRollUuid());
        BigDecimal customerBefore = RemainSourceValidator.availableWeight(roll);
        BigDecimal ownAfter = current.subtract(requested);
        line.setCurrentOwnWeight(ownAfter);
        line.setRolledBackSystemWeight(value(line.getRolledBackSystemWeight()).add(requested));
        line.setStatus(ownAfter.signum() == 0 ? "FULL_ROLLED_BACK" : "PARTIAL_ROLLED_BACK");
        ConcurrencyGuard.requireRowUpdated(lineMapper.updateById(line));
        lot.setCurrentWeight(value(lot.getCurrentWeight()).subtract(requested));
        lot.setStatus(lot.getCurrentWeight().signum() == 0 ? "EMPTY" : "IN_OWN_STOCK");
        ConcurrencyGuard.requireRowUpdated(lotMapper.updateById(lot));
        ledgerMapper.insert(newLedger(line, lot, current, ownAfter, requested, ledgerRequest, command.getReason()));
        roll.setRemainingWeight(customerBefore.add(requested));
        roll.setRemainOwnWeight(value(roll.getRemainOwnWeight()).subtract(requested));
        roll.setOwnershipStatus(roll.getRemainOwnWeight().signum() == 0 ? 0 : 1);
        roll.setRemainTransferState(3);
        ConcurrencyGuard.requireRowUpdated(finishRollMapper.updateById(roll));
        registration.setTotalRolledBackWeight(value(registration.getTotalRolledBackWeight()).add(requested));
    }

    private RemainInventoryLedger newLedger(RemainRegistrationLine line, RemainInventoryLot lot,
                                            BigDecimal before, BigDecimal after, BigDecimal weight,
                                            String requestId, String reason) {
        RemainInventoryLedger ledger = new RemainInventoryLedger();
        ledger.setUuid(UUID.randomUUID().toString());
        ledger.setLotUuid(lot.getUuid());
        ledger.setRegistrationLineUuid(line.getUuid());
        ledger.setSourceFinishRollUuid(line.getSourceFinishRollUuid());
        ledger.setEventType("ROLLBACK");
        ledger.setWeightDelta(weight.negate());
        ledger.setBeforeWeight(before);
        ledger.setAfterWeight(after);
        ledger.setRequestId(requestId);
        ledger.setReason(reason);
        ledger.setCreateBy(AuthContextHolder.currentDisplayName());
        return ledger;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? ZERO : value;
    }
}
