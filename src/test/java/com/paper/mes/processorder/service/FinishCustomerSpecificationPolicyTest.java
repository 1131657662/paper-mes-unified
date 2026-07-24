package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.FinishConfigSpecDTO;
import com.paper.mes.processorder.entity.FinishRoll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FinishCustomerSpecificationPolicyTest {

    @Test
    void apply_override_preserves_physical_specification_and_records_audit() {
        FinishRoll finish = physicalFinish();
        FinishConfigSpecDTO spec = customerSpec("客户按合同标注");

        FinishCustomerSpecificationPolicy.apply(finish, spec, "planner");

        assertEquals(265, finish.getGramWeight());
        assertEquals(1000, finish.getFinishWidth());
        assertEquals(275, finish.getCustomerGramWeight());
        assertEquals(1000, finish.getCustomerFinishWidth());
        assertEquals("planner", finish.getCustomerSpecOverrideBy());
        assertNotNull(finish.getCustomerSpecOverrideAt());
    }

    @Test
    void apply_override_without_reason_is_rejected() {
        assertThrows(BusinessException.class,
                () -> FinishCustomerSpecificationPolicy.apply(physicalFinish(), customerSpec(null), "planner"));
    }

    private FinishRoll physicalFinish() {
        FinishRoll finish = new FinishRoll();
        finish.setPaperName("白卡纸");
        finish.setGramWeight(265);
        finish.setFinishWidth(1000);
        return finish;
    }

    private FinishConfigSpecDTO customerSpec(String reason) {
        FinishConfigSpecDTO spec = new FinishConfigSpecDTO();
        spec.setCustomerPaperName("客户白卡");
        spec.setCustomerGramWeight(275);
        spec.setCustomerFinishWidth(1000);
        spec.setCustomerSpecOverrideReason(reason);
        return spec;
    }
}
