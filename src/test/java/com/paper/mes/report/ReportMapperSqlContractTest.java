package com.paper.mes.report;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void reportSql_whenCalculatingUnreceived_excludesUnsettledOrders() throws IOException {
        String sql = resourceText("mapper/report/ReportMapper.xml");

        assertTrue(sql.contains("GREATEST(COALESCE(settle.billedAmount, 0)"));
        assertTrue(sql.contains("- COALESCE(settle.receivedAmount, 0), 0)"));
        assertTrue(sql.contains("CASE WHEN settle.order_uuid IS NULL THEN COALESCE(o.total_amount, 0) ELSE 0 END"));
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
    void legacyReportSummarySql_usesUnifiedOrderFilters() throws IOException {
        String sql = resourceText("mapper/report/ReportMapper.xml");

        assertTrue(slice(sql, "<select id=\"monthlySummary\"", "<select id=\"customerSummary\"")
                .contains("<include refid=\"OrderFilters\"/>"));
        assertTrue(slice(sql, "<select id=\"customerSummary\"", "<select id=\"lossAnalysis\"")
                .contains("<include refid=\"OrderFilters\"/>"));
    }

    @Test
    void rollLevelLegacyReportSql_usesRollFilters() throws IOException {
        String sql = resourceText("mapper/report/ReportMapper.xml");

        assertTrue(slice(sql, "<select id=\"lossAnalysis\"", "<select id=\"machineOutput\"")
                .contains("<include refid=\"RollFilters\"/>"));
        assertTrue(slice(sql, "<select id=\"machineOutput\"", "</mapper>")
                .contains("<include refid=\"RollFilters\"/>"));
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

    private String normalize(String value) {
        return value.replaceAll("\\s+", " ").trim();
    }

    private String slice(String text, String start, String end) {
        int startIndex = text.indexOf(start);
        int endIndex = text.indexOf(end, startIndex + start.length());
        assertTrue(startIndex >= 0, "Missing start: " + start);
        assertTrue(endIndex >= 0, "Missing end: " + end);
        return text.substring(startIndex, endIndex);
    }
}
