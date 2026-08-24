package com.paper.mes.remain.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
@RequiredArgsConstructor
public class RemainLockService {

    private final JdbcTemplate jdbcTemplate;

    public void lockRegistration(String uuid) {
        jdbcTemplate.queryForList("""
                SELECT uuid
                FROM biz_remain_registration
                WHERE uuid = ? AND is_deleted = 0
                FOR UPDATE
                """, uuid);
    }

    public void lockLines(Collection<String> lineUuids) {
        lineUuids.stream().filter(this::present).distinct().sorted().forEach(uuid ->
                jdbcTemplate.queryForList("""
                        SELECT uuid
                        FROM biz_remain_registration_line
                        WHERE uuid = ? AND is_deleted = 0
                        FOR UPDATE
                        """, uuid));
    }

    public void lockLots(Collection<String> lotUuids) {
        lotUuids.stream().filter(this::present).distinct().sorted().forEach(uuid ->
                jdbcTemplate.queryForList("""
                        SELECT uuid
                        FROM biz_remain_inventory_lot
                        WHERE uuid = ? AND is_deleted = 0
                        FOR UPDATE
                        """, uuid));
    }

    public void lockAdjustment(String uuid) {
        jdbcTemplate.queryForList("""
                SELECT uuid
                FROM biz_remain_adjustment
                WHERE uuid = ?
                FOR UPDATE
                """, uuid);
    }

    public void lockCreditAccount(String customerUuid) {
        jdbcTemplate.queryForList("""
                SELECT uuid
                FROM biz_remain_customer_credit_account
                WHERE customer_uuid = ? AND is_deleted = 0
                FOR UPDATE
                """, customerUuid);
    }

    public void lockCreditLedger(String uuid) {
        jdbcTemplate.queryForList("""
                SELECT uuid
                FROM biz_remain_customer_credit_ledger
                WHERE uuid = ?
                FOR UPDATE
                """, uuid);
    }

    public void lockRefund(String uuid) {
        jdbcTemplate.queryForList("""
                SELECT uuid
                FROM biz_remain_refund
                WHERE uuid = ?
                FOR UPDATE
                """, uuid);
    }

    private boolean present(String value) {
        return value != null && !value.isBlank();
    }
}
