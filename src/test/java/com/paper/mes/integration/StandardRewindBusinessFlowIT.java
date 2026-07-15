package com.paper.mes.integration;

import com.paper.mes.processorder.service.ProcessOrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class StandardRewindBusinessFlowIT {

    @Autowired private RepresentativeRewindFixture rewindFixture;
    @Autowired private RepresentativeOrderFixture orderFixture;
    @Autowired private BusinessFlowOrderCleanup cleanup;
    @Autowired private ProcessOrderService processOrderService;

    private RepresentativeOrderFixture.Scenario scenario;

    @AfterEach
    void tearDown() {
        if (scenario != null) cleanup.delete(scenario.orderUuid());
    }

    @ParameterizedTest(name = "复卷模式 {0} 完整流程")
    @CsvSource({
            "1, 4, 1",
            "2, 1, 0",
            "3, 2, 0",
            "4, 1, 0"
    })
    void standardRewind_eachCoreMode_completesWithExpectedOutputs(int mode, int outputCount,
                                                                  int remainCount) {
        scenario = rewindFixture.create(mode);

        orderFixture.issueAndComplete(scenario);

        var detail = processOrderService.getDetail(scenario.orderUuid());
        assertThat(detail.getOrder().getOrderStatus()).isEqualTo(4);
        assertThat(detail.getFinishRolls()).hasSize(outputCount);
        assertThat(detail.getFinishRolls()).filteredOn(item -> item.getIsRemain() == 1).hasSize(remainCount);
        assertThat(detail.getFinishRolls()).allMatch(item -> item.getActualWeight() != null);
        assertThat(detail.getRollProductions().getFirst().getRewindParams())
                .allMatch(item -> item.getParamMode() == mode);
    }
}
