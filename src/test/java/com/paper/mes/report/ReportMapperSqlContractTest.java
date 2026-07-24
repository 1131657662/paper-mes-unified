package com.paper.mes.report;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportMapperSqlContractTest {

    @Test
    void reportAndDashboard_whenCalculatingReceivables_useSameSettleAllocationSql() throws IOException {
        String reportSql = settleAllocationSql("mapper/report/ReportMapper.xml");
        String dashboardSql = settleAllocationSql("mapper/dashboard/DashboardMapper.xml");

        assertEquals(reportSql, dashboardSql);
    }

    @Test
    void settleAllocationSql_whenReadingReceipts_usesOnlyActiveReceiveRecords() throws IOException {
        String sql = settleAllocationSql("mapper/report/ReportMapper.xml");

        assertTrue(sql.contains("COALESCE(record_status, 1) = 1"));
        assertTrue(sql.contains("SUM(COALESCE(receive_amount, 0)) AS receivedAmount"));
        assertTrue(sql.contains("COALESCE(rr.receivedAmount, 0) * COALESCE(sd.order_amount, 0) / st.settleTotal"));
    }

    @Test
    void settleAllocationSql_whenAllocatingReceivables_excludesDeletedAndInactiveSettleData() throws IOException {
        String sql = settleAllocationSql("mapper/report/ReportMapper.xml");

        assertTrue(sql.contains("JOIN biz_settle_order so ON so.uuid = sd.settle_uuid AND so.is_deleted = 0 AND COALESCE(so.settle_status, 1) IN (1, 2, 3)"));
        assertTrue(sql.contains("FROM biz_settle_detail WHERE is_deleted = 0"));
        assertTrue(sql.contains("WHERE sd.is_deleted = 0"));
    }

    @Test
    void settleAllocationSql_whenCalculatingReceivables_prefersCurrentSettleAmountBeforeSnapshotFallback() throws IOException {
        String sql = settleAllocationSql("mapper/report/ReportMapper.xml");

        assertTrue(sql.contains("COALESCE( so.total_amount, CAST(JSON_UNQUOTE(JSON_EXTRACT(so.snap_bill, '$.total_amount')) AS DECIMAL(18, 2)), CAST(JSON_UNQUOTE(JSON_EXTRACT(so.snap_bill, '$.totalAmount')) AS DECIMAL(18, 2)), st.settleTotal, 0 )"));
    }

    @Test
    void settleAllocationSql_whenCalculatingReceivables_usesReceiveLedgerNotCachedSettleAmount() throws IOException {
        String sql = settleAllocationSql("mapper/report/ReportMapper.xml");

        assertTrue(sql.contains("FROM biz_receive_record"));
        assertFalse(sql.contains("so.received_amount"));
    }

    @Test
    void reportSql_whenCalculatingUnreceived_excludesUnsettledOrders() throws IOException {
        String sql = resourceText("mapper/report/ReportMapper.xml");

        assertTrue(sql.contains("GREATEST(COALESCE(settle.billedAmount, 0)"));
        assertTrue(sql.contains("- COALESCE(settle.receivedAmount, 0), 0)"));
        assertTrue(sql.contains("CASE WHEN settle.order_uuid IS NULL THEN COALESCE(o.total_amount, 0) ELSE 0 END"));
        assertFalse(sql.contains("GREATEST(COALESCE(settle.billedAmount, o.total_amount, 0)"));
        assertFalse(sql.contains("COALESCE(settle.billedAmount, o.total_amount, 0)\n                           - COALESCE(settle.receivedAmount"));
    }

    @Test
    void reportSql_whenMappingSettlementFields_usesUnderscoreAliases() throws IOException {
        String sql = resourceText("mapper/report/ReportMapper.xml");

        assertTrue(sql.contains("AS settled_amount"));
        assertTrue(sql.contains("AS pending_settle_amount"));
        assertFalse(sql.contains("AS settledAmount"));
        assertFalse(sql.contains("AS pendingSettleAmount"));
    }

    @Test
    void reportAndDashboard_whenDefaultingCompletedScope_excludeVoidedOrders() throws IOException {
        String reportSql = resourceText("mapper/report/ReportMapper.xml");
        String dashboardSql = resourceText("mapper/dashboard/DashboardMapper.xml");

        assertFalse(reportSql.contains("o.order_status &gt;= 4"));
        assertFalse(dashboardSql.contains("o.order_status &gt;= 4"));
        assertTrue(reportSql.contains("AND o.order_status IN (4, 5)"));
        assertTrue(dashboardSql.contains("AND o.order_status IN (4, 5)"));
        assertTrue(reportSql.contains("WHEN 6 THEN '已作废'"));
        assertTrue(dashboardSql.contains("WHEN 6 THEN '已作废'"));
    }

    @Test
    void reportSql_whenRollFiltersApplied_allocatesAmountsByScopedRollBase() throws IOException {
        String sql = resourceText("mapper/report/ReportMapper.xml");

        assertTrue(sql.contains("<sql id=\"RollScopedAmountJoin\">"));
        assertTrue(sql.contains("SUM(rb.rollAmountBase) AS scopedAmountBase"));
        assertTrue(sql.contains("COALESCE(settle.billedAmount, o.total_amount, 0) * amountScope.scopedAmountBase / amountScope.orderAmountBase"));
        assertTrue(sql.contains("COALESCE(settle.receivedAmount, 0) * amountScope.scopedAmountBase / amountScope.orderAmountBase"));
    }

    @Test
    void reportSql_whenShadowLegacyEndpointsRemoved_hasOnlyUnifiedReportQueries() throws IOException {
        String sql = resourceText("mapper/report/ReportMapper.xml");
        String controller = sourceText("src/main/java/com/paper/mes/report/controller/ReportController.java");

        assertFalse(sql.contains("<select id=\"monthlySummary\""));
        assertFalse(sql.contains("<select id=\"customerSummary\""));
        assertFalse(sql.contains("<select id=\"lossAnalysis\""));
        assertFalse(sql.contains("<select id=\"machineOutput\""));
        assertFalse(controller.contains("@GetMapping(\"/monthly\")"));
        assertFalse(controller.contains("@GetMapping(\"/customer\")"));
        assertFalse(controller.contains("@GetMapping(\"/loss\")"));
        assertFalse(controller.contains("@GetMapping(\"/machine\")"));
    }

    @Test
    void reportSql_whenCountingProcessingWeights_excludesDirectShipAcrossAllViews() throws IOException {
        String sql = resourceText("mapper/report/ReportMapper.xml");

        assertEquals(11, count(sql, "COALESCE(r.process_mode, 0) != 3"));
        assertEquals(2, count(sql, "COALESCE(process_mode, 0) != 3"));
        assertTrue(sql.contains("COUNT(CASE WHEN COALESCE(r.process_mode, 0) != 3 THEN 1 END) AS originalRollCount"));
    }

    @Test
    void reportSql_whenFilteringPeriods_usesIndexedAccountingDate() throws IOException {
        String sql = resourceText("mapper/report/ReportMapper.xml");

        assertFalse(sql.contains("DATE(COALESCE(o.back_record_time, o.order_date))"));
        assertFalse(sql.contains("DATE_FORMAT(COALESCE(oi.back_record_time, oi.order_date), '%Y-%m')"));
        assertTrue(sql.contains("AND o.accounting_date &gt;= #{q.dateFrom}"));
        assertTrue(sql.contains("AND o.accounting_date &lt;= #{q.dateTo}"));
        assertTrue(sql.contains("DATE_FORMAT(oi.accounting_date, '%Y-%m')"));
        assertTrue(sql.contains("ORDER BY o.accounting_date DESC"));
    }

    @Test
    void reportSql_whenLoadingDetails_limitsPageAndStreamsExport() throws IOException {
        String sql = resourceText("mapper/report/ReportMapper.xml");

        assertTrue(sql.contains("<select id=\"detailCount\""));
        assertTrue(sql.contains("<sql id=\"DetailRowsQuery\""));
        assertTrue(sql.contains("LIMIT #{offset}, #{limit}"));
        assertTrue(sql.contains("<select id=\"detailCursor\""));
        assertTrue(sql.contains("fetchSize=\"500\""));
        assertTrue(sql.contains("resultSetType=\"FORWARD_ONLY\""));
    }

    @Test
    void reportSql_whenLoadingPaperCandidates_includesHistoricalAndArchiveNames() throws IOException {
        String sql = resourceText("mapper/report/ReportMapper.xml");

        assertTrue(sql.contains("<select id=\"paperCandidates\""));
        assertTrue(sql.contains("FROM biz_original_roll r"));
        assertTrue(sql.contains("FROM sys_paper p"));
        assertTrue(sql.contains("LIKE CONCAT(#{keyword}, '%')"));
        assertTrue(sql.contains("LIMIT #{limit}"));
    }

    private String settleAllocationSql(String resource) throws IOException {
        String sql = resourceText(resource);
        String start = "<sql id=\"SettleAllocationByOrder\">";
        String end = "</sql>";
        int startIndex = sql.indexOf(start);
        int endIndex = sql.indexOf(end, startIndex);
        return normalize(sql.substring(startIndex + start.length(), endIndex));
    }

    private String resourceText(String resource) throws IOException {
        try (var in = getClass().getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("Missing resource: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String sourceText(String relativePath) throws IOException {
        return java.nio.file.Files.readString(java.nio.file.Path.of(relativePath), StandardCharsets.UTF_8);
    }

    private String normalize(String value) {
        return value.replaceAll("\\s+", " ").trim();
    }

    private long count(String text, String pattern) {
        return (text.length() - text.replace(pattern, "").length()) / pattern.length();
    }
}
