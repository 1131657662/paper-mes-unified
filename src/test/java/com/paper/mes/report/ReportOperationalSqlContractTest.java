package com.paper.mes.report;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportOperationalSqlContractTest {
    private static String mapper;

    @BeforeAll
    static void readMapper() throws IOException {
        mapper = Files.readString(Path.of(
                "src/main/resources/mapper/report/ReportOperationalMapper.xml"), StandardCharsets.UTF_8);
    }

    @Test
    void financialTopics_excludeVoidedAndCancelledFacts() {
        assertTrue(mapper.contains("s.settle_status IN (1, 2, 3)"));
        assertTrue(mapper.contains("r.record_status = 1"));
        assertTrue(mapper.contains("r.receive_date &lt; DATE_ADD(#{q.dateTo}, INTERVAL 1 DAY)"));
        assertTrue(mapper.contains("s.unreceived_amount &gt; 0"));
    }

    @Test
    void inventoryTopic_onlyCountsCurrentStoredRollsAndActiveLocks() {
        String inventoryWhere = section("<sql id=\"InventoryWhere\"", "</sql>");
        assertTrue(mapper.contains("f.finish_status = 2"));
        assertTrue(mapper.contains("lockRow.stock_lock_status = 1"));
        assertTrue(inventoryWhere.contains("COALESCE(f.remaining_weight, f.actual_weight, 0) &gt; 0"));
        assertTrue(inventoryWhere.contains("o.order_status IN (3, 4, 5)"));
        assertTrue(inventoryWhere.contains("f.stock_in_time &gt;= #{q.dateFrom}"));
        assertFalse(inventoryWhere.contains("f.stock_in_time IS NULL OR"));
        assertFalse(mapper.contains("f.remaining_weight, f.actual_weight, f.estimate_weight"));
        assertFalse(mapper.contains("CURRENT_STOCK_BY_STOCK_IN_MONTH"));
    }

    @Test
    void deliveryTopic_preAggregatesDetailsBeforeJoiningDocuments() {
        String detailTotals = section("<sql id=\"DeliveryDetailTotals\"", "</sql>");
        assertTrue(detailTotals.contains("GROUP BY dd.delivery_uuid"));
        assertFalse(detailTotals.contains("biz_delivery_order"));
    }

    @Test
    void topicOverviews_returnZeroCountsForEmptyDatasets() {
        String settlement = section("<select id=\"settlementOverview\"", "</select>");
        String collection = section("<select id=\"collectionOverview\"", "</select>");
        String inventory = section("<select id=\"inventoryOverview\"", "</select>");
        String delivery = section("<select id=\"deliveryOverview\"", "</select>");

        assertTrue(settlement.contains("COALESCE(SUM(s.settle_status = 1), 0)"));
        assertTrue(collection.contains("COALESCE(SUM(r.cash_amount &gt; 0), 0)"));
        assertTrue(inventory.contains("COALESCE(SUM(lockRow.finish_uuid IS NULL), 0)"));
        assertTrue(delivery.contains("COALESCE(SUM(d.delivery_status = 1), 0)"));
    }

    private String section(String startToken, String endToken) {
        int start = mapper.indexOf(startToken);
        int end = mapper.indexOf(endToken, start);
        return mapper.substring(start, end);
    }
}
