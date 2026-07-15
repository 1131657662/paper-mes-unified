package com.paper.mes.integration;

import com.paper.mes.processorder.dto.PrintDTO;
import com.paper.mes.processorder.dto.PrintViewVersion;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import com.paper.mes.processorder.service.ProcessOrderService;
import com.paper.mes.oplog.service.OperationLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ProcessOrderPrintViewBusinessFlowIT {

    @Autowired private BackRecordOnSiteFixture fixture;
    @Autowired private BusinessFlowOrderCleanup cleanup;
    @Autowired private ProcessOrderService processOrderService;
    @Autowired private ProcessOrderMapper processOrderMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    private BackRecordOnSiteFixture.Scenario scenario;

    @BeforeEach
    void setUp() {
        scenario = fixture.arrange();
        scenario.order().setOrderStatus(1);
        scenario.order().setPrintStatus(0);
        scenario.order().setPrintCount(0);
        processOrderMapper.updateById(scenario.order());
        processOrderService.print(scenario.order().getUuid(), new PrintDTO());
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM sys_operation_log WHERE biz_uuid = ?", scenario.order().getUuid());
        cleanup.delete(scenario.order().getUuid());
    }

    @Test
    void issuedPrintView_whenLiveStructureChanges_keepsFrozenVersion() {
        String orderUuid = scenario.order().getUuid();
        var before = processOrderService.getPrintView(orderUuid, PrintViewVersion.ISSUED);
        String originalUuid = before.getDetail().getOriginalRolls().getFirst().getUuid();

        jdbcTemplate.update("UPDATE biz_original_roll SET paper_name = 'changed-after-print' WHERE uuid = ?",
                originalUuid);
        var after = processOrderService.getPrintView(orderUuid, PrintViewVersion.ISSUED);

        assertThat(after.getSchemaVersion()).isEqualTo("2.0");
        assertThat(after.getSource()).isEqualTo("SNAPSHOT");
        assertThat(after.getDetail().getOriginalRolls().getFirst().getPaperName())
                .isEqualTo(before.getDetail().getOriginalRolls().getFirst().getPaperName())
                .isNotEqualTo("changed-after-print");
    }

    @Test
    void reprint_withReason_recordsAuditableOperation() {
        PrintDTO dto = new PrintDTO();
        dto.setReason("车间纸张污损");

        processOrderService.print(scenario.order().getUuid(), dto);

        var log = jdbcTemplate.queryForMap("""
                SELECT action_type, remark, biz_no
                FROM sys_operation_log
                WHERE biz_uuid = ? AND action_type = ?
                """, scenario.order().getUuid(), OperationLogService.ACTION_REPRINT);
        assertThat(log.get("action_type")).isEqualTo(OperationLogService.ACTION_REPRINT);
        assertThat(log.get("remark")).isEqualTo("车间纸张污损");
        assertThat(log.get("biz_no")).isEqualTo(scenario.order().getOrderNo());
    }
}
