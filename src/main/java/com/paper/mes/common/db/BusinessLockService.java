package com.paper.mes.common.db;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.common.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;
import java.util.List;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class BusinessLockService {

    private static final String INVENTORY_SWITCH_LOCK = "paper_mes_inventory_switch";

    private final JdbcTemplate jdbcTemplate;

    public void lockDeliveryOrder(String uuid) {
        lockOne("biz_delivery_order", uuid);
    }

    public void lockSettleOrder(String uuid) {
        lockOne("biz_settle_order", uuid);
    }

    public void lockSettleOrders(Collection<String> uuids) {
        lockMany("biz_settle_order", uuids);
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

    public void lockInventorySwitch() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new BusinessException(ResultCode.CONFLICT, ErrorCode.E004.getCode(),
                    "inventory switch lock requires an active transaction");
        }
        if (TransactionSynchronizationManager.hasResource(INVENTORY_SWITCH_LOCK)) {
            return;
        }
        Integer acquired = jdbcTemplate.queryForObject(
                "SELECT GET_LOCK(?, 30)", Integer.class, INVENTORY_SWITCH_LOCK);
        if (!Integer.valueOf(1).equals(acquired)) {
            throw new BusinessException(ResultCode.CONFLICT, ErrorCode.E004.getCode(),
                    "inventory switch lock could not be acquired");
        }
        TransactionSynchronizationManager.bindResource(INVENTORY_SWITCH_LOCK, Boolean.TRUE);
        try {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    releaseInventorySwitchLock();
                }
            });
        } catch (RuntimeException exception) {
            TransactionSynchronizationManager.unbindResourceIfPossible(INVENTORY_SWITCH_LOCK);
            releaseInventorySwitchLock();
            throw exception;
        }
    }

    private void releaseInventorySwitchLock() {
        if (TransactionSynchronizationManager.hasResource(INVENTORY_SWITCH_LOCK)) {
            TransactionSynchronizationManager.unbindResourceIfPossible(INVENTORY_SWITCH_LOCK);
        }
        jdbcTemplate.queryForObject("SELECT RELEASE_LOCK(?)", Integer.class, INVENTORY_SWITCH_LOCK);
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
