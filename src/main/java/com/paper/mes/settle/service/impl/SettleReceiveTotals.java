package com.paper.mes.settle.service.impl;

import com.paper.mes.settle.entity.ReceiveRecord;

import java.math.BigDecimal;
import java.math.RoundingMode;

record SettleReceiveTotals(BigDecimal receiveAmount, BigDecimal cashAmount, BigDecimal scrapOffsetAmount) {

    private static final int MONEY_SCALE = 2;

    static SettleReceiveTotals zero() {
        return new SettleReceiveTotals(money(null), money(null), money(null));
    }

    SettleReceiveTotals add(ReceiveRecord record) {
        return new SettleReceiveTotals(
                receiveAmount.add(money(record.getReceiveAmount())),
                cashAmount.add(cashAmount(record)),
                scrapOffsetAmount.add(money(record.getScrapOffsetAmount())));
    }

    private static BigDecimal cashAmount(ReceiveRecord record) {
        if (record.getCashAmount() != null) {
            return money(record.getCashAmount());
        }
        if (record.getScrapOffsetAmount() == null || record.getScrapOffsetAmount().signum() == 0) {
            return money(record.getReceiveAmount());
        }
        return money(null);
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
