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
                "confirmFinishStock",
                "updateDetailStockLocks(details, STOCK_LOCK_ACTIVE, STOCK_LOCK_RELEASED)",
                "buildDeliverySnapshot",
                "updateDeliveryForConfirm(order)");
        assertContainsAll(slice(source, "private void confirmFinishStock", "private void updateDeliveryForConfirm"),
                "DeliveryStockPolicy.remainingAfterConfirm",
                ".eq(FinishRoll::getFinishStatus, FINISH_STATUS_IN_STOCK)",
                ".set(FinishRoll::getRemainingWeight, remaining)",
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
                "buildRollbackSnapshot(order, rollbackReason, rollbackOperator, rollbackTime)",
                "rollbackFinishStock",
                "updateDetailStockLocks(details, STOCK_LOCK_RELEASED, STOCK_LOCK_ACTIVE)",
                "updateDeliveryForRollback(order)");
        assertContainsAll(slice(source, "public void appendDetails", "public void removeDetail"),
                "businessLockService.lockDeliveryOrder(uuid);",
                "businessLockService.lockFinishRolls",
                "order.getDeliveryStatus() != DELIVERY_STATUS_PENDING");
        assertContainsAll(slice(source, "public void removeDetail", "private String nextDeliveryNo"),
                "businessLockService.lockDeliveryOrder(uuid);",
                "order.getDeliveryStatus() != DELIVERY_STATUS_PENDING",
                ".eq(DeliveryDetail::getUuid, detailUuid)",
                ".eq(DeliveryDetail::getDeliveryUuid, uuid)",
                "refreshTotals(order)");
        assertContainsAll(slice(source, "private void refreshTotals", "private void updateFinishStatus"),
                ".eq(DeliveryOrder::getDeliveryStatus, DELIVERY_STATUS_PENDING)",
                ".set(DeliveryOrder::getTotalCount, order.getTotalCount())",
                ".set(DeliveryOrder::getTotalWeight, order.getTotalWeight())",
                ".setSql(\"version = version + 1\")");
        assertContainsAll(slice(source, "private void updateDeliveryForRollback", "private String currentOperator"),
                ".eq(DeliveryOrder::getDeliveryStatus, DELIVERY_STATUS_OUT)",
                ".set(DeliveryOrder::getDeliveryStatus, DELIVERY_STATUS_PENDING)",
                ".set(DeliveryOrder::getSnapDelivery, order.getSnapDelivery())",
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
                "\"remaining_weight\"",
                "\"out_weight\"",
                "\"original_summary\"",
                "\"process_mode_text\"",
                "\"process_summary\"",
                "\"original_items\"",
                "\"process_step_items\"",
                "\"machine_names\"",
                "\"actual_remark\"");
        assertContainsAll(slice(source, "private String buildRollbackSnapshot", "private List<Map<String, Object>> buildDeliverySnapshotItems"),
                "\"snapshot_type\"",
                "\"delivery_rollback\"",
                "\"rollback_reason\"",
                "\"rollback_operator\"",
                "\"rollback_time\"",
                "\"previous_confirm_snapshot\"");
        assertContainsAll(slice(source, "private List<DeliveryDetailItemVO> readSnapshotDeliveryItems", "private DeliveryOrder snapshotDeliveryOrder"),
                "isRollbackSnapshot(snapDelivery)",
                "return null");
        assertContainsAll(slice(source, "private DeliveryOrder snapshotDeliveryOrder", "private DeliveryOrder copyDeliveryOrder"),
                "isSnapshotType(root, \"delivery_rollback\")",
                "return view");
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
        assertContainsAll(slice(source, "private String createFromOrders", "private void ensureOrderNotSettled"),
                "applyReceiveState(settle, amounts.total(), BigDecimal.ZERO)");
    }

    @Test
    void settleAmounts_whenBuiltFromProcessOrder_useLockedProcessOrderAmounts() throws IOException {
        String source = source(SETTLE_SERVICE);

        assertContainsAll(slice(source, "private SettleDetail buildDetail",
                        "private List<SettleDetail> normalizeDetailsForInvoiceView"),
                "d.setOrderAmount(settleOrderAmount(order, fallbackAmount))");
        assertContainsAll(slice(source, "private SettleDetail normalizedDetail",
                        "private void applySettlementAmountView"),
                "detail.setOrderAmount(settleOrderAmount(order, fallbackAmount))");
        assertContainsAll(slice(source, "private SettlementAmounts sumAmounts",
                        "private BigDecimal detailBaseAmount"),
                "noTax = noTax.add(settleNoTaxAmount(order, baseAmount))",
                "tax = tax.add(settleTaxAmount(order, fallbackTax))",
                "total = total.add(detail.getOrderAmount())");
        assertContainsAll(slice(source, "private BigDecimal settleOrderAmount",
                        "private BigDecimal detailBaseAmount"),
                "order.getTotalAmount()",
                "order.getTotalAmountNoTax()",
                "order.getTotalAmountTax()");
    }

    @Test
    void settleReceiveAndCancel_whenMoneyChanges_keepsLocksAndStatusGuards() throws IOException {
        String source = source(SETTLE_SERVICE);

        assertContainsAll(slice(source, "public void receive", "public void cancelReceive"),
                "businessLockService.lockSettleOrder(uuid);",
                "SettleReceiveAmountResolver.resolve(dto, unreceived)",
                "record.setReceiveAmount(amount.receiveAmount())",
                "record.setCashAmount(amount.cashAmount())",
                "record.setScrapOffsetAmount(amount.scrapOffsetAmount())",
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
        assertContainsAll(slice(source, "private void refreshReceiveState", "private void updateReceiveRecordForCancel"),
                "applyReceiveState(settle, total, activeReceiveTotals(settle.getUuid()))");
        assertContainsAll(slice(source, "private void applyReceiveState", "private void updateReceiveRecordForCancel"),
                "SettleReceiveStatusResolver.resolve(totalAmount, totals.receiveAmount())",
                "settle.setCashReceivedAmount(totals.cashAmount())",
                "settle.setScrapOffsetAmount(totals.scrapOffsetAmount())");
        assertContainsAll(slice(source, "private BigDecimal activeReceiveAmount", "private List<SettleDetail> settleDetails"),
                ".eq(ReceiveRecord::getIsDeleted, 0)",
                ".eq(ReceiveRecord::getSettleUuid, settleUuid)",
                "totals = totals.add(record)");
    }

    @Test
    void settleVoidAndSnapshot_whenReversingSettle_keepsAuditBoundaries() throws IOException {
        String source = source(SETTLE_SERVICE);
        String snapshot = slice(source, "private String buildSettleSnapshot", "private List<Map<String, Object>> buildSettleSnapshotOrders");
        String sourceOrders = slice(source, "private List<Map<String, Object>> buildSettleSnapshotOrders",
                "private List<Map<String, Object>> buildSettleSnapshotDetails");
        String detailItems = slice(source, "private List<Map<String, Object>> buildSettleSnapshotDetails",
                "private List<SettleDetail> readSnapshotDetails");
        String printLineItems = slice(source, "private SettlePrintLineVO buildPrintLine",
                "private void applyOrderAmountClosure");

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
        assertContainsAll(slice(source, "private List<SettlePrintLineVO> readSnapshotPrintLines", "private SettleOrder snapshotSettleOrder"),
                "SettleSnapshotPrintLineReader.read(snapBill, objectMapper)");
        assertContainsAll(sourceOrders,
                "\"order_no\"",
                "\"settle_type\"",
                "\"is_invoice\"",
                "\"tax_rate\"",
                "\"urgent_fee\"",
                "\"pallet_fee\"",
                "\"loading_fee\"",
                "\"freight_fee\"",
                "\"other_fee\"",
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
        assertContainsAll(printLineItems,
                "line.setOriginalRollNo",
                "line.setOriginalExtraNo",
                "line.setActualGramWeight",
                "line.setActualWidth",
                "line.setOriginalDiameter",
                "line.setCoreDiameter",
                "line.setOriginalLength",
                "line.setProcessStepSummary",
                "line.setFinishDetailSummary",
                "line.setTrimSummary",
                "line.setMachineUuid",
                "line.setMachineName",
                "line.setTaxRate");
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
