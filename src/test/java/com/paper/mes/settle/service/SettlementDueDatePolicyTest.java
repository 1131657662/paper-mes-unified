package com.paper.mes.settle.service;

import com.paper.mes.customer.entity.Customer;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class SettlementDueDatePolicyTest {

    @Test
    void resolve_whenContractDayPassed_usesNextMonthContractDay() {
        Customer customer = monthlyCustomer(25);

        LocalDate dueDate = SettlementDueDatePolicy.resolve(
                customer, LocalDate.of(2026, 7, 31), LocalDate.of(2026, 7, 31));

        assertThat(dueDate).isEqualTo(LocalDate.of(2026, 8, 25));
    }

    @Test
    void resolve_whenNextMonthIsShort_clampsToLastDay() {
        Customer customer = monthlyCustomer(30);

        LocalDate dueDate = SettlementDueDatePolicy.resolve(
                customer, LocalDate.of(2026, 1, 31), LocalDate.of(2026, 1, 31));

        assertThat(dueDate).isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    void resolve_whenCustomerIsNotMonthly_usesSettlementDate() {
        Customer customer = new Customer();
        customer.setSettleType(1);

        LocalDate dueDate = SettlementDueDatePolicy.resolve(
                customer, LocalDate.of(2026, 7, 18), LocalDate.of(2026, 7, 31));

        assertThat(dueDate).isEqualTo(LocalDate.of(2026, 7, 18));
    }

    private Customer monthlyCustomer(int settleDay) {
        Customer customer = new Customer();
        customer.setSettleType(2);
        customer.setSettleDay(settleDay);
        return customer;
    }
}
