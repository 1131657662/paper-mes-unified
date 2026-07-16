package com.paper.mes.settle.service.impl;

import com.paper.mes.common.BusinessException;
import com.paper.mes.settle.dto.ReceiveDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SettleReceiveAmountResolverTest {

    @Test
    void resolve_withLegacyReceiveAmount_treatsAmountAsCash() {
        ReceiveDTO dto = new ReceiveDTO();
        dto.setReceiveAmount(new BigDecimal("1000"));
        dto.setPayMethod(2);

        SettleReceiveAmountResolver.Resolved resolved =
                SettleReceiveAmountResolver.resolve(dto, new BigDecimal("2000"));

        assertThat(resolved.receiveAmount()).isEqualByComparingTo("1000.00");
        assertThat(resolved.cashAmount()).isEqualByComparingTo("1000.00");
        assertThat(resolved.scrapOffsetAmount()).isEqualByComparingTo("0.00");
        assertThat(resolved.receiveType()).isEqualTo(SettleReceiveAmountResolver.RECEIVE_TYPE_CASH);
    }

    @Test
    void resolve_withOnlyScrapOffset_calculatesUnitPrice() {
        ReceiveDTO dto = new ReceiveDTO();
        dto.setCashAmount(BigDecimal.ZERO);
        dto.setScrapOffsetAmount(new BigDecimal("500"));
        dto.setScrapWeight(new BigDecimal("100"));

        SettleReceiveAmountResolver.Resolved resolved =
                SettleReceiveAmountResolver.resolve(dto, new BigDecimal("1000"));

        assertThat(resolved.receiveAmount()).isEqualByComparingTo("500.00");
        assertThat(resolved.cashAmount()).isEqualByComparingTo("0.00");
        assertThat(resolved.scrapUnitPrice()).isEqualByComparingTo("5.0000");
        assertThat(resolved.receiveType()).isEqualTo(SettleReceiveAmountResolver.RECEIVE_TYPE_SCRAP);
    }

    @Test
    void resolve_withCashAndScrap_marksMixedReceive() {
        ReceiveDTO dto = new ReceiveDTO();
        dto.setCashAmount(new BigDecimal("800"));
        dto.setScrapOffsetAmount(new BigDecimal("200"));
        dto.setScrapWeight(new BigDecimal("50"));
        dto.setPayMethod(2);

        SettleReceiveAmountResolver.Resolved resolved =
                SettleReceiveAmountResolver.resolve(dto, new BigDecimal("1000"));

        assertThat(resolved.receiveAmount()).isEqualByComparingTo("1000.00");
        assertThat(resolved.cashAmount()).isEqualByComparingTo("800.00");
        assertThat(resolved.scrapOffsetAmount()).isEqualByComparingTo("200.00");
        assertThat(resolved.receiveType()).isEqualTo(SettleReceiveAmountResolver.RECEIVE_TYPE_MIXED);
    }

    @Test
    void resolve_withCashAndDiscount_closesExactOutstandingAmount() {
        ReceiveDTO dto = new ReceiveDTO();
        dto.setCashAmount(new BigDecimal("1790"));
        dto.setDiscountAmount(new BigDecimal("1"));
        dto.setPayMethod(2);

        SettleReceiveAmountResolver.Resolved resolved =
                SettleReceiveAmountResolver.resolve(dto, new BigDecimal("1791"));

        assertThat(resolved.receiveAmount()).isEqualByComparingTo("1791.00");
        assertThat(resolved.cashAmount()).isEqualByComparingTo("1790.00");
        assertThat(resolved.discountAmount()).isEqualByComparingTo("1.00");
        assertThat(resolved.receiveType()).isEqualTo(SettleReceiveAmountResolver.RECEIVE_TYPE_MIXED);
    }

    @Test
    void resolve_withOnlyDiscount_marksDiscountWriteOff() {
        ReceiveDTO dto = new ReceiveDTO();
        dto.setDiscountAmount(new BigDecimal("1"));

        SettleReceiveAmountResolver.Resolved resolved =
                SettleReceiveAmountResolver.resolve(dto, new BigDecimal("1"));

        assertThat(resolved.receiveAmount()).isEqualByComparingTo("1.00");
        assertThat(resolved.receiveType()).isEqualTo(SettleReceiveAmountResolver.RECEIVE_TYPE_DISCOUNT);
    }

    @Test
    void resolve_whenTotalExceedsUnreceived_rejectsOverpay() {
        ReceiveDTO dto = new ReceiveDTO();
        dto.setCashAmount(new BigDecimal("1001"));
        dto.setPayMethod(2);

        assertThatThrownBy(() -> SettleReceiveAmountResolver.resolve(dto, new BigDecimal("1000")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void resolve_whenCashAndDiscountExceedUnreceived_rejectsWriteOff() {
        ReceiveDTO dto = new ReceiveDTO();
        dto.setCashAmount(new BigDecimal("1000"));
        dto.setDiscountAmount(new BigDecimal("1"));
        dto.setPayMethod(2);

        assertThatThrownBy(() -> SettleReceiveAmountResolver.resolve(dto, new BigDecimal("1000")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void resolve_whenScrapHasNoWeight_rejectsInvalidOffset() {
        ReceiveDTO dto = new ReceiveDTO();
        dto.setScrapOffsetAmount(new BigDecimal("100"));

        assertThatThrownBy(() -> SettleReceiveAmountResolver.resolve(dto, new BigDecimal("1000")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void resolve_whenCashHasNoPayMethod_rejectsMissingMethod() {
        ReceiveDTO dto = new ReceiveDTO();
        dto.setCashAmount(new BigDecimal("100"));

        assertThatThrownBy(() -> SettleReceiveAmountResolver.resolve(dto, new BigDecimal("1000")))
                .isInstanceOf(BusinessException.class);
    }
}
