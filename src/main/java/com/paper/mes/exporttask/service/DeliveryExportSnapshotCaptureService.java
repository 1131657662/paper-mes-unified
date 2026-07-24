package com.paper.mes.exporttask.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.delivery.dto.DeliveryInventoryFinishQuery;
import com.paper.mes.delivery.dto.DeliveryInventoryFinishVO;
import com.paper.mes.delivery.dto.DeliveryListExportRow;
import com.paper.mes.delivery.dto.DeliveryQuery;
import com.paper.mes.delivery.entity.DeliveryOrder;
import com.paper.mes.delivery.mapper.DeliveryInventoryMapper;
import com.paper.mes.delivery.mapper.DeliveryOrderMapper;
import com.paper.mes.delivery.service.impl.DeliveryOrderQueryBuilder;
import com.paper.mes.exporttask.entity.ExportTask;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

@Component
@RequiredArgsConstructor
public class DeliveryExportSnapshotCaptureService {

    private static final int BATCH_SIZE = 500;

    private final DeliveryInventoryMapper inventoryMapper;
    private final DeliveryOrderMapper deliveryOrderMapper;
    private final JdbcTemplate jdbcTemplate;
    private final ExportTaskPayloadWriter payloadWriter;

    public void captureInventory(ExportTask task, DeliveryInventoryFinishQuery query) {
        createSnapshot(task, DeliveryExportSnapshotMetadata.INVENTORY_TYPE);
        long rowNo = 0;
        while (true) {
            List<DeliveryInventoryFinishVO> rows = inventoryMapper.finishRows(query, rowNo, BATCH_SIZE);
            appendRows(task.getQuerySnapshotUuid(), rowNo, rows);
            rowNo += rows.size();
            if (rows.size() < BATCH_SIZE) break;
        }
        complete(task.getQuerySnapshotUuid(), rowNo);
    }

    public void captureReconciliation(ExportTask task, DeliveryQuery query) {
        createSnapshot(task, DeliveryExportSnapshotMetadata.RECONCILIATION_TYPE);
        long rowNo = 0;
        long current = 1;
        while (true) {
            Page<DeliveryOrder> page = deliveryOrderMapper.selectPage(
                    Page.of(current++, BATCH_SIZE, false), DeliveryOrderQueryBuilder.build(query));
            List<DeliveryListExportRow> rows = page.getRecords().stream()
                    .map(DeliveryListExportRow::from)
                    .toList();
            appendRows(task.getQuerySnapshotUuid(), rowNo, rows);
            rowNo += page.getRecords().size();
            if (page.getRecords().size() < BATCH_SIZE) break;
        }
        complete(task.getQuerySnapshotUuid(), rowNo);
    }

    private void createSnapshot(ExportTask task, String snapshotType) {
        ConcurrencyGuard.requireRowUpdated(jdbcTemplate.update(
                """
                INSERT INTO sys_export_snapshot
                    (uuid, task_uuid, snapshot_type, captured_at, row_count, create_time)
                VALUES (?, ?, ?, ?, 0, ?)
                """, task.getQuerySnapshotUuid(), task.getUuid(), snapshotType,
                LocalDateTime.now(), LocalDateTime.now()));
    }

    private void appendRows(String snapshotUuid, long rowNo, List<?> rows) {
        if (rows.isEmpty()) return;
        int[] counts = jdbcTemplate.batchUpdate(
                "INSERT INTO sys_export_snapshot_row (snapshot_uuid, row_no, row_payload) VALUES (?, ?, ?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement statement, int index) throws SQLException {
                        statement.setString(1, snapshotUuid);
                        statement.setLong(2, rowNo + index + 1);
                        statement.setString(3, payloadWriter.write(rows.get(index)));
                    }

                    @Override
                    public int getBatchSize() {
                        return rows.size();
                    }
                });
        boolean persisted = counts.length == rows.size() && Arrays.stream(counts)
                .allMatch(count -> count > 0 || count == Statement.SUCCESS_NO_INFO);
        ConcurrencyGuard.requireRowUpdated(persisted ? 1 : 0);
    }

    private void complete(String snapshotUuid, long rowCount) {
        ConcurrencyGuard.requireRowUpdated(jdbcTemplate.update(
                "UPDATE sys_export_snapshot SET row_count = ? WHERE uuid = ?", rowCount, snapshotUuid));
    }
}
