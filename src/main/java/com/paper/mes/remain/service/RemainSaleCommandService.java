package com.paper.mes.remain.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.remain.dto.RemainSaleCreateDTO;
import com.paper.mes.remain.dto.RemainSaleLineDTO;
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
public class RemainSaleCommandService {
    private final RemainSaleMapper saleMapper;
    private final RemainSaleLineMapper saleLineMapper;
    private final RemainInventoryLotMapper lotMapper;
    private final RemainInventoryLedgerMapper ledgerMapper;
    private final RemainRegistrationLineMapper registrationLineMapper;
    private final RemainLockService lockService;
    private final RemainRegistrationTotalsService registrationTotals;

    @Transactional(rollbackFor = Exception.class)
    public RemainSale create(RemainSaleCreateDTO request) {
        String hash = RemainRequestFingerprint.sale(request);
        RemainSale replay = saleMapper.selectOne(new LambdaQueryWrapper<RemainSale>()
                .eq(RemainSale::getRequestId, request.getRequestId()));
        if (replay != null) {
            if (!hash.equals(replay.getRequestHash())) {
                throw new BusinessException("相同请求号的处理载荷不一致");
            }
            return replay;
        }
        RemainSalePolicy.validateRequest(request);
        Map<String, RemainInventoryLot> lots = loadLots(request);
        Map<String, RemainRegistrationLine> lines = loadLines(lots);
        lockService.lockLots(lots.keySet());
        lockService.lockLines(lines.keySet());
        lots = loadLots(request);
        lines = loadLines(lots);
        BigDecimal totalWeight = validateLots(request, lots, lines);
        BigDecimal amount = RemainSalePolicy.calculateAmount(request, totalWeight);
        RemainSale sale = newSale(request, hash, totalWeight, amount);
        saleMapper.insert(sale);
        writeLines(sale, request, lots, lines, totalWeight, amount);
        registrationTotals.refresh(lines.values().stream()
                .map(RemainRegistrationLine::getRegistrationUuid).toList());
        return sale;
    }

    private Map<String, RemainInventoryLot> loadLots(RemainSaleCreateDTO request) {
        return lotMapper.selectBatchIds(request.getLines().stream().map(RemainSaleLineDTO::getLotUuid).toList())
                .stream().collect(Collectors.toMap(RemainInventoryLot::getUuid, Function.identity()));
    }

    private Map<String, RemainRegistrationLine> loadLines(Map<String, RemainInventoryLot> lots) {
        return registrationLineMapper.selectBatchIds(lots.values().stream()
                        .map(RemainInventoryLot::getRegistrationLineUuid).toList())
                .stream().collect(Collectors.toMap(RemainRegistrationLine::getUuid, Function.identity()));
    }

    private BigDecimal validateLots(RemainSaleCreateDTO request, Map<String, RemainInventoryLot> lots,
                                    Map<String, RemainRegistrationLine> lines) {
        BigDecimal total = BigDecimal.ZERO;
        for (RemainSaleLineDTO item : request.getLines()) {
            RemainInventoryLot lot = lots.get(item.getLotUuid());
            if (lot == null || !"IN_OWN_STOCK".equals(lot.getStatus())) {
                throw new BusinessException("库存批次不存在或不可处理");
            }
            if (request.getWarehouseUuid() != null && !request.getWarehouseUuid().equals(lot.getWarehouseUuid())) {
                throw new BusinessException("处理单包含不同仓库批次");
            }
            RemainRegistrationLine line = lines.get(lot.getRegistrationLineUuid());
            if (line == null || item.getSystemWeight().compareTo(value(lot.getCurrentWeight())) > 0
                    || item.getSystemWeight().compareTo(value(line.getCurrentOwnWeight())) > 0) {
                throw new BusinessException("处理重量超过当前我方库存");
            }
            total = total.add(item.getSystemWeight());
        }
        return total;
    }

