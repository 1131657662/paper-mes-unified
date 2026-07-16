package com.paper.mes.settle.service.impl;

import com.paper.mes.common.BusinessException;
import com.paper.mes.settle.dto.ReceiveDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class SettleReceiveAmountResolver {

    static final int RECEIVE_TYPE_CASH = 1;
    static final int RECEIVE_TYPE_SCRAP = 2;
    static final int RECEIVE_TYPE_MIXED = 3;
    static final int RECEIVE_TYPE_DISCOUNT = 4;

    private static final int MONEY_SCALE = 2;
    private static final int WEIGHT_SCALE = 3;
    private static final int PRICE_SCALE = 4;

    private SettleReceiveAmountResolver() {
    }

    static Resolved resolve(ReceiveDTO dto, BigDecimal unreceivedAmount) {
        BigDecimal cash = cashAmount(dto);
        BigDecimal scrap = money(dto.getScrapOffsetAmount());
        BigDecimal discount = money(dto.getDiscountAmount());
        BigDecimal total = cash.add(scrap).add(discount).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal weight = weight(dto.getScrapWeight());
        Amounts amounts = new Amounts(cash, scrap, discount, total, weight);
        validate(dto, amounts, unreceivedAmount);
        return new Resolved(total, cash, scrap, discount, weight,
                unitPrice(scrap, weight), receiveType(amounts));
    }

    private static BigDecimal cashAmount(ReceiveDTO dto) {
        BigDecimal explicitCash = money(dto.getCashAmount());
        if (dto.getCashAmount() != null || dto.getScrapOffsetAmount() != null || dto.getDiscountAmount() != null) {
            return explicitCash;
        }
        return money(dto.getReceiveAmount());
    }

    private static void validate(ReceiveDTO dto, Amounts amounts, BigDecimal unreceivedAmount) {
        if (amounts.total().signum() <= 0) {
            throw new BusinessException("本次结清金额必须大于 0");
        }
        if (amounts.total().compareTo(money(unreceivedAmount)) > 0) {
            throw new BusinessException("本次结清金额超过未收金额，禁止超额核销");
        }
        if (amounts.scrap().signum() > 0 && amounts.weight().signum() <= 0) {
            throw new BusinessException("废纸抵扣金额大于 0 时，废纸重量必须大于 0");
        }
        if (amounts.cash().signum() > 0 && dto.getPayMethod() == null) {
            throw new BusinessException("实际到账金额大于 0 时必须选择收款方式");
        }
        if (amounts.cash().signum() > 0 && dto.getPayMethod() != null && dto.getPayMethod() != 1
                && (dto.getPayNo() == null || dto.getPayNo().isBlank())) {
            throw new BusinessException("转账、微信或支付宝到账必须填写交易流水号");
        }
    }

    private static int receiveType(Amounts amounts) {
        int componentCount = positive(amounts.cash()) + positive(amounts.scrap()) + positive(amounts.discount());
        if (componentCount > 1) {
            return RECEIVE_TYPE_MIXED;
        }
        if (amounts.scrap().signum() > 0) {
            return RECEIVE_TYPE_SCRAP;
        }
        return amounts.discount().signum() > 0 ? RECEIVE_TYPE_DISCOUNT : RECEIVE_TYPE_CASH;
    }

    private static int positive(BigDecimal amount) {
        return amount.signum() > 0 ? 1 : 0;
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
                    BigDecimal discountAmount, BigDecimal scrapWeight, BigDecimal scrapUnitPrice,
                    int receiveType) {
    }

    private record Amounts(BigDecimal cash, BigDecimal scrap, BigDecimal discount,
                           BigDecimal total, BigDecimal weight) {
    }
}
