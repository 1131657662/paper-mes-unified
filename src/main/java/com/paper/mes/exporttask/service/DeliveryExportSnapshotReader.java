package com.paper.mes.exporttask.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DeliveryExportSnapshotReader {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public DeliveryExportSnapshotMetadata metadata(String snapshotUuid) {
        List<DeliveryExportSnapshotMetadata> rows = jdbcTemplate.query(
                """
                SELECT uuid, snapshot_type, captured_at, row_count
                FROM sys_export_snapshot WHERE uuid = ?
                """,
                (rs, rowNum) -> new DeliveryExportSnapshotMetadata(
                        rs.getString("uuid"), rs.getString("snapshot_type"),
                        rs.getTimestamp("captured_at").toLocalDateTime(), rs.getLong("row_count")),
                snapshotUuid);
        if (rows.isEmpty()) throw new BusinessException("导出数据快照不存在或已过期");
        return rows.getFirst();
    }

    public <T> List<T> rows(String snapshotUuid, long afterRowNo, int limit, Class<T> rowType) {
        return jdbcTemplate.query(
                """
                SELECT row_payload FROM sys_export_snapshot_row
                WHERE snapshot_uuid = ? AND row_no > ?
                ORDER BY row_no ASC LIMIT ?
                """,
                (rs, rowNum) -> read(rs.getString("row_payload"), rowType),
                snapshotUuid, afterRowNo, limit);
    }

    private <T> T read(String payload, Class<T> rowType) {
        try {
            return objectMapper.readValue(payload, rowType);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("导出数据快照损坏");
        }
    }
}
