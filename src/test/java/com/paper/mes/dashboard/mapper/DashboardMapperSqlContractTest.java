package com.paper.mes.dashboard.mapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardMapperSqlContractTest {

    @Test
    void monthlyTrendGroupsCompletedOrdersByBackRecordTime() throws IOException {
        String sql = monthlyTrendSql();

        assertTrue(sql.contains("DATE_FORMAT(COALESCE(o.back_record_time, o.order_date), '%Y-%m') AS month"));
        assertTrue(sql.contains("o.back_record_time &gt;= #{monthStart}"));
        assertTrue(sql.contains("o.back_record_time &lt; DATE_ADD(#{today}, INTERVAL 1 DAY)"));
        assertTrue(sql.contains("o.back_record_time IS NULL"));
        assertTrue(sql.contains("o.order_date &gt;= #{monthStart}"));
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
