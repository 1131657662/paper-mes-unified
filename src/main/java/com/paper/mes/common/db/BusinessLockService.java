package com.paper.mes.common.db;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.time.LocalDate;

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

    /** Locks the complete finished-order range used by a monthly settlement. */
    public List<String> lockMonthlyFinishedProcessOrders(String customerUuid,
                                                          LocalDate periodStart,
                                                          LocalDate periodEnd) {
        if (customerUuid == null || customerUuid.isBlank()
                || periodStart == null || periodEnd == null) {
            return List.of();
        }
        return jdbcTemplate.query("""
                SELECT uuid
                FROM biz_process_order FORCE INDEX (idx_order_customer_status_accounting)
                WHERE customer_uuid = ?
                  AND order_status = 4
                  AND is_deleted = 0
                  AND accounting_date BETWEEN ? AND ?
                ORDER BY accounting_date ASC, order_no ASC, uuid ASC
                FOR UPDATE
                """, (resultSet, rowNumber) -> resultSet.getString("uuid"),
                customerUuid, periodStart, periodEnd);
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
