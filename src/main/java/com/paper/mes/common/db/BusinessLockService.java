package com.paper.mes.common.db;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BusinessLockService {

    private final JdbcTemplate jdbcTemplate;

    public void lockDeliveryOrder(String uuid) {
        lockOne("biz_delivery_order", uuid);
    }

    public void lockSettleOrder(String uuid) {
        lockOne("biz_settle_order", uuid);
    }

    public void lockReceiveRecord(String uuid) {
        lockOne("biz_receive_record", uuid);
    }

    public void lockProcessOrders(Collection<String> uuids) {
        lockMany("biz_process_order", uuids);
    }

    public void lockFinishRolls(Collection<String> uuids) {
        lockMany("biz_finish_roll", uuids);
    }

    private void lockOne(String tableName, String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return;
        }
        jdbcTemplate.queryForList("""
                SELECT uuid
                FROM %s
                WHERE uuid = ?
                FOR UPDATE
                """.formatted(tableName), uuid);
    }

    private void lockMany(String tableName, Collection<String> uuids) {
        if (uuids == null || uuids.isEmpty()) {
            return;
        }
        List<String> ordered = uuids.stream()
                .filter(uuid -> uuid != null && !uuid.isBlank())
                .distinct()
                .sorted()
                .toList();
        for (String uuid : ordered) {
            lockOne(tableName, uuid);
        }
    }
}
