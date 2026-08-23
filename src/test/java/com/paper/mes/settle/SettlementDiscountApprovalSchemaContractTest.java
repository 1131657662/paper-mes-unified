package com.paper.mes.settle;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SettlementDiscountApprovalSchemaContractTest {
    @Test
    void migrationBindsApprovalToTheCompleteReceiptPlan() throws IOException {
        String migration = Files.readString(Path.of(
                "sql/V3.63__complete_settlement_discount_approval_workflow.sql"));

        assertThat(migration).contains(
                "`cash_amount`", "`scrap_offset_amount`", "`unreceived_snapshot`",
                "`discount_percent`", "`required_level`", "`request_hash`",
                "`decision_reason`", "`policy_version`", "`active_settle_uuid`",
                "uk_discount_approval_active_settle", "idx_discount_approval_inbox",
                "idx_discount_approval_requester", "approval_status` BETWEEN 1 AND 6",
                "WHERE `policy_version` = 'legacy-v1'",
                "历史审批缺少完整收款方案，请重新提交");
    }

    @Test
    void canonicalSchemaMatchesMigrationBoundary() throws IOException {
        String schema = Files.readString(Path.of("sql/01_schema_v4.1.sql"));
        assertThat(Files.readString(Path.of("sql/schema-baseline.version")).trim()).isEqualTo("3.73.1");
        assertThat(schema).contains("uk_discount_approval_active_settle",
                "idx_discount_approval_inbox", "chk_discount_approval_level");
    }
}
