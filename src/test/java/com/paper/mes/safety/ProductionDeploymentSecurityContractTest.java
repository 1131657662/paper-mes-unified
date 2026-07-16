package com.paper.mes.safety;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionDeploymentSecurityContractTest {

    private static final List<String> DDL_BOOTSTRAPS = List.of(
            "src/main/java/com/paper/mes/backup/config/BackupIntegrityBootstrap.java",
            "src/main/java/com/paper/mes/notification/config/NotificationIntegrityBootstrap.java",
            "src/main/java/com/paper/mes/system/config/config/ArchiveCodeIntegrityBootstrap.java",
            "src/main/java/com/paper/mes/system/config/config/DeliveryIntegrityBootstrap.java",
            "src/main/java/com/paper/mes/system/config/config/DocumentTrustIntegrityBootstrap.java",
            "src/main/java/com/paper/mes/system/config/config/DocumentSnapshotBootstrap.java",
            "src/main/java/com/paper/mes/system/config/config/OperationLogIntegrityBootstrap.java",
            "src/main/java/com/paper/mes/system/config/config/ProcessDraftIntegrityBootstrap.java",
            "src/main/java/com/paper/mes/system/config/config/ProcessOrderVoidIntegrityBootstrap.java",
            "src/main/java/com/paper/mes/system/config/config/ProcessParamIntegrityBootstrap.java",
            "src/main/java/com/paper/mes/system/config/config/ProcessRouteIntegrityBootstrap.java",
            "src/main/java/com/paper/mes/system/config/config/ReportIntegrityBootstrap.java",
            "src/main/java/com/paper/mes/system/config/config/SettleIntegrityBootstrap.java"
    );

    @Test
    void productionProfile_disablesRuntimeDdlAndRequiresExternalDatabasePassword() throws Exception {
        String production = source("src/main/resources/application-prod.example.yml");

        assertContainsAll(production,
                "schema-bootstrap:",
                "enabled: false",
                "password: ${PAPER_MES_DB_PASSWORD}",
                "cookie-secure: true");
        assertFalse(production.contains("CHANGE_ME_STRONG_PASSWORD"));
    }

    @Test
    void baseProfile_disablesRuntimeDdlByDefault() throws Exception {
        String application = source("src/main/resources/application.yml");
        String environment = source("deploy/paper-mes.env.example");

        assertContainsAll(application,
                "schema-bootstrap:",
                "enabled: ${PAPER_MES_SCHEMA_BOOTSTRAP_ENABLED:false}");
        assertContainsAll(environment, "PAPER_MES_SCHEMA_BOOTSTRAP_ENABLED=false");
    }

    @Test
    void ddlBootstraps_areDisabledWhenProductionSchemaBootstrapIsOff() throws Exception {
        for (String path : DDL_BOOTSTRAPS) {
            assertContainsAll(source(path),
                    "@ConditionalOnProperty",
                    "prefix = \"app.schema-bootstrap\"",
                    "havingValue = \"true\"");
        }
        assertContainsAll(source("src/main/java/com/paper/mes/auth/config/AuthBootstrap.java"),
                "if (schemaBootstrapEnabled())", "app.schema-bootstrap.enabled");
        assertContainsAll(source("src/main/java/com/paper/mes/system/config/config/SystemConfigBootstrap.java"),
                "if (schemaBootstrapEnabled)", "seedConfigItems();");
        assertContainsAll(source("src/main/java/com/paper/mes/system/config/config/SystemNoRuleBootstrap.java"),
                "if (schemaBootstrapEnabled)", "seedRules();");
    }

    @Test
    void migrations_coverInfrastructureNeededByDmlOnlyApplicationAccount() throws Exception {
        String runtime = source("sql/V2.8__add_runtime_safety_infrastructure.sql");
        String archive = source("sql/V2.9__add_active_archive_code_constraints.sql");
        String settlementDiscount = source("sql/V3.1__add_settlement_discount_amount.sql");
        String documentTrust = source("sql/V3.2__add_document_void_and_receive_idempotency.sql");

        assertContainsAll(runtime,
                "CREATE TABLE IF NOT EXISTS `sys_backup_task`",
                "CREATE TABLE IF NOT EXISTS `sys_notification`",
                "`stock_lock_status`",
                "`finish_uuid_active`",
                "idx_original_roll_machine_uuid");
        assertContainsAll(archive,
                "customer_code_active", "paper_code_active",
                "machine_code_active", "warehouse_code_active");
        assertContainsAll(settlementDiscount,
                "biz_settle_order", "biz_receive_record",
                "discount_amount", "chk_settle_discount_nonnegative",
                "chk_receive_discount_nonnegative");
        assertContainsAll(documentTrust,
                "biz_delivery_order", "biz_settle_order", "biz_receive_record",
                "void_reason", "void_by", "void_time", "request_id",
                "uk_receive_settle_request", "innodb_lock_wait_timeout");
    }

    @Test
    void freshDatabaseBaseline_containsAllRuntimeInfrastructure() throws Exception {
        String baseline = source("sql/01_schema_v4.1.sql");

        assertContainsAll(baseline,
                "CREATE TABLE `biz_process_config_draft`",
                "CREATE TABLE `sys_roll_no_sequence`",
                "CREATE TABLE `sys_dict_item`",
                "CREATE TABLE `sys_config_item`",
                "CREATE TABLE `sys_backup_task`",
                "CREATE TABLE `sys_notification`");
        assertContainsAll(baseline,
                "customer_code_active", "paper_code_active",
                "machine_code_active", "warehouse_code_active");
    }

    @Test
    void migrations_doNotSilentlyDeleteDuplicateBusinessRows() throws Exception {
        String runtime = source("sql/V2.8__add_runtime_safety_infrastructure.sql");
        String guide = source("docs/生产部署指南.md");
        String behaviorTest = source("deploy/test-runtime-migration-conflict.ps1");

        assertFalse(runtime.contains("duplicate_detail"));
        assertFalse(runtime.contains("SET d.is_deleted = 1"));
        assertContainsAll(runtime, "Never choose and delete duplicate business rows");
        assertContainsAll(guide, "迁移前数据检查", "重复占用必须先停止发布");
        assertContainsAll(behaviorTest,
                "paper_processing_migration_guard_test", "refusing to overwrite it",
                "Duplicate entry", "SUM(is_deleted)", "DROP DATABASE");
    }

    @Test
    void deploymentTemplates_enforceLeastPrivilegeAndHttps() throws Exception {
        String guide = source("docs/生产部署指南.md");
        String appGrant = guide.substring(
                guide.indexOf("CREATE USER IF NOT EXISTS 'paper_mes_app'"),
                guide.indexOf("CREATE USER IF NOT EXISTS 'paper_mes_migrator'"));
        String nginx = source("deploy/nginx-paper-mes-https.example.conf");

        assertContainsAll(appGrant, "GRANT SELECT, INSERT, UPDATE, DELETE");
        assertFalse(appGrant.contains("ALTER"));
        assertFalse(appGrant.contains("DROP"));
        assertContainsAll(nginx,
                "listen 443 ssl http2", "return 301 https://$host$request_uri",
                "ssl_protocols TLSv1.2 TLSv1.3", "Strict-Transport-Security",
                "limit_req zone=paper_mes_login");
        assertContainsAll(source("deploy/paper-mes.service.example"),
                "EnvironmentFile=/etc/paper-mes/paper-mes.env");
    }

    private String source(String path) throws Exception {
        return Files.readString(Path.of(path));
    }

    private void assertContainsAll(String source, String... fragments) {
        for (String fragment : fragments) {
            assertTrue(source.contains(fragment), "缺少生产安全约束: " + fragment);
        }
    }
}
