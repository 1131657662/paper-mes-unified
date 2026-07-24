package com.paper.mes.health.service;

import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.common.BusinessException;
import com.paper.mes.health.dto.DataHealthRepairRequest;
import com.paper.mes.health.dto.DataHealthRepairResultVO;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.settle.service.SettleReceiveStatusResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class DataHealthRepairService {

    private static final Pattern ID_PATTERN = Pattern.compile("[0-9A-Za-z-]{1,36}");
    private final JdbcTemplate jdbcTemplate;
    private final OperationLogService operationLogService;

    @Transactional(rollbackFor = Exception.class)
    public DataHealthRepairResultVO reconcileSettlement(String uuid, DataHealthRepairRequest request) {
        validateId(uuid);
        SettlementRow settlement = settlement(uuid);
        validateConfirmation(request, settlement.number());
        SettlementAmounts amounts = settlementAmounts(uuid);
        ReceiveAmounts received = receiveAmounts(uuid);
        SettleReceiveStatusResolver.State state = SettleReceiveStatusResolver.resolve(
                amounts.total(), received.received());
        updateSettlement(settlement, amounts, received, state);
        recordRepair("结算单", uuid, settlement.number(), request.reason());
        return new DataHealthRepairResultVO(settlement.number(), "结算单金额和收款状态已重新对账");
    }

    @Transactional(rollbackFor = Exception.class)
    public DataHealthRepairResultVO restoreCompletedOrder(String uuid, DataHealthRepairRequest request) {
        validateId(uuid);
        ProcessOrderRow order = processOrder(uuid);
        validateConfirmation(request, order.number());
        if (order.status() != 5 || hasActiveSettlement(uuid)) {
            throw new BusinessException("当前加工单不符合恢复条件");
        }
        int updated = jdbcTemplate.update(RESTORE_ORDER_SQL,
                AuthContextHolder.currentDisplayName(), uuid, order.version());
        requireUpdated(updated);
        recordRepair("加工单", uuid, order.number(), request.reason());
        return new DataHealthRepairResultVO(order.number(), "加工单已从已结算恢复为已完成");
    }

    private SettlementRow settlement(String uuid) {
        return jdbcTemplate.query(SETTLEMENT_SQL, rs -> {
            if (!rs.next()) throw new BusinessException("结算单不存在");
            return new SettlementRow(uuid, rs.getString("settle_no"), rs.getInt("version"));
        }, uuid);
    }

    private SettlementAmounts settlementAmounts(String uuid) {
        return jdbcTemplate.query(SETTLEMENT_AMOUNTS_SQL, rs -> {
            if (!rs.next() || rs.getInt("detail_count") == 0) {
                throw new BusinessException("结算单没有有效明细，不能自动重算");
            }
            return new SettlementAmounts(rs.getBigDecimal("saw_amount"), rs.getBigDecimal("rewind_amount"),
                    rs.getBigDecimal("service_amount"), rs.getBigDecimal("extra_amount"),
                    rs.getBigDecimal("no_tax_amount"), rs.getBigDecimal("tax_amount"),
                    rs.getBigDecimal("total_amount"));
        }, uuid);
    }

    private ReceiveAmounts receiveAmounts(String uuid) {
        return jdbcTemplate.query(RECEIVE_AMOUNTS_SQL, rs -> {
            rs.next();
            return new ReceiveAmounts(rs.getBigDecimal("received_amount"),
                    rs.getBigDecimal("cash_amount"), rs.getBigDecimal("scrap_amount"),
                    rs.getBigDecimal("discount_amount"));
        }, uuid);
    }

    private ProcessOrderRow processOrder(String uuid) {
        return jdbcTemplate.query(PROCESS_ORDER_SQL, rs -> {
            if (!rs.next()) throw new BusinessException("加工单不存在");
            return new ProcessOrderRow(rs.getString("order_no"), rs.getInt("order_status"), rs.getInt("version"));
        }, uuid);
    }

    private void updateSettlement(SettlementRow row, SettlementAmounts amounts,
                                  ReceiveAmounts received, SettleReceiveStatusResolver.State state) {
        int updated = jdbcTemplate.update(UPDATE_SETTLEMENT_SQL,
                amounts.saw(), amounts.rewind(), amounts.service(), amounts.extra(), amounts.noTax(),
                amounts.tax(), amounts.total(),
                state.receivedAmount(), received.cash(), received.scrap(), received.discount(),
                state.unreceivedAmount(), state.status(), AuthContextHolder.currentDisplayName(),
                row.uuid(), row.version());
        requireUpdated(updated);
    }

    private boolean hasActiveSettlement(String orderUuid) {
        Integer count = jdbcTemplate.queryForObject(ACTIVE_SETTLEMENT_SQL, Integer.class, orderUuid);
        return count != null && count > 0;
    }

    private void validateConfirmation(DataHealthRepairRequest request, String businessNo) {
        if (!businessNo.equals(request.confirmation().trim())) {
            throw new BusinessException("确认单号与当前业务单不一致");
        }
    }

    private void validateId(String uuid) {
        if (uuid == null || !ID_PATTERN.matcher(uuid).matches()) {
            throw new BusinessException("业务编号格式不正确");
        }
    }

    private void requireUpdated(int updated) {
        if (updated != 1) throw new BusinessException("数据已被其他人修改，请重新扫描后再试");
    }

    private void recordRepair(String type, String uuid, String number, String reason) {
        operationLogService.record(type, uuid, number, OperationLogService.ACTION_DATA_REPAIR,
                null, "数据健康巡检修复：" + reason.trim());
    }

    private record SettlementRow(String uuid, String number, int version) { }
    private record ProcessOrderRow(String number, int status, int version) { }
    private record SettlementAmounts(BigDecimal saw, BigDecimal rewind, BigDecimal service, BigDecimal extra,
                                     BigDecimal noTax, BigDecimal tax, BigDecimal total) { }
    private record ReceiveAmounts(BigDecimal received, BigDecimal cash, BigDecimal scrap,
                                  BigDecimal discount) { }

    private static final String SETTLEMENT_SQL = """
            SELECT settle_no, version FROM biz_settle_order
            WHERE uuid = ? AND is_deleted = 0 AND settle_status IN (1, 2, 3) FOR UPDATE
            """;
    private static final String PROCESS_ORDER_SQL = """
            SELECT order_no, order_status, version FROM biz_process_order WHERE uuid = ? AND is_deleted = 0 FOR UPDATE
            """;
    private static final String ACTIVE_SETTLEMENT_SQL = """
            SELECT COUNT(*) FROM biz_settle_detail d
            INNER JOIN biz_settle_order s ON s.uuid = d.settle_uuid AND s.is_deleted = 0
            WHERE d.order_uuid = ? AND d.is_deleted = 0
            """;
    private static final String RESTORE_ORDER_SQL = """
            UPDATE biz_process_order SET order_status = 4, update_by = ?, update_time = NOW(), version = version + 1
            WHERE uuid = ? AND order_status = 5 AND version = ? AND is_deleted = 0
            """;
    private static final String SETTLEMENT_AMOUNTS_SQL = """
            SELECT COUNT(*) detail_count, COALESCE(SUM(d.saw_amount), 0) saw_amount,
                   COALESCE(SUM(d.rewind_amount), 0) rewind_amount,
                   COALESCE(SUM(d.service_amount), 0) service_amount,
                   COALESCE(SUM(d.extra_amount), 0) extra_amount,
                   COALESCE(SUM(COALESCE(d.saw_amount, 0) + COALESCE(d.rewind_amount, 0)
                       + COALESCE(d.service_amount, 0) + COALESCE(d.extra_amount, 0)), 0) no_tax_amount,
                   COALESCE(SUM(d.order_amount), 0)
                       - COALESCE(SUM(COALESCE(d.saw_amount, 0) + COALESCE(d.rewind_amount, 0)
                           + COALESCE(d.service_amount, 0) + COALESCE(d.extra_amount, 0)), 0) tax_amount,
                   COALESCE(SUM(d.order_amount), 0) total_amount
            FROM biz_settle_detail d
            WHERE d.settle_uuid = ? AND d.is_deleted = 0
            """;
    private static final String RECEIVE_AMOUNTS_SQL = """
            SELECT COALESCE(SUM(receive_amount), 0) received_amount,
                   COALESCE(SUM(cash_amount), 0) cash_amount,
                   COALESCE(SUM(scrap_offset_amount), 0) scrap_amount,
                   COALESCE(SUM(discount_amount), 0) discount_amount
            FROM biz_receive_record WHERE settle_uuid = ? AND is_deleted = 0 AND record_status = 1
            """;
    private static final String UPDATE_SETTLEMENT_SQL = """
            UPDATE biz_settle_order SET saw_amount = ?, rewind_amount = ?, service_amount = ?,
                   extra_amount = ?, amount_no_tax = ?, tax_amount = ?, total_amount = ?, received_amount = ?,
                   cash_received_amount = ?, scrap_offset_amount = ?, discount_amount = ?,
                   unreceived_amount = ?, settle_status = ?,
                   update_by = ?, update_time = NOW(), version = version + 1
            WHERE uuid = ? AND version = ? AND is_deleted = 0
            """;
}
