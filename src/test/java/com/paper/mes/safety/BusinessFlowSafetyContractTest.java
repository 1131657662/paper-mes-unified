package com.paper.mes.safety;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BusinessFlowSafetyContractTest {

    private static final String DELIVERY_SERVICE =
            "src/main/java/com/paper/mes/delivery/service/impl/DeliveryServiceImpl.java";
    private static final String SETTLE_SERVICE =
            "src/main/java/com/paper/mes/settle/service/impl/SettleServiceImpl.java";
    private static final String LOCK_SERVICE =
            "src/main/java/com/paper/mes/common/db/BusinessLockService.java";

    @Test
    void deliveryConfirm_whenChangingStockAndOrder_keepsLocksAndGuardedUpdates() throws IOException {
        String source = source(DELIVERY_SERVICE);

        assertContainsAll(slice(source, "public void confirm", "public void rollback"),
                "businessLockService.lockDeliveryOrder(uuid);",
                "businessLockService.lockFinishRolls",
                "updateFinishStatus",
                "buildDeliverySnapshot",
                "updateDeliveryForConfirm(order)");
        assertContainsAll(slice(source, "private void updateFinishStatus", "private void updateDeliveryForConfirm"),
                ".eq(FinishRoll::getFinishStatus, fromStatus)",
                ".setSql(\"version = version + 1\")");
        assertContainsAll(slice(source, "private void updateDeliveryForConfirm", "private void updateDeliveryForRollback"),
                ".eq(DeliveryOrder::getDeliveryStatus, DELIVERY_STATUS_PENDING)",
                ".set(DeliveryOrder::getSnapDelivery, order.getSnapDelivery())",
                ".setSql(\"version = version + 1\")");
    }

    @Test
    void deliveryRollbackAndChange_whenEditingPendingOrder_keepsStatusGuards() throws IOException {
        String source = source(DELIVERY_SERVICE);

        assertContainsAll(slice(source, "public void rollback", "public void appendDetails"),
                "businessLockService.lockDeliveryOrder(uuid);",
                "businessLockService.lockProcessOrders",
                "businessLockService.lockFinishRolls",
                "updateDeliveryForRollback(order)");
        assertContainsAll(slice(source, "public void appendDetails", "public void removeDetail"),
                "businessLockService.lockDeliveryOrder(uuid);",
                "businessLockService.lockFinishRolls",
                "order.getDeliveryStatus() != DELIVERY_STATUS_PENDING");
        assertContainsAll(slice(source, "public void removeDetail", "private String nextDeliveryNo"),
                "businessLockService.lockDeliveryOrder(uuid);",
                "order.getDeliveryStatus() != DELIVERY_STATUS_PENDING",
                "refreshTotals(order)");
        assertContainsAll(slice(source, "private void updateDeliveryForRollback", "private String currentOperator"),
                ".eq(DeliveryOrder::getDeliveryStatus, DELIVERY_STATUS_OUT)",
                ".set(DeliveryOrder::getDeliveryStatus, DELIVERY_STATUS_PENDING)",
                ".setSql(\"version = version + 1\")");
    }

    @Test
    void deliverySnapshot_whenGenerated_keepsTraceableFields() throws IOException {
        String source = source(DELIVERY_SERVICE);
        String snapshot = slice(source,
                "private String buildDeliverySnapshot", "private List<Map<String, Object>> buildDeliverySnapshotItems");
        String detailSnapshot = slice(source,
                "private List<Map<String, Object>> buildDeliverySnapshotItems", "private List<DeliveryDetailItemVO>");

        assertContainsAll(snapshot,
                "\"schema_version\"",
                "\"snapshot_type\"",
                "\"delivery_no\"",
                "\"customer_name\"",
                "\"car_no\"",
                "\"detail_items\"",
                "\"details\"");
        assertContainsAll(detailSnapshot,
                "\"order_no\"",
                "\"paper_name\"",
                "\"gram_weight\"",
                "\"finish_width\"",
                "\"actual_weight\"",
                "\"out_weight\"",
                "\"original_summary\"",
                "\"process_mode_text\"",
                "\"process_summary\"",
                "\"actual_remark\"");
    }

    @Test
    void settleCreate_whenLockingCandidates_reloadsAndRevalidatesOrders() throws IOException {
        String source = source(SETTLE_SERVICE);

        assertContainsAll(slice(source, "public String createByOrder", "public String createByOrders"),
                "businessLockService.lockProcessOrders(List.of(dto.getOrderUuid()))",
                "validateFinishedOrder(order)");
        assertContainsAll(slice(source, "public String createByOrders", "public String createByMonth"),
                "businessLockService.lockProcessOrders(dto.getOrderUuids())",
                "loadOrdersByUuid(dto.getOrderUuids())");
        assertContainsAll(slice(source, "public String createByMonth", "public SettleDetailVO getDetail"),
                "businessLockService.lockProcessOrders(orderUuids)",
                "orders = loadOrdersByUuid(orderUuids)");
        assertContainsAll(slice(source, "private List<ProcessOrder> loadOrdersByUuid", "private Customer resolveSingleCustomer"),
                "validateFinishedOrder(order)",
                "ordered.add(byUuid.get(uuid))");
        assertContainsAll(slice(source, "private void insertSettleDetail", "private void rollbackSettledProcessOrder"),
                "ConcurrencyGuard.requireRowUpdated(settleDetailMapper.insert(detail))",
                "DuplicateKeyException");
    }

    @Test
    void settleReceiveAndCancel_whenMoneyChanges_keepsLocksAndStatusGuards() throws IOException {
        String source = source(SETTLE_SERVICE);

        assertContainsAll(slice(source, "public void receive", "public void cancelReceive"),
                "businessLockService.lockSettleOrder(uuid);",
                "amount.compareTo(unreceived) > 0",
                "refreshReceiveState(settle)");
        assertContainsAll(slice(source, "public void cancelReceive", "public void voidSettle"),
                "businessLockService.lockSettleOrder(uuid);",
                "businessLockService.lockReceiveRecord(receiveUuid);",
                "updateReceiveRecordForCancel(record)",
                "refreshReceiveState(settle)");
        assertContainsAll(slice(source, "private void updateReceiveRecordForCancel", "private void updateSettleReceiveState"),
                ".eq(ReceiveRecord::getRecordStatus, RECEIVE_STATUS_ACTIVE)",
                ".setSql(\"version = version + 1\")");
        assertContainsAll(slice(source, "private void updateSettleReceiveState", "private BigDecimal activeReceiveAmount"),
                "wrapper.eq(SettleOrder::getSettleStatus, previousStatus)",
                ".setSql(\"version = version + 1\")");
    }

    @Test
    void settleVoidAndSnapshot_whenReversingSettle_keepsAuditBoundaries() throws IOException {
        String source = source(SETTLE_SERVICE);
        String snapshot = slice(source, "private String buildSettleSnapshot", "private List<Map<String, Object>> buildSettleSnapshotOrders");
        String sourceOrders = slice(source, "private List<Map<String, Object>> buildSettleSnapshotOrders",
                "private List<Map<String, Object>> buildSettleSnapshotDetails");
        String detailItems = slice(source, "private List<Map<String, Object>> buildSettleSnapshotDetails",
                "private List<SettleDetail> readSnapshotDetails");

        assertContainsAll(slice(source, "public void voidSettle", "private SettleDetail buildDetail"),
                "businessLockService.lockSettleOrder(uuid);",
                "activeReceiveAmount(uuid)",
                "businessLockService.lockProcessOrders",
                "rollbackSettledProcessOrder(order.getUuid())");
        assertContainsAll(slice(source, "private void rollbackSettledProcessOrder", "private String buildSettleSnapshot"),
                ".eq(ProcessOrder::getOrderStatus, ORDER_STATUS_SETTLED)",
                ".set(ProcessOrder::getOrderStatus, ORDER_STATUS_FINISHED)",
                ".setSql(\"version = version + 1\")");
        assertContainsAll(snapshot,
                "\"schema_version\"",
                "\"source_orders\"",
                "\"detail_items\"",
                "\"details\"",
                "\"print_line_items\"",
                "\"print_lines\"");
        assertContainsAll(sourceOrders,
                "\"order_no\"",
                "\"settle_type\"",
                "\"is_invoice\"",
                "\"tax_rate\"",
                "\"process_amount_no_tax\"",
                "\"process_amount_tax\"",
                "\"extra_amount_no_tax\"",
                "\"extra_amount_tax\"",
                "\"total_amount\"");
        assertContainsAll(detailItems,
                "\"order_no\"",
                "\"saw_amount\"",
                "\"rewind_amount\"",
                "\"extra_amount\"",
                "\"order_amount\"",
                "\"remark\"");
    }

    @Test
    void businessLocks_whenLockingManyRows_useStableOrderToReduceDeadlocks() throws IOException {
        assertContainsAll(source(LOCK_SERVICE),
                ".distinct()",
                ".sorted()",
                "FOR UPDATE");
    }

    private String source(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath), StandardCharsets.UTF_8);
    }

    private String slice(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        assertTrue(startIndex >= 0, "Missing start marker: " + start);
        int endIndex = source.indexOf(end, startIndex + start.length());
        assertTrue(endIndex >= 0, "Missing end marker: " + end);
        return source.substring(startIndex, endIndex);
    }

    private void assertContainsAll(String text, String... snippets) {
        for (String snippet : snippets) {
            assertTrue(text.contains(snippet), "Missing snippet: " + snippet);
        }
    }
}
