package com.paper.mes.remain.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.remain.dto.RemainSaleCreateDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;

final class RemainSalePolicy {
    static final String SYSTEM_WEIGHT = "SYSTEM_WEIGHT_UNIT_PRICE";
    static final String ACTUAL_WEIGHT = "ACTUAL_WEIGHT_UNIT_PRICE";
    static final String TOTAL_AMOUNT = "TOTAL_AMOUNT";

    private RemainSalePolicy() {
    }

    static void validateRequest(RemainSaleCreateDTO request) {
        if (request.getLines() == null || request.getLines().isEmpty()) {
            throw new BusinessException("至少选择一个我方库存批次");
        }
        if (new HashSet<>(request.getLines().stream().map(item -> item.getLotUuid()).toList()).size()
                != request.getLines().size()) {
            throw new BusinessException("同一库存批次不能重复选择");
        }
        if (!SYSTEM_WEIGHT.equals(request.getPricingMode())
                && !ACTUAL_WEIGHT.equals(request.getPricingMode())
                && !TOTAL_AMOUNT.equals(request.getPricingMode())) {
            throw new BusinessException("不支持的余料计价方式");
        }
        if (!TOTAL_AMOUNT.equals(request.getPricingMode()) && positive(request.getUnitPrice()).signum() <= 0) {
            throw new BusinessException("按单价计价时单价必须大于零");
        }
        if (ACTUAL_WEIGHT.equals(request.getPricingMode()) && positive(request.getActualWeight()).signum() <= 0) {
            throw new BusinessException("按实际过磅计价时必须填写实际重量");
        }
        if (TOTAL_AMOUNT.equals(request.getPricingMode()) && request.getTotalAmount() == null) {
            throw new BusinessException("协商总价不能为空");
        }
        if (TOTAL_AMOUNT.equals(request.getPricingMode()) && positive(request.getTotalAmount()).signum() < 0) {
            throw new BusinessException("协商总价不能为负数");
        }
        if (request.getReceivedAmount().remainder(BigDecimal.ONE).signum() != 0) {
            throw new BusinessException("实收金额必须为整数元");
        }
    }

    static BigDecimal calculateAmount(RemainSaleCreateDTO request, BigDecimal systemWeight) {
        BigDecimal amount = switch (request.getPricingMode()) {
            case SYSTEM_WEIGHT -> systemWeight.multiply(request.getUnitPrice());
            case ACTUAL_WEIGHT -> request.getActualWeight().multiply(request.getUnitPrice());
            case TOTAL_AMOUNT -> request.getTotalAmount();
            default -> throw new BusinessException("不支持的余料计价方式");
        };
        return amount.setScale(0, RoundingMode.HALF_UP);
    }

    private static BigDecimal positive(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
