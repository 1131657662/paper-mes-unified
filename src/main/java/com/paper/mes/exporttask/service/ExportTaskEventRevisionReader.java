package com.paper.mes.exporttask.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ExportTaskEventRevisionReader {
    private static final String EMPTY_REVISION = "0:0";
    private final JdbcTemplate jdbcTemplate;

    public Map<String, String> read(Set<String> userUuids) {
        if (userUuids == null || userUuids.isEmpty()) return Map.of();
        Map<String, String> revisions = defaults(userUuids);
        String placeholders = String.join(",", Collections.nCopies(userUuids.size(), "?"));
        String sql = "SELECT requester_uuid, COUNT(*) AS task_count, "
                + "COALESCE(SUM(version), 0) AS version_sum FROM sys_export_task "
                + "WHERE is_deleted = 0 AND requester_uuid IN (" + placeholders + ") "
                + "GROUP BY requester_uuid";
        jdbcTemplate.queryForList(sql, userUuids.toArray()).forEach(row -> {
            String userUuid = String.valueOf(row.get("requester_uuid"));
            revisions.put(userUuid, signature(row));
        });
        return revisions;
    }

    private Map<String, String> defaults(Set<String> userUuids) {
        Map<String, String> revisions = new LinkedHashMap<>();
        userUuids.forEach(userUuid -> revisions.put(userUuid, EMPTY_REVISION));
        return revisions;
    }

    private String signature(Map<String, Object> row) {
        Number count = (Number) row.get("task_count");
        Number versionSum = (Number) row.get("version_sum");
        return number(count) + ":" + number(versionSum);
    }

    private long number(Number value) {
        return value == null ? 0 : value.longValue();
    }
}
