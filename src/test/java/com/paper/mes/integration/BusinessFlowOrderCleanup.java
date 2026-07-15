package com.paper.mes.integration;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

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
        String customerUuid = jdbcTemplate.query(
                "SELECT customer_uuid FROM biz_process_order WHERE uuid = ?",
                result -> result.next() ? result.getString(1) : null,
                orderUuid);
        for (String table : CHILD_TABLES) {
            jdbcTemplate.update("DELETE FROM " + table + " WHERE order_uuid = ?", orderUuid);
        }
        jdbcTemplate.update("DELETE FROM biz_process_order WHERE uuid = ?", orderUuid);
        if (customerUuid != null) {
            jdbcTemplate.update("DELETE FROM sys_customer WHERE uuid = ?", customerUuid);
        }
    }
}
