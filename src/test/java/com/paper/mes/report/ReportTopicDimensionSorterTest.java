package com.paper.mes.report;

import com.paper.mes.report.dto.ReportDimensionVO;
import com.paper.mes.report.service.ReportTopicDimensionSorter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportTopicDimensionSorterTest {

    @Test
    void byInputWeight_ordersLargestMachineLoadFirst() {
        List<ReportDimensionVO> result = ReportTopicDimensionSorter.byInputWeight(List.of(
                row("small", "10", "1", "1"), row("large", "20", "1", "1")));

        assertEquals(List.of("large", "small"), result.stream().map(ReportDimensionVO::getDimensionName).toList());
    }

    @Test
    void byLossRisk_ordersRatioThenWeight() {
        List<ReportDimensionVO> result = ReportTopicDimensionSorter.byLossRisk(List.of(
                row("medium", "10", "2", "10"), row("high", "10", "3", "10"),
                row("highest", "10", "1", "11")));

        assertEquals(List.of("highest", "high", "medium"),
                result.stream().map(ReportDimensionVO::getDimensionName).toList());
    }

    private ReportDimensionVO row(String name, String input, String loss, String ratio) {
        ReportDimensionVO row = new ReportDimensionVO();
        row.setDimensionName(name);
        row.setOriginalWeight(new BigDecimal(input));
        row.setLossWeight(new BigDecimal(loss));
        row.setLossRatio(new BigDecimal(ratio));
        return row;
    }
}
