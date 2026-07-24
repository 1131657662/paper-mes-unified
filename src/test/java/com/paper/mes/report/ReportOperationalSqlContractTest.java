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
        assertTrue(mapper.contains("f.finish_status = 2"));
        assertTrue(mapper.contains("lockRow.stock_lock_status = 1"));
        assertTrue(mapper.contains("f.stock_in_time IS NULL OR f.stock_in_time &gt;= #{q.dateFrom}"));
        assertFalse(mapper.contains("CURRENT_STOCK_BY_STOCK_IN_MONTH"));
    }

    @Test
    void deliveryTopic_preAggregatesDetailsBeforeJoiningDocuments() {
        String detailTotals = section("<sql id=\"DeliveryDetailTotals\"", "</sql>");
        assertTrue(detailTotals.contains("GROUP BY dd.delivery_uuid"));
        assertFalse(detailTotals.contains("biz_delivery_order"));
    }

    private String section(String startToken, String endToken) {
        int start = mapper.indexOf(startToken);
        int end = mapper.indexOf(endToken, start);
        return mapper.substring(start, end);
    }
}
