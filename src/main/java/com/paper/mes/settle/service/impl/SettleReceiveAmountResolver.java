package com.paper.mes.settle.service.impl;

import com.paper.mes.common.BusinessException;
import com.paper.mes.settle.dto.ReceiveDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class SettleReceiveAmountResolver {

    static final int RECEIVE_TYPE_CASH = 1;
    static final int RECEIVE_TYPE_SCRAP = 2;
    static final int RECEIVE_TYPE_MIXED = 3;

    private static final int MONEY_SCALE = 2;
    private static final int WEIGHT_SCALE = 3;
    private static final int PRICE_SCALE = 4;

    private SettleReceiveAmountResolver() {
    }

    static Resolved resolve(ReceiveDTO dto, BigDecimal unreceivedAmount) {
        BigDecimal cash = cashAmount(dto);
        BigDecimal scrap = money(dto.getScrapOffsetAmount());
        BigDecimal total = cash.add(scrap).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal weight = weight(dto.getScrapWeight());
        validate(dto, cash, scrap, total, weight, unreceivedAmount);
        return new Resolved(total, cash, scrap, weight, unitPrice(scrap, weight), receiveType(cash, scrap));
    }

    private static BigDecimal cashAmount(ReceiveDTO dto) {
        BigDecimal explicitCash = money(dto.getCashAmount());
        if (explicitCash.signum() > 0 || dto.getScrapOffsetAmount() != null) {
            return explicitCash;
        }
        return money(dto.getReceiveAmount());
    }

    private static void validate(ReceiveDTO dto, BigDecimal cash, BigDecimal scrap, BigDecimal total,
                                 BigDecimal weight, BigDecimal unreceivedAmount) {
        if (total.signum() <= 0) {
            throw new BusinessException("本次结清金额必须大于 0");
        }
        if (total.compareTo(money(unreceivedAmount)) > 0) {
            throw new BusinessException("本次结清金额超过未收金额，禁止超收");
        }
        if (scrap.signum() > 0 && weight.signum() <= 0) {
            throw new BusinessException("废纸抵扣金额大于 0 时，废纸重量必须大于 0");
        }
        if (cash.signum() > 0 && dto.getPayMethod() == null) {
            throw new BusinessException("现金实收金额大于 0 时必须选择收款方式");
        }
    }

    private static int receiveType(BigDecimal cash, BigDecimal scrap) {
        if (cash.signum() > 0 && scrap.signum() > 0) {
            return RECEIVE_TYPE_MIXED;
        }
        return scrap.signum() > 0 ? RECEIVE_TYPE_SCRAP : RECEIVE_TYPE_CASH;
    }

    private static BigDecimal unitPrice(BigDecimal scrap, BigDecimal weight) {
        if (scrap.signum() <= 0 || weight.signum() <= 0) {
            return BigDecimal.ZERO.setScale(PRICE_SCALE, RoundingMode.HALF_UP);
        }
        return scrap.divide(weight, PRICE_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal weight(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(WEIGHT_SCALE, RoundingMode.HALF_UP);
    }

    record Resolved(BigDecimal receiveAmount, BigDecimal cashAmount, BigDecimal scrapOffsetAmount,
                    BigDecimal scrapWeight, BigDecimal scrapUnitPrice, int receiveType) {
    }
}
