package com.paper.mes.integration;

import com.paper.mes.processorder.dto.FinishConfigBatchSaveDTO;
import com.paper.mes.processorder.dto.FinishConfigSaveDTO;
import com.paper.mes.processorder.dto.FinishConfigSpecDTO;
import com.paper.mes.processorder.dto.OriginalRollDTO;
import com.paper.mes.processorder.service.ProcessOrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class FinishConfigBatchBusinessFlowIT {

    @Autowired private RepresentativeOrderFixture fixture;
    @Autowired private BusinessFlowOrderCleanup cleanup;
    @Autowired private ProcessOrderService processOrderService;

    private RepresentativeOrderFixture.Scenario scenario;

    @AfterEach
    void tearDown() {
        if (scenario != null) cleanup.delete(scenario.orderUuid());
    }

    @Test
    void batchSave_whenLaterRollFails_keepsEarlierConfigurationUnchanged() {
        scenario = fixture.createStandardSaw();
        String secondRollUuid = processOrderService.addRoll(scenario.orderUuid(), secondRoll());
        var before = processOrderService.getDetail(scenario.orderUuid());
        String firstRollUuid = before.getOriginalRolls().getFirst().getUuid();
        List<String> originalFinishNumbers = before.getFinishRolls().stream()
                .map(item -> item.getFinishRollNo())
                .toList();

        FinishConfigBatchSaveDTO request = batchRequest(firstRollUuid, secondRollUuid);

        assertThatThrownBy(() -> processOrderService.saveFinishConfigBatch(scenario.orderUuid(), request));
        var after = processOrderService.getDetail(scenario.orderUuid());
        assertThat(after.getFinishRolls()).extracting(item -> item.getFinishRollNo())
                .containsExactlyElementsOf(originalFinishNumbers);
    }

    private FinishConfigBatchSaveDTO batchRequest(String firstRollUuid, String secondRollUuid) {
        FinishConfigBatchSaveDTO dto = new FinishConfigBatchSaveDTO();
        dto.setItems(List.of(item(firstRollUuid, config(1900)), item(secondRollUuid, config(2500))));
        return dto;
    }

    private FinishConfigBatchSaveDTO.FinishConfigBatchItemDTO item(
            String rollUuid,
            FinishConfigSaveDTO config) {
        var item = new FinishConfigBatchSaveDTO.FinishConfigBatchItemDTO();
        item.setRollUuid(rollUuid);
        item.setConfig(config);
        return item;
    }

    private FinishConfigSaveDTO config(int finishWidth) {
        FinishConfigSaveDTO dto = new FinishConfigSaveDTO();
        dto.setProcessMode(1);
        dto.setMainStepType(1);
        dto.setSpareCount(0);
        dto.setUnitPrice(new BigDecimal("12.00"));
        dto.setFinishSpecs(List.of(finishSpec(finishWidth)));
        return dto;
    }

    private FinishConfigSpecDTO finishSpec(int width) {
        FinishConfigSpecDTO dto = new FinishConfigSpecDTO();
        dto.setItemType("FINISH");
        dto.setFinishWidth(width);
        dto.setCount(1);
        return dto;
    }

    private OriginalRollDTO secondRoll() {
        OriginalRollDTO dto = new OriginalRollDTO();
        dto.setRollNo("IT-BATCH-SECOND");
        dto.setPaperName("批量配置事务测试纸");
        dto.setGramWeight(80);
        dto.setOriginalWidth(2000);
        dto.setRollWeight(new BigDecimal("1000.000"));
        dto.setPieceNum(1);
        dto.setProcessMode(1);
        dto.setMainStepType(1);
        return dto;
    }
}
