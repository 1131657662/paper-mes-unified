package com.paper.mes.integration;

import com.paper.mes.health.service.DataHealthService;
import com.paper.mes.processorder.dto.FinishConfigSaveDTO;
import com.paper.mes.processorder.dto.FinishConfigSpecDTO;
import com.paper.mes.processorder.dto.ProcessOrderVoidDTO;
import com.paper.mes.processorder.dto.ProcessOrderDetailVO;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.service.ProcessOrderDraftService;
import com.paper.mes.processorder.service.ProcessOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class ProcessOrderRollbackEditBusinessFlowIT {

    @Autowired private RepresentativeOrderFixture fixture;
    @Autowired private ProcessOrderService processOrderService;
    @Autowired private ProcessOrderDraftService draftService;
    @Autowired private DataHealthService dataHealthService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void completedOrder_whenRolledBackAndReconfigured_completesWithOnlyNewOutputs() {
        var scenario = fixture.createStandardSaw();
        fixture.issueAndComplete(scenario);
        var completed = processOrderService.getDetail(scenario.orderUuid());
        String rollUuid = completed.getOriginalRolls().getFirst().getUuid();
        List<String> oldFinishNumbers = completed.getFinishRolls().stream().map(item -> item.getFinishRollNo()).toList();

        processOrderService.rollbackToDraft(scenario.orderUuid(), "客户调整成品门幅");
        var draft = processOrderService.getDetail(scenario.orderUuid());
        draftService.saveProcessConfig(scenario.orderUuid(), rollUuid, changedSawConfig());
        draftService.submit(scenario.orderUuid());
        fixture.issueAndComplete(scenario);

        var revised = processOrderService.getDetail(scenario.orderUuid());
        assertThat(draft.getOrder().getOrderStatus()).isZero();
        assertThat(draft.getOrder().getSnapPrint()).isNull();
        assertThat(draft.getOrder().getSnapFinish()).isNull();
        assertThat(draft.getFinishRolls()).allMatch(item -> item.getRollNoStatus() == 3);
        assertThat(draft.getFinishRolls()).allMatch(item -> item.getActualWeight() == null);
        assertThat(revised.getOrder().getOrderStatus()).isEqualTo(4);
        assertThat(activeFinishes(revised)).hasSize(3);
        assertThat(activeFinishes(revised)).filteredOn(item -> item.getIsRemain() == 0)
                .allMatch(item -> item.getFinishWidth() == 900);
        assertThat(activeFinishes(revised)).filteredOn(item -> item.getIsRemain() == 1)
                .allMatch(item -> item.getFinishWidth() == 200);
        assertThat(activeFinishes(revised)).extracting(item -> item.getFinishRollNo())
                .doesNotContainAnyElementsOf(oldFinishNumbers);
    }

    @Test
    void incompleteCompletedOrder_whenRolledBackAndVoided_clearsCriticalHealthIssues() {
        var scenario = fixture.createStandardSaw();
        fixture.issueAndComplete(scenario);
        makeProductionActualsIncomplete(scenario.orderUuid());

        assertThat(criticalIssueTypes(scenario.orderUuid())).containsExactlyInAnyOrder(
                "COMPLETED_ORIGINAL_WEIGHT_MISSING", "COMPLETED_FINISH_WEIGHT_MISSING",
                "COMPLETED_FINISH_PENDING_INBOUND", "COMPLETED_ORDER_WITHOUT_ACTUAL_OUTPUT");

        processOrderService.rollbackToDraft(scenario.orderUuid(), "清理历史测试单");
        processOrderService.voidOrder(scenario.orderUuid(), voidRequest());

        assertThat(processOrderService.getDetail(scenario.orderUuid()).getOrder().getOrderStatus()).isEqualTo(6);
        assertThat(criticalIssueTypes(scenario.orderUuid())).isEmpty();
    }

    private List<FinishRoll> activeFinishes(ProcessOrderDetailVO detail) {
        return detail.getFinishRolls().stream()
                .filter(item -> item.getRollNoStatus() == null || item.getRollNoStatus() != 3)
                .toList();
    }

    private void makeProductionActualsIncomplete(String orderUuid) {
        jdbcTemplate.update("UPDATE biz_original_roll SET actual_weight = NULL WHERE order_uuid = ?", orderUuid);
        jdbcTemplate.update("""
                UPDATE biz_finish_roll SET actual_weight = NULL, remaining_weight = 0, finish_status = 1
                WHERE order_uuid = ? AND is_deleted = 0 AND is_spare = 0 AND roll_no_status <> 3
                """, orderUuid);
    }

    private List<String> criticalIssueTypes(String orderUuid) {
        return dataHealthService.inspect().issues().stream()
                .filter(issue -> orderUuid.equals(issue.businessUuid()) && "CRITICAL".equals(issue.severity()))
                .map(issue -> issue.issueType())
                .toList();
    }

    private ProcessOrderVoidDTO voidRequest() {
        ProcessOrderVoidDTO dto = new ProcessOrderVoidDTO();
        dto.setReason("本地历史测试单，无真实生产回录数据");
        return dto;
    }

    private FinishConfigSaveDTO changedSawConfig() {
        FinishConfigSaveDTO dto = new FinishConfigSaveDTO();
        dto.setProcessMode(1);
        dto.setMainStepType(1);
        dto.setSpareCount(0);
        dto.setFinishSpecs(List.of(spec("FINISH", 900, 2), spec("TRIM", 200, 1)));
        return dto;
    }

    private FinishConfigSpecDTO spec(String type, int width, int count) {
        FinishConfigSpecDTO dto = new FinishConfigSpecDTO();
        dto.setItemType(type);
        dto.setFinishWidth(width);
        dto.setCount(count);
        return dto;
    }
}
