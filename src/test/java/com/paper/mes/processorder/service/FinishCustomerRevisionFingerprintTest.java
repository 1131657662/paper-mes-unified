package com.paper.mes.processorder.service;

import com.paper.mes.customerdisplay.formula.CustomerWeightCalculationMode;
import com.paper.mes.processorder.dto.FinishCustomerRevisionRequestDTO;
import com.paper.mes.processorder.dto.FinishCustomerSpecItemDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class FinishCustomerRevisionFingerprintTest {

    @Test
    void of_sameBusinessPayloadWithDifferentRequestIds_returnsSameHash() {
        FinishCustomerRevisionRequestDTO left = request("request-a", "75.0");
        FinishCustomerRevisionRequestDTO right = request("request-b", "75.00");

        assertEquals(FinishCustomerRevisionFingerprint.of(left),
                FinishCustomerRevisionFingerprint.of(right));
    }

    @Test
    void of_changedCustomerGsm_returnsDifferentHash() {
        FinishCustomerRevisionRequestDTO left = request("request-a", "75");
        FinishCustomerRevisionRequestDTO right = request("request-a", "76");

        assertNotEquals(FinishCustomerRevisionFingerprint.of(left),
                FinishCustomerRevisionFingerprint.of(right));
    }

    private FinishCustomerRevisionRequestDTO request(String requestId, String customerGsm) {
        FinishCustomerSpecItemDTO item = new FinishCustomerSpecItemDTO();
        item.setFinishUuid("finish-1");
        item.setExpectedVersion(1);
        item.setCustomerGramWeight(new BigDecimal(customerGsm).intValueExact());
        item.setCalculationMode(CustomerWeightCalculationMode.FORMULA);
        item.setFormulaExpression("physicalWeight * customerGsm / physicalGsm");
        item.setFormulaVariables(Map.of("adjustment", BigDecimal.ZERO));

        FinishCustomerRevisionRequestDTO request = new FinishCustomerRevisionRequestDTO();
        request.setRequestId(requestId);
        request.setExpectedOrderVersion(1);
        request.setReason("customer request");
        request.setItems(List.of(item));
        return request;
    }
}
