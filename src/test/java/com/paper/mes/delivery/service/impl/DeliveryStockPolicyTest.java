package com.paper.mes.delivery.service.impl;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.entity.FinishRoll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeliveryStockPolicyTest {

    @Test
    void remainingAfterConfirm_whenPartialDelivery_keepsRestWeight() {
        FinishRoll finish = finish("A000001", 1, 2, "100.000", "70.000");

        BigDecimal remaining = DeliveryStockPolicy.remainingAfterConfirm(finish, bd("30.000"));

        assertEquals(0, bd("40.000").compareTo(remaining));
    }

    @Test
    void remainingAfterConfirm_whenNoRemainingWeight_usesActualWeight() {
        FinishRoll finish = finish("A000002", 1, 2, "100.000", null);

        BigDecimal remaining = DeliveryStockPolicy.remainingAfterConfirm(finish, bd("100.000"));

        assertEquals(0, BigDecimal.ZERO.compareTo(remaining));
    }

    @Test
    void validateOutWeight_whenDirectRollPartialDelivery_throws() {
        FinishRoll finish = finish("A000003", 2, 2, "100.000", "100.000");

        assertThrows(BusinessException.class,
                () -> DeliveryStockPolicy.validateOutWeight(finish, bd("50.000")));
    }

    @Test
    void remainingAfterRollback_whenConfirmedPartialDelivery_addsBackWeight() {
        FinishRoll finish = finish("A000004", 1, 2, "100.000", "60.000");

        BigDecimal remaining = DeliveryStockPolicy.remainingAfterRollback(finish, bd("40.000"));

        assertEquals(0, bd("100.000").compareTo(remaining));
    }

    @Test
    void availableWeight_whenOldOutboundDataHasNoRemainingWeight_returnsZero() {
        FinishRoll finish = finish("A000005", 1, 3, "100.000", null);

        assertEquals(0, BigDecimal.ZERO.compareTo(DeliveryStockPolicy.availableWeight(finish)));
    }

    private static FinishRoll finish(String rollNo, int sourceType, int status, String actual, String remaining) {
        FinishRoll finish = new FinishRoll();
        finish.setFinishRollNo(rollNo);
        finish.setSourceType(sourceType);
        finish.setFinishStatus(status);
        finish.setActualWeight(actual == null ? null : bd(actual));
        finish.setRemainingWeight(remaining == null ? null : bd(remaining));
        return finish;
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
