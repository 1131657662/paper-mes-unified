package com.paper.mes.remain.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import com.paper.mes.remain.dto.RemainRegistrationLineDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RemainSourceValidator {

    private final ProcessOrderMapper processOrderMapper;
    private final FinishRollMapper finishRollMapper;
    private final BusinessLockService businessLockService;

    public SourceContext lockAndValidate(String orderUuid, List<RemainRegistrationLineDTO> lines) {
        businessLockService.lockProcessOrders(List.of(orderUuid));
        ProcessOrder order = processOrderMapper.selectById(orderUuid);
        if (order == null || !Objects.equals(order.getIsDeleted(), 0)) {
            throw new BusinessException("加工单不存在");
        }
        List<String> rollUuids = lines.stream().map(RemainRegistrationLineDTO::getSourceFinishRollUuid)
                .distinct().toList();
        if (rollUuids.size() != lines.size()) {
            throw new BusinessException("同一余卷不能在一张登记单重复登记");
        }
        businessLockService.lockFinishRolls(rollUuids);
        Map<String, FinishRoll> rolls = finishRollMapper.selectBatchIds(rollUuids).stream()
                .collect(Collectors.toMap(FinishRoll::getUuid, Function.identity()));
        if (rolls.size() != rollUuids.size()) {
            throw new BusinessException("部分来源余卷不存在");
        }
        for (RemainRegistrationLineDTO line : lines) {
            validateLine(order, rolls.get(line.getSourceFinishRollUuid()), line.getTransferredSystemWeight());
        }
        return new SourceContext(order, rolls);
    }

    private void validateLine(ProcessOrder order, FinishRoll roll, BigDecimal requested) {
        if (!Objects.equals(roll.getOrderUuid(), order.getUuid())
                || order.getCustomerUuid() == null || order.getCustomerUuid().isBlank()) {
            throw new BusinessException("登记来源必须属于同一加工单和客户");
        }
        if (!Integer.valueOf(1).equals(roll.getIsRemain()) || !Integer.valueOf(2).equals(roll.getFinishStatus())) {
            throw new BusinessException("只有已入库的余料卷可以登记");
        }
        BigDecimal available = availableWeight(roll);
        if (requested == null || requested.signum() <= 0 || requested.compareTo(available) > 0) {
            throw new BusinessException("登记重量超过客户当前可用余料");
        }
    }

    public static BigDecimal availableWeight(FinishRoll roll) {
        BigDecimal available = roll.getRemainingWeight();
        if (available == null) {
            available = roll.getActualWeight();
        }
        if (available == null || available.signum() < 0) {
            throw new BusinessException("来源余卷没有可用系统重量");
        }
        return available;
    }

    public record SourceContext(ProcessOrder order, Map<String, FinishRoll> rolls) {
    }
}
