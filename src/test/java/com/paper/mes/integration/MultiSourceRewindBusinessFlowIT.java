package com.paper.mes.integration;

import com.paper.mes.processorder.service.ProcessOrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class MultiSourceRewindBusinessFlowIT {

    @Autowired private RepresentativeRewindFixture rewindFixture;
    @Autowired private RepresentativeOrderFixture orderFixture;
    @Autowired private BusinessFlowOrderCleanup cleanup;
    @Autowired private ProcessOrderService processOrderService;

    private RepresentativeOrderFixture.Scenario scenario;

    @AfterEach
    void tearDown() {
        if (scenario != null) cleanup.delete(scenario.orderUuid());
    }

    @Test
    void mergedRewind_withTwoMotherRolls_completesWithBothSources() {
        scenario = rewindFixture.createMerge();

        orderFixture.issueAndComplete(scenario);

        var detail = processOrderService.getDetail(scenario.orderUuid());
        assertThat(detail.getOrder().getOrderStatus()).isEqualTo(4);
        assertThat(detail.getOriginalRolls()).hasSize(2);
        assertThat(detail.getFinishRolls()).hasSize(1);
        assertThat(detail.getRollProductions().getFirst().getFinishes().getFirst().getSources()).hasSize(2);
    }
}
