package com.paper.mes.safety;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusinessFlowConcurrencyContractTest {

    private static final String DELIVERY_SERVICE =
            "src/main/java/com/paper/mes/delivery/service/impl/DeliveryServiceImpl.java";
    private static final String SETTLE_SERVICE =
            "src/main/java/com/paper/mes/settle/service/impl/SettleServiceImpl.java";
    private static final String PROCESS_ORDER_SERVICE =
            "src/main/java/com/paper/mes/processorder/service/impl/ProcessOrderServiceImpl.java";
    private static final String DELIVERY_INTEGRITY_BOOTSTRAP =
            "src/main/java/com/paper/mes/system/config/config/DeliveryIntegrityBootstrap.java";

    @Test
    void deliveryRollback_whenOrderIsSettled_blocksBeforeReturningStock() throws IOException {
        String source = source(DELIVERY_SERVICE);
        String rollback = slice(source, "public void rollback", "public void appendDetails");

        assertContainsAll(rollback,
                "businessLockService.lockProcessOrders",
                "businessLockService.lockFinishRolls",
                "ensureOrdersNotSettled(details)",
                "rollbackFinishStock(finish, detail)",
                "updateDetailStockLocks(details, STOCK_LOCK_RELEASED, STOCK_LOCK_ACTIVE)");
        assertBefore(rollback,
                "ensureOrdersNotSettled(details)",
                "rollbackFinishStock(finish, detail)");

        assertContainsAll(slice(source, "private void ensureOrdersNotSettled", "private String originalSummary"),
                "settleDetailMapper.selectCount",
                ".eq(SettleDetail::getIsDeleted, 0)",
                ".in(SettleDetail::getOrderUuid, orderUuids)",
                "throw new BusinessException(ErrorCode.E004, \"关联加工单已生成结算单，不可回退出库\")");
    }

    @Test
    void deliveryChange_whenEditingPendingOrder_keepsReservationGuards() throws IOException {
        String source = source(DELIVERY_SERVICE);
        String append = slice(source, "public void appendDetails", "public void removeDetail");
        String appendGuard = slice(source, "private List<String> validateAppendFinishUuids",
                "private Map<String, FinishRoll> loadFinishRollsByUuid");
        String remove = slice(source, "public void removeDetail", "private String nextDeliveryNo");

        assertContainsAll(append,
                "businessLockService.lockDeliveryOrder(uuid);",
                "businessLockService.lockFinishRolls",
                "order.getDeliveryStatus() == null || order.getDeliveryStatus() != DELIVERY_STATUS_PENDING",
                "validateAppendFinishUuids(dto.getItems(), existingFinishUuids, lockedFinishUuids)",
                "refreshTotals(order)");
        assertContainsAll(appendGuard,
                "requestFinishUuids.add(item.getFinishUuid())",
                "existingFinishUuids.contains(item.getFinishUuid())",
                "lockedFinishUuids.contains(item.getFinishUuid())");
        assertBefore(append,
                "order.getDeliveryStatus() == null || order.getDeliveryStatus() != DELIVERY_STATUS_PENDING",
                "insertDeliveryDetail(detail)");

        assertContainsAll(remove,
                "businessLockService.lockDeliveryOrder(uuid);",
                "order.getDeliveryStatus() == null || order.getDeliveryStatus() != DELIVERY_STATUS_PENDING",
                "deliveryDetailMapper.delete(new LambdaQueryWrapper<DeliveryDetail>()",
                ".eq(DeliveryDetail::getUuid, detailUuid)",
                ".eq(DeliveryDetail::getDeliveryUuid, uuid)",
                "refreshTotals(order)");
        assertBefore(remove,
                "order.getDeliveryStatus() == null || order.getDeliveryStatus() != DELIVERY_STATUS_PENDING",
                "deliveryDetailMapper.delete(new LambdaQueryWrapper<DeliveryDetail>()");

        assertContainsAll(slice(source, "private void refreshTotals", "private void updateFinishStatus"),
                ".eq(DeliveryOrder::getDeliveryStatus, DELIVERY_STATUS_PENDING)",
                ".setSql(\"version = version + 1\")");
    }

    @Test
    void deliveryConfirmAndRollback_whenSubmittedRepeatedly_useStatusGuardedUpdates() throws IOException {
        String source = source(DELIVERY_SERVICE);

        assertContainsAll(slice(source, "private void updateDeliveryForConfirm", "private void updateDeliveryForRollback"),
                "ConcurrencyGuard.requireRowUpdated",
                ".eq(DeliveryOrder::getUuid, order.getUuid())",
                ".eq(DeliveryOrder::getDeliveryStatus, DELIVERY_STATUS_PENDING)",
                ".set(DeliveryOrder::getDeliveryStatus, DELIVERY_STATUS_OUT)",
                ".setSql(\"version = version + 1\")");
        assertContainsAll(slice(source, "private void updateDeliveryForRollback", "private String currentOperator"),
                "ConcurrencyGuard.requireRowUpdated",
                ".eq(DeliveryOrder::getUuid, order.getUuid())",
                ".eq(DeliveryOrder::getDeliveryStatus, DELIVERY_STATUS_OUT)",
                ".set(DeliveryOrder::getDeliveryStatus, DELIVERY_STATUS_PENDING)",
                ".setSql(\"version = version + 1\")");
    }

    @Test
    void processOrderVoid_whenSubmitted_usesLockReasonAndStatusGuard() throws IOException {
        String source = source(PROCESS_ORDER_SERVICE);

        assertContainsAll(slice(source, "public void voidOrder", "private void validateVoidOrder"),
                "businessLockService.lockProcessOrders(List.of(uuid));",
                "validateVoidOrder(order);",
                "markOrderVoided(order, dto.getReason().trim());");
        assertContainsAll(slice(source, "private void validateVoidOrder", "private void markOrderVoided"),
                "!List.of(STATUS_DRAFT, STATUS_PENDING, STATUS_PROCESSING).contains(status)",
                "ensureOrderNotReferencedBySettle",
                "ensureOrderHasNoDeliveryDetail",
                "ensureOrderHasNoOutboundFinish",
                "ensureNoBackRecordData");
        assertContainsAll(slice(source, "private void markOrderVoided", "private void ensureOrderNotReferencedBySettle"),
                ".in(ProcessOrder::getOrderStatus, List.of(STATUS_DRAFT, STATUS_PENDING, STATUS_PROCESSING))",
                ".set(ProcessOrder::getOrderStatus, STATUS_VOIDED)",
                ".set(ProcessOrder::getVoidReason, reason)",
                ".setSql(\"version = version + 1\")",
                "OperationLogService.ACTION_VOID_ORDER");
    }

    @Test
    void processOrderRollback_whenSubmitted_requiresReasonLocksAndBlocksDownstreamDocuments() throws IOException {
        String source = source(PROCESS_ORDER_SERVICE);

        assertContainsAll(slice(source, "public void changeStatus", "public void voidOrder"),
                "businessLockService.lockProcessOrders(List.of(uuid));",
                "String rollbackReason = requireRollbackReason(reason);",
                "validateRollback(order, from, to);",
                "cleanupDataOnRollback(order, from, to);",
                "OperationLogService.ACTION_ROLLBACK");
        assertContainsAll(slice(source, "public void rollbackToDraft", "public void voidOrder"),
                "updateOrderForDraftRollback(order)");
        assertContainsAll(slice(source, "private void validateRollback", "private void cleanupDataOnRollback"),
                "ensureOrderNotReferencedBySettle",
                "ensureOrderHasNoDeliveryDetail",
                "FINISH_STATUS_OUT",
                "ensureNoBackRecordData(order)");
        assertContainsAll(slice(source, "private void cleanupDataOnRollback", "public void changeRollStatus"),
                "from == OrderStatus.PENDING && to == OrderStatus.DRAFT",
                "clearGeneratedProductionData(order)",
                "from == OrderStatus.PROCESSING && to == OrderStatus.PENDING",
                "resetIssueAndBackRecordFields(order)");
        assertContainsAll(slice(source, "private void clearRollActuals", "private void voidDirectShipFinishes"),
                ".set(OriginalRoll::getTotalLossRatio, ZERO_AMOUNT)");
        assertFalse(slice(source, "private void clearRollActuals", "private void voidDirectShipFinishes")
                        .contains(".set(OriginalRoll::getTotalLossRatio, null)"),
                "母卷总损耗率列为 NOT NULL，回退编辑应归零而不是写入 NULL");
        assertContainsAll(slice(source, "private void clearGeneratedProductionData", "private void resetIssueAndBackRecordFields"),
                ".set(OriginalRoll::getProcessAmount, ZERO_AMOUNT)");
        assertFalse(slice(source, "private void clearGeneratedProductionData", "private void resetIssueAndBackRecordFields")
                .contains(".set(OriginalRoll::getProcessAmount, null)"),
                "待下发回退草稿时原纸加工费列为 NOT NULL，应归零而不是写入 NULL");
        assertContainsAll(slice(source, "private void updateOrderForDraftRollback", "public void changeRollStatus"),
                ".set(ProcessOrder::getBackRecordTime, null)",
                ".set(ProcessOrder::getSnapPrint, null)",
                ".set(ProcessOrder::getTotalAmount, null)",
                ".set(ProcessOrder::getTotalStepCount, 0)",
                ".setSql(\"version = version + 1\")");
    }

    @Test
    void processOrderPrint_locksOrderAndRejectsLostUpdate() throws IOException {
        String print = slice(source(PROCESS_ORDER_SERVICE),
                "public PrintResultVO print", "private String buildSnapPrint");

        assertContainsAll(print,
                "businessLockService.lockProcessOrders(List.of(uuid));",
                "ConcurrencyGuard.requireUpdated(updateById(order));");
        assertBefore(print,
                "businessLockService.lockProcessOrders(List.of(uuid));",
                "ProcessOrder order = getById(uuid);");
    }

    @Test
    void processOrderRemark_whenFinishedBeforeSettlement_allowsSafeRemarkEdits() throws IOException {
        String source = source(PROCESS_ORDER_SERVICE);
        String editableRule = slice(source, "private void validateRemarkEditable", "private void recordFieldIfChanged");

        assertContainsAll(editableRule, "STATUS_SETTLED", "STATUS_VOIDED");
        assertFalse(editableRule.contains("STATUS_FINISHED"), "已完成未结算的加工单应允许直接修改备注");
    }

    @Test
    void deliveryAvailable_whenListingFinishes_acceptsOnlyCompletedProcessOrders() throws IOException {
        String source = source(DELIVERY_SERVICE);

        assertContainsAll(slice(source, "public List<AvailableFinishVO> listAvailable", "public String create"),
                ".in(ProcessOrder::getOrderStatus, List.of(ORDER_STATUS_FINISHED, ORDER_STATUS_SETTLED))");
        assertContainsAll(slice(source, "public String create", "public DeliveryDetailVO getDetail"),
                "if (!canDeliveryProcessOrder(order))",
                "throw new BusinessException(\"加工单非可出库状态：\" + order.getOrderNo())");
    }

    @Test
    void settleReceiveAndVoid_whenMoneyChanges_useActiveLedgerAndLocks() throws IOException {
        String source = source(SETTLE_SERVICE);
        String receive = slice(source, "public void receive", "public void cancelReceive");
        String cancel = slice(source, "public void cancelReceive", "public void voidSettle");
        String voidSettle = slice(source, "public void voidSettle", "private List<SettleDetail> normalizeDetailsForInvoiceView");

        assertContainsAll(receive,
                "businessLockService.lockSettleOrder(uuid);",
                "SettleReceiveAmountResolver.resolve(dto, unreceived)",
                "record.setReceiveAmount(amount.receiveAmount())",
                "record.setCashAmount(amount.cashAmount())",
                "record.setScrapOffsetAmount(amount.scrapOffsetAmount())",
                "record.setDiscountAmount(amount.discountAmount())",
                "record.setRecordStatus(RECEIVE_STATUS_ACTIVE)",
                "refreshReceiveState(settle)");
        assertBefore(receive, "businessLockService.lockSettleOrder(uuid);", "insertReceiveRecord(record)");

        assertContainsAll(cancel,
                "businessLockService.lockSettleOrder(uuid);",
                "businessLockService.lockReceiveRecord(receiveUuid);",
                "updateReceiveRecordForCancel(record)",
                "refreshReceiveState(settle)");
        assertContainsAll(voidSettle,
                "businessLockService.lockSettleOrder(uuid);",
                "activeReceiveAmount(uuid).compareTo(BigDecimal.ZERO) > 0",
                "businessLockService.lockProcessOrders",
                "rollbackSettledProcessOrder(order.getUuid())");

        assertContainsAll(slice(source, "private BigDecimal activeReceiveAmount", "private List<SettleDetail> settleDetails"),
                ".eq(ReceiveRecord::getIsDeleted, 0)",
                "record.getRecordStatus() == null || record.getRecordStatus() == RECEIVE_STATUS_ACTIVE",
                "totals = totals.add(record)");
    }

    @Test
    void settleVoid_whenSubmittedRepeatedly_keepsMasterAndDeletesOnlyOwnActiveDetails() throws IOException {
        String source = source(SETTLE_SERVICE);

        assertContainsAll(slice(source, "public void voidSettle", "private List<SettleDetail> normalizeDetailsForInvoiceView"),
                "settleDetailMapper.delete(new LambdaQueryWrapper<SettleDetail>()",
                ".eq(SettleDetail::getUuid, detail.getUuid())",
                ".eq(SettleDetail::getSettleUuid, uuid)",
                "markSettleVoided(settle, dto.getReason())");
        assertContainsAll(slice(source, "private void markSettleVoided", "private String buildSettleSnapshot"),
                "ConcurrencyGuard.requireRowUpdated",
                ".eq(SettleOrder::getUuid, settle.getUuid())",
                ".eq(SettleOrder::getSettleStatus, settle.getSettleStatus())",
                ".set(SettleOrder::getSettleStatus, SETTLE_STATUS_VOID)",
                ".set(SettleOrder::getVoidReason, reason.trim())",
                ".setSql(\"version = version + 1\")");
    }

    @Test
    void deliveryDetailReservation_whenConcurrentCreate_usesUniqueActiveFinishGuard() throws IOException {
        String source = source(DELIVERY_SERVICE);
        String insert = slice(source, "private void insertDeliveryDetail", "private void validateOutWeight");
        String bootstrap = source(DELIVERY_INTEGRITY_BOOTSTRAP);

        assertContainsAll(insert,
                "ConcurrencyGuard.requireRowUpdated(deliveryDetailMapper.insert(detail))",
                "DuplicateKeyException",
                "ErrorCode.E004");
        assertContainsAll(bootstrap,
                "normalizeDuplicateDeliveryDetails()",
                "stock_lock_status",
                "finish_uuid_active",
                "uk_biz_delivery_detail_active_finish",
                "ADD UNIQUE KEY `uk_biz_delivery_detail_active_finish` (`finish_uuid_active`)");
    }

    @Test
    void monthlySettlement_whenCreatingLocksTheIndexedRangeBeforeLoadingOrders() throws IOException {
        String lockSource = source("src/main/java/com/paper/mes/common/db/BusinessLockService.java");
        String settleSource = source(SETTLE_SERVICE);

        assertContainsAll(lockSource,
                "lockMonthlyFinishedProcessOrders",
                "FORCE INDEX (idx_order_customer_status_accounting)",
                "order_status = 4",
                "is_deleted = 0",
                "accounting_date BETWEEN ? AND ?",
                "ORDER BY accounting_date ASC, order_no ASC, uuid ASC",
                "FOR UPDATE");
        String createByMonth = slice(settleSource, "public String createByMonth", "public SettleDetailVO getDetail");
        assertContainsAll(createByMonth,
                "businessLockService.lockMonthlyFinishedProcessOrders(",
                "List<ProcessOrder> orders = loadOrdersByUuid(orderUuids)");
        assertBefore(createByMonth,
                "businessLockService.lockMonthlyFinishedProcessOrders(",
                "List<ProcessOrder> orders = loadOrdersByUuid(orderUuids)");
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

    private void assertBefore(String text, String first, String second) {
        int firstIndex = text.indexOf(first);
        int secondIndex = text.indexOf(second);
        assertTrue(firstIndex >= 0, "Missing first snippet: " + first);
        assertTrue(secondIndex >= 0, "Missing second snippet: " + second);
        assertTrue(firstIndex < secondIndex, "Expected snippet order: " + first + " before " + second);
    }
}
