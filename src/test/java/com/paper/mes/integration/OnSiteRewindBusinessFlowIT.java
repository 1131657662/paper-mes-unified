package com.paper.mes.integration;

import com.paper.mes.processorder.dto.BackRecordDTO;
import com.paper.mes.processorder.dto.BackRecordFinishDTO;
import com.paper.mes.processorder.dto.BackRecordRollDTO;
import com.paper.mes.processorder.dto.BackRecordStepDTO;
import com.paper.mes.processorder.dto.PrintDTO;
import com.paper.mes.processorder.service.ProcessOrderService;
import com.paper.mes.health.service.ProductionHealthInspector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class OnSiteRewindBusinessFlowIT {

    @Autowired private RepresentativeRewindFixture fixture;
    @Autowired private BusinessFlowOrderCleanup cleanup;
    @Autowired private ProcessOrderService processOrderService;
    @Autowired private ProductionHealthInspector productionHealthInspector;
    @Autowired private JdbcTemplate jdbcTemplate;

    private RepresentativeOrderFixture.Scenario scenario;

    @AfterEach
    void tearDown() {
        if (scenario != null) cleanup.delete(scenario.orderUuid());
    }

    @Test
    void onSiteRewind_withoutPreGeneratedFinish_recordsActualSpecificationAtCompletion() {
        scenario = fixture.createOnSite();
        processOrderService.print(scenario.orderUuid(), new PrintDTO());
        processOrderService.changeStatus(scenario.orderUuid(), 3, null);
        var before = processOrderService.getDetail(scenario.orderUuid());

        processOrderService.backRecord(scenario.orderUuid(), request(before));

        var detail = processOrderService.getDetail(scenario.orderUuid());
        var finish = detail.getFinishRolls().getFirst();
        assertThat(before.getFinishRolls()).isEmpty();
        assertThat(detail.getOrder().getOrderStatus()).isEqualTo(4);
        assertThat(finish.getFinishWidth()).isEqualTo(900);
        assertThat(finish.getFinishDiameter()).isEqualTo(30);
        assertThat(finish.getFinishCoreDiameter()).isEqualTo(3);
        assertThat(finish.getActualWeight()).isEqualByComparingTo("800.000");
    }

    @Test
    void onSiteRewind_withInvalidInventoryWidth_isReportedAsCriticalHealthIssue() {
        scenario = fixture.createOnSite();
        processOrderService.print(scenario.orderUuid(), new PrintDTO());
        processOrderService.changeStatus(scenario.orderUuid(), 3, null);
        var before = processOrderService.getDetail(scenario.orderUuid());
        processOrderService.backRecord(scenario.orderUuid(), request(before));
        var finish = processOrderService.getDetail(scenario.orderUuid()).getFinishRolls().getFirst();

        jdbcTemplate.update("UPDATE biz_finish_roll SET finish_width = 0 WHERE uuid = ?", finish.getUuid());

        assertThat(productionHealthInspector.inspect()).anySatisfy(issue -> {
            assertThat(issue.issueType()).isEqualTo("ONSITE_FINISH_WIDTH_INVALID");
            assertThat(issue.severity()).isEqualTo("CRITICAL");
            assertThat(issue.businessUuid()).isEqualTo(finish.getUuid());
        });
    }

    private BackRecordDTO request(com.paper.mes.processorder.dto.ProcessOrderDetailVO detail) {
        var roll = detail.getOriginalRolls().getFirst();
        BackRecordDTO dto = new BackRecordDTO();
        dto.setRolls(List.of(rollRecord(roll.getUuid())));
        dto.setFinishes(List.of(finishRecord(roll.getUuid())));
        BackRecordStepDTO step = new BackRecordStepDTO();
        step.setUuid(detail.getSteps().getFirst().getUuid());
        step.setLossWeight(BigDecimal.ZERO);
        dto.setSteps(List.of(step));
        return dto;
    }

    private BackRecordRollDTO rollRecord(String uuid) {
        BackRecordRollDTO dto = new BackRecordRollDTO();
        dto.setUuid(uuid);
        dto.setActualGramWeight(100);
        dto.setActualWidth(1500);
        dto.setActualWeight(new BigDecimal("800.000"));
        return dto;
    }

    private BackRecordFinishDTO finishRecord(String originalUuid) {
        BackRecordFinishDTO dto = new BackRecordFinishDTO();
        dto.setOriginalUuid(originalUuid);
        dto.setFinishWidth(900);
        dto.setFinishDiameter(30);
        dto.setFinishCoreDiameter(3);
        dto.setActualWeight(new BigDecimal("800.000"));
        dto.setIsRemain(0);
        dto.setIsAbnormal(0);
        return dto;
    }
}
