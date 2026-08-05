package com.paper.mes.safety;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryLedgerSchemaContractTest {

    @Test
    void migrationDefinesAppendOnlyLedgerAndAllRequiredEvents() throws Exception {
        String sql = Files.readString(Path.of("sql/V3.52__add_inventory_transaction_ledger.sql"),
                StandardCharsets.UTF_8);

        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `biz_inventory_transaction`");
        assertThat(sql).contains("UNIQUE KEY `uk_inventory_transaction_idempotency`");
        assertThat(sql).contains("FOREIGN KEY (`finish_roll_uuid`)");
        assertThat(sql).contains("trg_inventory_transaction_no_update");
        assertThat(sql).contains("trg_inventory_transaction_no_delete");
        assertThat(sql).contains("OPENING_BALANCE").contains("RECEIPT").contains("RESERVE")
                .contains("RELEASE").contains("ISSUE").contains("RETURN").contains("SCRAP")
                .contains("ADJUSTMENT");
        assertThat(sql).contains("quantity_before").contains("quantity_after")
                .contains("weight_before").contains("weight_after")
                .contains("reserved_weight_before").contains("available_weight_after")
                .contains("sequence_no").contains("DATETIME(6)");
    }

    @Test
    void canonicalBaselineContainsLedgerAndImmutabilityTriggers() throws Exception {
        String baseline = Files.readString(Path.of("sql/01_schema_v4.1.sql"), StandardCharsets.UTF_8);

        assertThat(baseline).contains("CREATE TABLE `biz_inventory_transaction`")
                .contains("trg_inventory_transaction_no_update")
                .contains("trg_inventory_transaction_no_delete")
                .contains("chk_inventory_transaction_balance_equation");
    }

    @Test
    void businessWritersRequireLedgerBeanAtApplicationStartup() throws Exception {
        String delivery = Files.readString(Path.of(
                "src/main/java/com/paper/mes/delivery/service/impl/DeliveryServiceImpl.java"),
                StandardCharsets.UTF_8);
        String processOrder = Files.readString(Path.of(
                "src/main/java/com/paper/mes/processorder/service/impl/ProcessOrderServiceImpl.java"),
                StandardCharsets.UTF_8);

        assertThat(delivery).contains("private final InventoryLedgerBusinessRecorder inventoryLedgerRecorder")
                .doesNotContain("@Autowired\n    private InventoryLedgerBusinessRecorder inventoryLedgerRecorder")
                .doesNotContain("inventoryLedgerRecorder != null")
                .doesNotContain("inventoryLedgerRecorder == null");
        assertThat(processOrder).contains("private final InventoryLedgerBusinessRecorder inventoryLedgerRecorder")
                .doesNotContain("@Autowired\n    private final InventoryLedgerBusinessRecorder inventoryLedgerRecorder")
                .doesNotContain("inventoryLedgerRecorder == null")
                .doesNotContain("inventoryLedgerRecorder != null");
    }

    @Test
    void localStartupChecksAndBootstrapsInventoryLedgerBeforeStartingServices() throws Exception {
        String launcher = Files.readString(Path.of("dev.ps1"), StandardCharsets.UTF_8);
        String schemaScript = Files.readString(Path.of("dev-schema.ps1"), StandardCharsets.UTF_8);

        assertThat(launcher).contains("Ensure-LocalInventoryLedgerSchema").contains(". $SchemaScript");
        assertThat(schemaScript).contains("V3.52__add_inventory_transaction_ledger.sql")
                .contains("biz_inventory_transaction")
                .contains("COUNT(DISTINCT index_name)")
                .contains("Get-Content -LiteralPath $MigrationPath -Raw -Encoding utf8")
                .contains("$OutputEncoding");
    }
}
