package com.paper.mes.integration;

import com.paper.mes.processorder.service.ProcessOrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class DirectShipBusinessFlowIT {

    @Autowired private RepresentativeOrderFixture fixture;
    @Autowired private BusinessFlowOrderCleanup cleanup;
    @Autowired private ProcessOrderService processOrderService;

    private RepresentativeOrderFixture.Scenario scenario;

    @AfterEach
    void tearDown() {
        if (scenario != null) cleanup.delete(scenario.orderUuid());
    }

    @Test
    void directShip_withoutFinishConfig_createsInventoryDuringBackRecord() {
        scenario = fixture.createDirectShip();

        fixture.issueAndComplete(scenario);

        var detail = processOrderService.getDetail(scenario.orderUuid());
        var original = detail.getOriginalRolls().getFirst();
        var finish = detail.getFinishRolls().getFirst();
        assertThat(detail.getOrder().getOrderStatus()).isEqualTo(4);
        assertThat(detail.getFinishRolls()).hasSize(1);
        assertThat(finish.getSourceType()).isEqualTo(2);
        assertThat(finish.getFinishRollNo()).isEqualTo(original.getRollNo());
        assertThat(finish.getActualWeight()).isEqualByComparingTo("800.000");
    }
}
