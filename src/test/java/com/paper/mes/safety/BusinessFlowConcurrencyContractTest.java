package com.paper.mes.safety;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BusinessFlowConcurrencyContractTest {

    private static final String DELIVERY_SERVICE =
            "src/main/java/com/paper/mes/delivery/service/impl/DeliveryServiceImpl.java";
    private static final String SETTLE_SERVICE =
            "src/main/java/com/paper/mes/settle/service/impl/SettleServiceImpl.java";

    @Test
    void deliveryRollback_whenOrderIsSettled_blocksBeforeReturningStock() throws IOException {
        String source = source(DELIVERY_SERVICE);
        String rollback = slice(source, "public void rollback", "public void appendDetails");

        assertContainsAll(rollback,
                "businessLockService.lockProcessOrders",
                "businessLockService.lockFinishRolls",
                "ensureOrdersNotSettled(details)",
                "updateFinishStatus(detail.getFinishUuid(), FINISH_STATUS_OUT, FINISH_STATUS_IN_STOCK)");
        assertBefore(rollback,
                "ensureOrdersNotSettled(details)",
                "updateFinishStatus(detail.getFinishUuid(), FINISH_STATUS_OUT, FINISH_STATUS_IN_STOCK)");

        assertContainsAll(slice(source, "private void ensureOrdersNotSettled", "private String originalSummary"),
                "settleDetailMapper.selectCount",
                ".in(SettleDetail::getOrderUuid, orderUuids)",
                "throw new BusinessException(ErrorCode.E004, \"关联加工单已生成结算单，不可回退出库\")");
    }

    @Test
    void deliveryChange_whenEditingPendingOrder_keepsReservationGuards() throws IOException {
        String source = source(DELIVERY_SERVICE);
        String append = slice(source, "public void appendDetails", "public void removeDetail");
        String remove = slice(source, "public void removeDetail", "private String nextDeliveryNo");

        assertContainsAll(append,
                "businessLockService.lockDeliveryOrder(uuid);",
                "businessLockService.lockFinishRolls",
                "order.getDeliveryStatus() == null || order.getDeliveryStatus() != DELIVERY_STATUS_PENDING",
                "requestFinishUuids.add(item.getFinishUuid())",
                "existingFinishUuids.contains(item.getFinishUuid())",
                "lockedFinishUuids.contains(item.getFinishUuid())",
                "refreshTotals(order)");
        assertBefore(append,
                "order.getDeliveryStatus() == null || order.getDeliveryStatus() != DELIVERY_STATUS_PENDING",
                "deliveryDetailMapper.insert(detail)");

        assertContainsAll(remove,
                "businessLockService.lockDeliveryOrder(uuid);",
                "order.getDeliveryStatus() == null || order.getDeliveryStatus() != DELIVERY_STATUS_PENDING",
                "deliveryDetailMapper.deleteById(detailUuid)",
                "refreshTotals(order)");
        assertBefore(remove,
                "order.getDeliveryStatus() == null || order.getDeliveryStatus() != DELIVERY_STATUS_PENDING",
                "deliveryDetailMapper.deleteById(detailUuid)");

        assertContainsAll(slice(source, "private void refreshTotals", "private void updateFinishStatus"),
                ".eq(DeliveryOrder::getDeliveryStatus, DELIVERY_STATUS_PENDING)",
                ".setSql(\"version = version + 1\")");
    }

    @Test
    void settleReceiveAndVoid_whenMoneyChanges_useActiveLedgerAndLocks() throws IOException {
        String source = source(SETTLE_SERVICE);
        String receive = slice(source, "public void receive", "public void cancelReceive");
        String cancel = slice(source, "public void cancelReceive", "public void voidSettle");
        String voidSettle = slice(source, "public void voidSettle", "private SettleDetail buildDetail");

        assertContainsAll(receive,
                "businessLockService.lockSettleOrder(uuid);",
                "amount.compareTo(unreceived) > 0",
                "record.setRecordStatus(RECEIVE_STATUS_ACTIVE)",
                "refreshReceiveState(settle)");
        assertBefore(receive, "businessLockService.lockSettleOrder(uuid);", "receiveRecordMapper.insert(record)");

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
                "record.getRecordStatus() == null || record.getRecordStatus() == RECEIVE_STATUS_ACTIVE",
                "total = total.add(nz(record.getReceiveAmount()))");
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
