package com.paper.mes.report;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportTopicSqlContractTest {

    @Test
    void qualityLossLeaders_areRankedAndBoundedInDatabase() throws IOException {
        String mapper = Files.readString(
                Path.of("src/main/resources/mapper/report/ReportMapper.xml"), StandardCharsets.UTF_8);
        int start = mapper.indexOf("<select id=\"lossLeaderRows\"");
        int end = mapper.indexOf("</select>", start);
        String query = mapper.substring(start, end);

        assertTrue(query.contains("ORDER BY lossRatio DESC, lossWeight DESC"));
        assertTrue(query.contains("LIMIT #{limit}"));
    }
}
