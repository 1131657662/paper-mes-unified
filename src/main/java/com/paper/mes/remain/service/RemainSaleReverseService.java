package com.paper.mes.remain.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.remain.dto.RemainSaleReverseDTO;
import com.paper.mes.remain.entity.RemainInventoryLedger;
import com.paper.mes.remain.entity.RemainInventoryLot;
import com.paper.mes.remain.entity.RemainRegistrationLine;
import com.paper.mes.remain.entity.RemainSale;
import com.paper.mes.remain.entity.RemainSaleLine;
import com.paper.mes.remain.mapper.RemainInventoryLedgerMapper;
import com.paper.mes.remain.mapper.RemainInventoryLotMapper;
import com.paper.mes.remain.mapper.RemainRegistrationLineMapper;
import com.paper.mes.remain.mapper.RemainSaleLineMapper;
import com.paper.mes.remain.mapper.RemainSaleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RemainSaleReverseService {
    private final RemainSaleMapper saleMapper;
    private final RemainSaleLineMapper saleLineMapper;
    private final RemainInventoryLotMapper lotMapper;
    private final RemainInventoryLedgerMapper ledgerMapper;
    private final RemainRegistrationLineMapper registrationLineMapper;
    private final RemainLockService lockService;
    private final RemainRegistrationTotalsService registrationTotals;

    @Transactional(rollbackFor = Exception.class)
    public RemainSale reverse(String saleUuid, RemainSaleReverseDTO request) {
        RemainSale replay = saleMapper.selectOne(new LambdaQueryWrapper<RemainSale>()
                .eq(RemainSale::getRequestId, request.getRequestId()));
        if (replay != null) {
            return replay;
        }
        RemainSale original = saleMapper.selectById(saleUuid);
        if (original == null || !"SALE".equals(original.getSaleKind())
                || !"CONFIRMED".equals(original.getStatus())) {
            throw new BusinessException("处理单不存在或已撤销");
        }
        List<RemainSaleLine> sourceLines = saleLineMapper.selectList(new LambdaQueryWrapper<RemainSaleLine>()
                .eq(RemainSaleLine::getSaleUuid, saleUuid));
        Map<String, RemainInventoryLot> lots = loadLots(sourceLines);
        Map<String, RemainRegistrationLine> registrationLines = loadRegistrationLines(lots);
        lockService.lockLots(lots.keySet());
        lockService.lockLines(registrationLines.keySet());
        sourceLines = saleLineMapper.selectList(new LambdaQueryWrapper<RemainSaleLine>()
                .eq(RemainSaleLine::getSaleUuid, saleUuid));
        lots = loadLots(sourceLines);
        registrationLines = loadRegistrationLines(lots);
        validateNoLaterSale(original, sourceLines);
        RemainSale reversal = newReversal(original, request);
        saleMapper.insert(reversal);
        for (RemainSaleLine source : sourceLines) {
            restoreLine(reversal, original, source, lots.get(source.getLotUuid()),
                    registrationLines.get(source.getRegistrationLineUuid()));
        }
        registrationTotals.refresh(registrationLines.values().stream()
                .map(RemainRegistrationLine::getRegistrationUuid).toList());
        original.setStatus("VOIDED");
        original.setReason(request.getReason());
        ConcurrencyGuard.requireRowUpdated(saleMapper.updateById(original));
        return reversal;
    }

    private Map<String, RemainInventoryLot> loadLots(List<RemainSaleLine> sourceLines) {
        return lotMapper.selectBatchIds(sourceLines.stream().map(RemainSaleLine::getLotUuid).toList())
                .stream().collect(Collectors.toMap(RemainInventoryLot::getUuid, Function.identity()));
    }

    private Map<String, RemainRegistrationLine> loadRegistrationLines(Map<String, RemainInventoryLot> lots) {
        return registrationLineMapper.selectBatchIds(lots.values().stream()
                        .map(RemainInventoryLot::getRegistrationLineUuid).toList())
                .stream().collect(Collectors.toMap(RemainRegistrationLine::getUuid, Function.identity()));
    }

    private void validateNoLaterSale(RemainSale original, List<RemainSaleLine> sourceLines) {
        List<String> saleIds = saleLineMapper.selectList(new LambdaQueryWrapper<RemainSaleLine>()
                        .in(RemainSaleLine::getLotUuid, sourceLines.stream().map(RemainSaleLine::getLotUuid).toList()))
                .stream().map(RemainSaleLine::getSaleUuid).distinct().toList();
        if (saleIds.isEmpty()) {
            return;
        }
        boolean later = saleMapper.selectList(new LambdaQueryWrapper<RemainSale>()
                        .in(RemainSale::getUuid, saleIds)
                        .eq(RemainSale::getSaleKind, "SALE")
                        .eq(RemainSale::getStatus, "CONFIRMED")
                        .gt(RemainSale::getCreateTime, original.getCreateTime())).stream().findAny().isPresent();
        if (later) {
            throw new BusinessException("处理单之后已有同批次处理记录，不能直接撤销");
        }
    }

    private RemainSale newReversal(RemainSale original, RemainSaleReverseDTO request) {
        RemainSale result = new RemainSale();
        result.setUuid(UUID.randomUUID().toString());
        result.setSaleNo("REV-" + result.getUuid().replace("-", "").substring(0, 16));
        result.setRequestId(request.getRequestId().trim());
        result.setRequestHash(original.getUuid() + "|" + request.getReason());
        result.setSaleKind("REVERSAL");
        result.setReversalOfUuid(original.getUuid());
        result.setProcessDate(LocalDateTime.now());
        result.setCreateTime(LocalDateTime.now());
        result.setWarehouseUuid(original.getWarehouseUuid());
        result.setPricingMode(original.getPricingMode());
        result.setSystemWeight(original.getSystemWeight());
        result.setActualWeight(original.getActualWeight());
        result.setUnitPrice(original.getUnitPrice());
        result.setCalculatedAmount(original.getCalculatedAmount());
        result.setReceivedAmount(original.getReceivedAmount());
        result.setStatus("CONFIRMED");
        result.setReason(request.getReason());
        result.setCreateBy(AuthContextHolder.currentDisplayName());
        result.setVersion(1);
        return result;
    }

    private void restoreLine(RemainSale reversal, RemainSale original, RemainSaleLine source,
                             RemainInventoryLot lot, RemainRegistrationLine line) {
        if (lot == null || line == null) {
            throw new BusinessException("处理来源库存不存在");
        }
        BigDecimal weight = source.getSystemWeight();
        BigDecimal max = value(line.getTransferredSystemWeight()).subtract(value(line.getRolledBackSystemWeight()));
        BigDecimal after = value(lot.getCurrentWeight()).add(weight);
        if (after.compareTo(max) > 0) {
            throw new BusinessException("撤销后库存重量超过登记明细可用重量");
        }
        lot.setCurrentWeight(after);
        lot.setStatus("IN_OWN_STOCK");
        ConcurrencyGuard.requireRowUpdated(lotMapper.updateById(lot));
        line.setCurrentOwnWeight(value(line.getCurrentOwnWeight()).add(weight));
        line.setProcessedSystemWeight(value(line.getProcessedSystemWeight()).subtract(weight).max(BigDecimal.ZERO));
        ConcurrencyGuard.requireRowUpdated(registrationLineMapper.updateById(line));
        RemainSaleLine reversalLine = new RemainSaleLine();
        reversalLine.setUuid(UUID.randomUUID().toString());
        reversalLine.setSaleUuid(reversal.getUuid());
        reversalLine.setLotUuid(lot.getUuid());
        reversalLine.setRegistrationLineUuid(line.getUuid());
        reversalLine.setSystemWeight(weight);
        reversalLine.setAmount(source.getAmount());
        reversalLine.setCreateTime(LocalDateTime.now());
        saleLineMapper.insert(reversalLine);
        RemainInventoryLedger sourceLedger = ledgerMapper.selectOne(new LambdaQueryWrapper<RemainInventoryLedger>()
                .eq(RemainInventoryLedger::getRequestId,
                        RemainRequestFingerprint.ledgerRequest(original.getRequestId(), line.getUuid(), "SALE_OUT")));
        RemainInventoryLedger ledger = new RemainInventoryLedger();
        ledger.setUuid(UUID.randomUUID().toString());
        ledger.setLotUuid(lot.getUuid());
        ledger.setRegistrationLineUuid(line.getUuid());
        ledger.setSourceFinishRollUuid(line.getSourceFinishRollUuid());
        ledger.setEventType("SALE_REVERSAL");
        ledger.setWeightDelta(weight);
        ledger.setBeforeWeight(after.subtract(weight));
        ledger.setAfterWeight(after);
        ledger.setRequestId(RemainRequestFingerprint.ledgerRequest(reversal.getRequestId(), line.getUuid(), "SALE_REVERSAL"));
        ledger.setReversalOfUuid(sourceLedger == null ? null : sourceLedger.getUuid());
        ledger.setReason(reversal.getReason());
        ledger.setCreateBy(AuthContextHolder.currentDisplayName());
        ledgerMapper.insert(ledger);
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
