package com.paper.mes.integration;

import com.paper.mes.processorder.dto.PrintViewVersion;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.service.ProcessOrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class StandardSawBusinessFlowIT {

    @Autowired private RepresentativeOrderFixture fixture;
    @Autowired private BusinessFlowOrderCleanup cleanup;
    @Autowired private ProcessOrderService processOrderService;

    private RepresentativeOrderFixture.Scenario scenario;

    @AfterEach
    void tearDown() {
        if (scenario != null) cleanup.delete(scenario.orderUuid());
    }

    @Test
    void standardSaw_withTwoFinishesAndTrim_completesAndFreezesBothPrintVersions() {
        scenario = fixture.createStandardSaw();

        fixture.issueAndComplete(scenario);

        var detail = processOrderService.getDetail(scenario.orderUuid());
        var issued = processOrderService.getPrintView(scenario.orderUuid(), PrintViewVersion.ISSUED);
        var finished = processOrderService.getPrintView(scenario.orderUuid(), PrintViewVersion.FINISHED);
        assertThat(detail.getOrder().getOrderStatus()).isEqualTo(4);
        assertThat(detail.getFinishRolls()).hasSize(3);
        assertThat(detail.getFinishRolls()).filteredOn(item -> item.getIsRemain() == 1).hasSize(1);
        assertThat(detail.getFinishRolls()).extracting(FinishRoll::getActualWeight).doesNotContainNull();
        assertThat(issued.getSchemaVersion()).isEqualTo("2.0");
        assertThat(finished.getSchemaVersion()).isEqualTo("2.0");
        assertThat(issued.getDetail().getFinishRolls()).allMatch(item -> item.getActualWeight() == null);
        assertThat(finished.getDetail().getFinishRolls()).allMatch(item -> item.getActualWeight() != null);
    }
}