    private RemainSale newSale(RemainSaleCreateDTO request, String hash, BigDecimal weight,
                               BigDecimal amount) {
        RemainSale sale = new RemainSale();
        sale.setUuid(UUID.randomUUID().toString());
        sale.setSaleNo("SALE-" + sale.getUuid().replace("-", "").substring(0, 16));
        sale.setRequestId(request.getRequestId().trim());
        sale.setRequestHash(hash);
        sale.setSaleKind("SALE");
        sale.setCreateTime(LocalDateTime.now());
        sale.setProcessDate(request.getProcessDate());
        sale.setWarehouseUuid(request.getWarehouseUuid());
        sale.setPricingMode(request.getPricingMode());
        sale.setSystemWeight(weight);
        sale.setActualWeight(request.getActualWeight());
        sale.setUnitPrice(request.getUnitPrice());
        sale.setCalculatedAmount(amount);
        sale.setReceivedAmount(request.getReceivedAmount());
        sale.setBuyerName(request.getBuyerName());
        sale.setVehicleNo(request.getVehicleNo());
        sale.setWeighingTicketNo(request.getWeighingTicketNo());
        sale.setWeighingEvidence(request.getWeighingEvidence());
        sale.setStatus("CONFIRMED");
        sale.setReason(request.getReason());
        sale.setCreateBy(AuthContextHolder.currentDisplayName());
        sale.setVersion(1);
        return sale;
    }

    private void writeLines(RemainSale sale, RemainSaleCreateDTO request,
                            Map<String, RemainInventoryLot> lots,
                            Map<String, RemainRegistrationLine> lines,
                            BigDecimal totalWeight, BigDecimal totalAmount) {
        List<BigDecimal> amounts = RemainSaleAllocation.amounts(request.getLines(), totalWeight, totalAmount);
        for (int i = 0; i < request.getLines().size(); i++) {
            RemainSaleLineDTO item = request.getLines().get(i);
            RemainInventoryLot lot = lots.get(item.getLotUuid());
            RemainRegistrationLine line = lines.get(lot.getRegistrationLineUuid());
            BigDecimal before = value(lot.getCurrentWeight());
            BigDecimal after = before.subtract(item.getSystemWeight());
            lot.setCurrentWeight(after);
            lot.setStatus(after.signum() == 0 ? "EMPTY" : "IN_OWN_STOCK");
            ConcurrencyGuard.requireRowUpdated(lotMapper.updateById(lot));
            line.setCurrentOwnWeight(value(line.getCurrentOwnWeight()).subtract(item.getSystemWeight()));
            line.setProcessedSystemWeight(value(line.getProcessedSystemWeight()).add(item.getSystemWeight()));
            ConcurrencyGuard.requireRowUpdated(registrationLineMapper.updateById(line));
            RemainSaleLine saleLine = newLine(sale, item, line, amounts.get(i));
            saleLineMapper.insert(saleLine);
            ledgerMapper.insert(newLedger(sale, line, lot, before, after, item.getSystemWeight(), "SALE_OUT"));
        }
    }

    private RemainSaleLine newLine(RemainSale sale, RemainSaleLineDTO item,
                                   RemainRegistrationLine line, BigDecimal amount) {
        RemainSaleLine result = new RemainSaleLine();
        result.setUuid(UUID.randomUUID().toString());
        result.setSaleUuid(sale.getUuid());
        result.setLotUuid(item.getLotUuid());
        result.setRegistrationLineUuid(line.getUuid());
        result.setSystemWeight(item.getSystemWeight());
        result.setAmount(amount);
        result.setCreateTime(LocalDateTime.now());
        return result;
    }

    private RemainInventoryLedger newLedger(RemainSale sale, RemainRegistrationLine line,
                                            RemainInventoryLot lot, BigDecimal before, BigDecimal after,
                                            BigDecimal weight, String event) {
        RemainInventoryLedger result = new RemainInventoryLedger();
        result.setUuid(UUID.randomUUID().toString());
        result.setLotUuid(lot.getUuid());
        result.setRegistrationLineUuid(line.getUuid());
        result.setSourceFinishRollUuid(line.getSourceFinishRollUuid());
        result.setEventType(event);
        result.setWeightDelta("SALE_OUT".equals(event) ? weight.negate() : weight);
        result.setBeforeWeight(before);
        result.setAfterWeight(after);
        result.setRequestId(RemainRequestFingerprint.ledgerRequest(sale.getRequestId(), line.getUuid(), event));
        result.setReason(sale.getReason());
        result.setCreateBy(AuthContextHolder.currentDisplayName());
        return result;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
