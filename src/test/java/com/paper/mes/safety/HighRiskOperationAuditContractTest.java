package com.paper.mes.safety;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class HighRiskOperationAuditContractTest {

    private static final String PROCESS_ORDER_SERVICE =
            "src/main/java/com/paper/mes/processorder/service/impl/ProcessOrderServiceImpl.java";
    private static final String DELIVERY_SERVICE =
            "src/main/java/com/paper/mes/delivery/service/impl/DeliveryServiceImpl.java";
    private static final String SETTLE_SERVICE =
            "src/main/java/com/paper/mes/settle/service/impl/SettleServiceImpl.java";
    private static final String BACKUP_EXECUTOR =
            "src/main/java/com/paper/mes/backup/service/BackupTaskExecutor.java";
    private static final String BACKUP_MAINTENANCE =
            "src/main/java/com/paper/mes/backup/service/BackupMaintenanceService.java";
    private static final String DATA_REPAIR_SERVICE =
            "src/main/java/com/paper/mes/health/service/DataHealthRepairService.java";

    @Test
    void processOrderHighRiskActions_areAudited() throws IOException {
        String source = source(PROCESS_ORDER_SERVICE);

        assertAudit(slice(source, "public void changeStatus", "public void voidOrder"),
                "OperationLogService.ACTION_ROLLBACK");
        assertAudit(slice(source, "private void markOrderVoided", "private void ensureOrderNotReferencedBySettle"),
                "OperationLogService.ACTION_VOID_ORDER");
        assertAudit(slice(source, "public PrintResultVO print", "private String buildSnapPrint"),
                "OperationLogService.ACTION_REPRINT");
        assertAudit(slice(source, "public BackRecordResultVO backRecord", "private void authorizeBlockRelease"),
                "OperationLogService.ACTION_BACK_RECORD");
    }

    @Test
    void deliveryHighRiskActions_areAudited() throws IOException {
        String source = source(DELIVERY_SERVICE);

        assertAudit(slice(source, "public void confirm", "public void rollback"),
                "OperationLogService.ACTION_DELIVERY_CONFIRM");
        assertAudit(slice(source, "public void rollback", "public void appendDetails"),
                "OperationLogService.ACTION_ROLLBACK");
        assertAudit(slice(source, "public void cancelPending", "private String nextDeliveryNo"),
                "OperationLogService.ACTION_DELIVERY_CANCEL");
    }

    @Test
    void settlementHighRiskActions_areAudited() throws IOException {
        String source = source(SETTLE_SERVICE);

        assertAudit(slice(source, "public void receive", "public void cancelReceive"),
                "OperationLogService.ACTION_RECEIVE");
        assertAudit(slice(source, "public void cancelReceive", "public void voidSettle"),
                "OperationLogService.ACTION_RECEIVE_CANCEL");
        assertAudit(slice(source, "public void voidSettle", "private SettleDetail buildDetail"),
                "OperationLogService.ACTION_SETTLE_VOID");
    }

    @Test
    void dataSafetyHighRiskActions_areAudited() throws IOException {
        String executor = source(BACKUP_EXECUTOR);
        String maintenance = source(BACKUP_MAINTENANCE);

        assertAudit(slice(executor, "public void startBackup", "public void startAutomaticBackup"),
                "OperationLogService.ACTION_BACKUP");
        assertAudit(slice(executor, "public void startVerification", "public boolean isRunning"),
                "OperationLogService.ACTION_BACKUP_VERIFY");
        assertAudit(slice(maintenance, "private void deleteBackup", "private void acquire"),
                "OperationLogService.ACTION_BACKUP_DELETE");
        assertAudit(slice(maintenance, "private int cleanup", "private void deleteDirectory"),
                "OperationLogService.ACTION_BACKUP_CLEANUP");
        assertAudit(source(DATA_REPAIR_SERVICE), "OperationLogService.ACTION_DATA_REPAIR");
    }

    private void assertAudit(String source, String action) {
        assertThat(source).contains("operationLogService.record", action);
    }

    private String source(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }

    private String slice(String source, String start, String end) {
        int from = source.indexOf(start);
        int to = source.indexOf(end, from + start.length());
        assertThat(from).as("start marker %s", start).isGreaterThanOrEqualTo(0);
        assertThat(to).as("end marker %s", end).isGreaterThan(from);
        return source.substring(from, to);
    }
}
