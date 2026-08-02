package com.paper.mes.integration;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Component
@RequiredArgsConstructor
class BusinessFlowOrderCleanup {

    private static final List<String> CHILD_TABLES = List.of(
            "biz_finish_original_rel",
            "biz_process_stage_input_rel",
            "biz_process_stage_output",
            "biz_process_param",
            "biz_process_step",
            "biz_finish_roll",
            "biz_process_config_draft",
            "biz_original_roll"
    );

    private final JdbcTemplate jdbcTemplate;

    void delete(String orderUuid) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            return;
        }
        OrderOwners owners = jdbcTemplate.query(
                "SELECT customer_uuid, warehouse_uuid FROM biz_process_order WHERE uuid = ?",
                result -> result.next() ? new OrderOwners(result.getString(1), result.getString(2)) : null,
                orderUuid);
        for (String table : CHILD_TABLES) {
            jdbcTemplate.update("DELETE FROM " + table + " WHERE order_uuid = ?", orderUuid);
        }
        jdbcTemplate.update("DELETE FROM biz_process_order WHERE uuid = ?", orderUuid);
        if (owners != null && owners.customerUuid() != null) {
            jdbcTemplate.update("DELETE FROM sys_customer WHERE uuid = ?", owners.customerUuid());
        }
        if (owners != null && owners.warehouseUuid() != null) {
            jdbcTemplate.update("DELETE FROM sys_warehouse WHERE uuid = ? AND warehouse_code LIKE 'IT-WH-%'",
                    owners.warehouseUuid());
        }
    }

    private record OrderOwners(String customerUuid, String warehouseUuid) {
    }
}
