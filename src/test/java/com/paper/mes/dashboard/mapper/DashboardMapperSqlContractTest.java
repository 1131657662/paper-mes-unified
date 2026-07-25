package com.paper.mes.dashboard.mapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardMapperSqlContractTest {

    @Test
    void monthlyTrendGroupsCompletedOrdersByAccountingDate() throws IOException {
        String sql = monthlyTrendSql();

        assertTrue(sql.contains("DATE_FORMAT(o.accounting_date, '%Y-%m') AS month"));
        assertTrue(sql.contains("o.accounting_date &gt;= #{monthStart}"));
        assertTrue(sql.contains("o.accounting_date &lt;= #{today}"));
    }

    @Test
    void metricsUsesRemainingInventoryWeightAndAccountingDate() throws IOException {
        String xml = resourceText("mapper/dashboard/DashboardMapper.xml");
        int start = xml.indexOf("<select id=\"metrics\"");
        int end = xml.indexOf("</select>", start);
        String sql = xml.substring(start, end).replaceAll("\\s+", " ").trim();

        assertTrue(sql.contains("COALESCE(f.remaining_weight, f.actual_weight, f.estimate_weight, 0)"));
        assertTrue(sql.contains("o.accounting_date &gt;= #{monthStart}"));
    }

    private String monthlyTrendSql() throws IOException {
        String xml = resourceText("mapper/dashboard/DashboardMapper.xml");
        int start = xml.indexOf("<select id=\"monthlyTrend\"");
        int end = xml.indexOf("</select>", start);
        return xml.substring(start, end).replaceAll("\\s+", " ").trim();
    }

    private String resourceText(String resource) throws IOException {
        try (var input = getClass().getClassLoader().getResourceAsStream(resource)) {
            if (input == null) throw new IOException("Missing resource: " + resource);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
