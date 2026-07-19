package com.paper.mes.settle.service;

import com.paper.mes.customer.entity.Customer;

import java.time.LocalDate;
import java.time.YearMonth;

/** Calculates the contractual payment due date for a settlement. */
public final class SettlementDueDatePolicy {

    private static final int SETTLE_TYPE_BY_MONTH = 2;

    private SettlementDueDatePolicy() {
    }

    public static LocalDate resolve(Customer customer, LocalDate settleDate, LocalDate periodEnd) {
        if (!hasMonthlyTerms(customer) || settleDate == null) {
            return settleDate;
        }
        LocalDate base = periodEnd == null ? settleDate : periodEnd;
        int settleDay = customer.getSettleDay();
        YearMonth baseMonth = YearMonth.from(base);
        LocalDate dueInBaseMonth = atContractDay(baseMonth, settleDay);
        if (!dueInBaseMonth.isBefore(base)) {
            return dueInBaseMonth;
        }
        return atContractDay(baseMonth.plusMonths(1), settleDay);
    }

    private static boolean hasMonthlyTerms(Customer customer) {
        return customer != null
                && customer.getSettleType() != null
                && customer.getSettleType() == SETTLE_TYPE_BY_MONTH
                && customer.getSettleDay() != null
                && customer.getSettleDay() > 0;
    }

    private static LocalDate atContractDay(YearMonth month, int settleDay) {
        return month.atDay(Math.min(settleDay, month.lengthOfMonth()));
    }
}
