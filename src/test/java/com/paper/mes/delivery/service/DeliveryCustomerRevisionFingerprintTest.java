package com.paper.mes.delivery.service;

import com.paper.mes.customerdisplay.formula.*;
import com.paper.mes.delivery.dto.*;
import org.junit.jupiter.api.Test;

import java.math.RoundingMode;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeliveryCustomerRevisionFingerprintTest {

    @Test
    void fingerprint_isStableAcrossItemOrder() {
        DeliveryCustomerRevisionRequestDTO first = request(List.of(item("d1"), item("d2")));
        DeliveryCustomerRevisionRequestDTO second = request(List.of(item("d2"), item("d1")));

        assertEquals(DeliveryCustomerRevisionFingerprint.of(first),
                DeliveryCustomerRevisionFingerprint.of(second));
    }

    private DeliveryCustomerRevisionRequestDTO request(List<DeliveryCustomerSpecItemDTO> items) {
        DeliveryCustomerRevisionRequestDTO request = new DeliveryCustomerRevisionRequestDTO();
        request.setRequestId("request-1");
        request.setExpectedDeliveryVersion(1);
        request.setReason("客户要求更正");
        request.setItems(items);
        return request;
    }

    private DeliveryCustomerSpecItemDTO item(String uuid) {
        DeliveryCustomerSpecItemDTO item = new DeliveryCustomerSpecItemDTO();
        item.setDeliveryDetailUuid(uuid);
        item.setExpectedDetailVersion(1);
        item.setCalculationMode(CustomerWeightCalculationMode.KEEP);
        item.setRoundingScale(3);
        item.setRoundingMode(RoundingMode.HALF_UP);
        item.setZeroPolicy(CustomerWeightZeroPolicy.SKIP);
        return item;
    }
}
