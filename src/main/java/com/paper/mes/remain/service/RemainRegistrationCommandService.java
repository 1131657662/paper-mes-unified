package com.paper.mes.remain.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.remain.dto.RemainRegistrationCreateDTO;
import com.paper.mes.remain.dto.RemainRegistrationLineDTO;
import com.paper.mes.remain.entity.RemainInventoryLedger;
import com.paper.mes.remain.entity.RemainInventoryLot;
import com.paper.mes.remain.entity.RemainRegistration;
import com.paper.mes.remain.entity.RemainRegistrationLine;
import com.paper.mes.remain.mapper.RemainInventoryLedgerMapper;
import com.paper.mes.remain.mapper.RemainInventoryLotMapper;
import com.paper.mes.remain.mapper.RemainRegistrationLineMapper;
import com.paper.mes.remain.mapper.RemainRegistrationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RemainRegistrationCommandService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(3);
    private final RemainRegistrationMapper registrationMapper;
    private final RemainRegistrationLineMapper lineMapper;
    private final RemainInventoryLotMapper lotMapper;
    private final RemainInventoryLedgerMapper ledgerMapper;
    private final RemainSourceValidator sourceValidator;
    private final FinishRollMapper finishRollMapper;

    @Transactional(rollbackFor = Exception.class)
    public RemainRegistration create(RemainRegistrationCreateDTO request) {
        String hash = RemainRequestFingerprint.registration(request);
        RemainRegistration existing = registrationMapper.selectOne(new LambdaQueryWrapper<RemainRegistration>()
                .eq(RemainRegistration::getRequestId, request.getRequestId()));
        if (existing != null) {
            if (!hash.equals(existing.getRequestHash())) {
                throw new BusinessException("相同请求号的登记载荷不一致");
            }
            return existing;
        }
        RemainSourceValidator.SourceContext context = sourceValidator
                .lockAndValidate(request.getOrderUuid(), request.getLines());
        BigDecimal total = request.getLines().stream()
                .map(RemainRegistrationLineDTO::getTransferredSystemWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        RemainRegistration registration = newRegistration(request, context.order().getCustomerUuid(), hash, total);
        registrationMapper.insert(registration);
        for (RemainRegistrationLineDTO lineRequest : request.getLines()) {
            writeLine(registration, lineRequest, context.rolls().get(lineRequest.getSourceFinishRollUuid()));
        }
        return registration;
    }

    private RemainRegistration newRegistration(RemainRegistrationCreateDTO request,
                                                String customerUuid, String hash, BigDecimal total) {
        String uuid = UUID.randomUUID().toString();
        RemainRegistration result = new RemainRegistration();
        result.setUuid(uuid);
        result.setRegistrationNo("REG-" + uuid.replace("-", "").substring(0, 16));
        result.setRequestId(request.getRequestId().trim());
        result.setRequestHash(hash);
        result.setOrderUuid(request.getOrderUuid());
        result.setCustomerUuid(customerUuid);
        result.setRegistrationDate(LocalDateTime.now());
        result.setConfirmationName(request.getConfirmationName().trim());
        result.setConfirmationChannel(request.getConfirmationChannel().trim());
        result.setConfirmationAt(request.getConfirmationAt());
        result.setConfirmationEvidence(request.getConfirmationEvidence().trim());
        result.setStatus("ACTIVE");
        result.setPriceStatus("PRICE_PENDING");
        result.setTotalTransferredWeight(total);
        result.setTotalRolledBackWeight(ZERO);
        result.setTotalProcessedWeight(ZERO);
        result.setTotalAmount(BigDecimal.ZERO);
        result.setRemark(request.getRemark());
        result.setIsDeleted(0);
        result.setVersion(1);
        return result;
    }

    private void writeLine(RemainRegistration registration, RemainRegistrationLineDTO request,
                           FinishRoll roll) {
        BigDecimal transfer = request.getTransferredSystemWeight();
        BigDecimal available = RemainSourceValidator.availableWeight(roll);
        BigDecimal ownBefore = value(roll.getRemainOwnWeight());
        BigDecimal ownAfter = ownBefore.add(transfer);
        BigDecimal customerAfter = available.subtract(transfer);
        RemainRegistrationLine line = newLine(registration, roll, available, transfer);
        lineMapper.insert(line);
        RemainInventoryLot lot = newLot(line, roll, transfer);
        lotMapper.insert(lot);
        ledgerMapper.insert(newLedger(line, lot, transfer,
                registration.getRequestId(), AuthContextHolder.currentDisplayName()));
        roll.setRemainingWeight(customerAfter);
        roll.setRemainOwnWeight(ownAfter);
        roll.setOwnershipStatus(customerAfter.signum() == 0 ? 2 : 1);
        roll.setRemainTransferState(customerAfter.signum() == 0 ? 2 : 1);
        ConcurrencyGuard.requireRowUpdated(finishRollMapper.updateById(roll));
    }

    private RemainRegistrationLine newLine(RemainRegistration registration, FinishRoll roll,
                                           BigDecimal available, BigDecimal transfer) {
        RemainRegistrationLine line = new RemainRegistrationLine();
        line.setUuid(UUID.randomUUID().toString());
        line.setRegistrationUuid(registration.getUuid());
        line.setSourceFinishRollUuid(roll.getUuid());
        line.setSourceOrderUuid(roll.getOrderUuid());
        line.setSourceCustomerUuid(registration.getCustomerUuid());
        line.setSourceSystemWeight(available);
        line.setTransferredSystemWeight(transfer);
        line.setRolledBackSystemWeight(ZERO);
        line.setProcessedSystemWeight(ZERO);
        line.setCurrentOwnWeight(transfer);
        line.setAmount(BigDecimal.ZERO);
        line.setStatus("ACTIVE");
        line.setIsDeleted(0);
        line.setVersion(1);
        return line;
    }

    private RemainInventoryLot newLot(RemainRegistrationLine line, FinishRoll roll, BigDecimal weight) {
        RemainInventoryLot lot = new RemainInventoryLot();
        lot.setUuid(UUID.randomUUID().toString());
        lot.setRegistrationLineUuid(line.getUuid());
        lot.setSourceFinishRollUuid(roll.getUuid());
        lot.setCustomerUuid(line.getSourceCustomerUuid());
        lot.setWarehouseUuid(roll.getWarehouseUuid());
        lot.setCurrentWeight(weight);
        lot.setStatus("IN_OWN_STOCK");
        lot.setPriceStatus("PRICE_PENDING");
        lot.setIsDeleted(0);
        lot.setVersion(1);
        return lot;
    }

    private RemainInventoryLedger newLedger(RemainRegistrationLine line, RemainInventoryLot lot,
                                            BigDecimal weight, String requestId, String operator) {
        RemainInventoryLedger ledger = new RemainInventoryLedger();
        ledger.setUuid(UUID.randomUUID().toString());
        ledger.setLotUuid(lot.getUuid());
        ledger.setRegistrationLineUuid(line.getUuid());
        ledger.setSourceFinishRollUuid(line.getSourceFinishRollUuid());
        ledger.setEventType("TRANSFER_IN");
        ledger.setWeightDelta(weight);
        ledger.setBeforeWeight(ZERO);
        ledger.setAfterWeight(weight);
        ledger.setRequestId(RemainRequestFingerprint.ledgerRequest(requestId, line.getUuid(), "TRANSFER_IN"));
        ledger.setCreateBy(operator);
        return ledger;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? ZERO : value;
    }
}
