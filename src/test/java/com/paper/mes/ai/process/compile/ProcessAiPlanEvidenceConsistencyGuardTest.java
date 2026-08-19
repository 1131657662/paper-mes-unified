package com.paper.mes.ai.process.compile;

import com.paper.mes.ai.process.intent.ProcessAiAssignment;
import com.paper.mes.ai.process.intent.ProcessAiDiameterRule;
import com.paper.mes.ai.process.intent.ProcessAiEvidence;
import com.paper.mes.ai.process.intent.ProcessAiMeasurement;
import com.paper.mes.ai.process.intent.ProcessAiRewindIntent;
import com.paper.mes.ai.process.intent.ProcessAiWidthRule;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.dto.RewindLayoutItemPlanDTO;
import com.paper.mes.processorder.dto.RewindSegmentPlanDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessAiPlanEvidenceConsistencyGuardTest {

    private final ProcessAiPlanEvidenceConsistencyGuard guard =
            new ProcessAiPlanEvidenceConsistencyGuard();

    @Test
    void rejectsDiameterEvidenceCompiledAsWidthOnly() {
        ProcessAiAssignment assignment = assignment("CHANGE_WIDTH", 1200, List.of(500, 500));
        ProcessPlanDTO plan = plan(1, null, List.of(500, 500));

        assertThat(guard.validate(assignment, plan))
                .contains("证据与结构化结果冲突：目标直径未进入改直径模式");
    }

    @Test
    void acceptsExplicitDiameterAndWidthCompiledAsModeThree() {
        ProcessAiAssignment assignment = assignment("CHANGE_WIDTH_AND_DIAMETER", 1200,
                List.of(500, 500));
        ProcessPlanDTO plan = plan(3, 1200, List.of(500, 500));

        assertThat(guard.validate(assignment, plan)).isEmpty();
    }

    private ProcessAiAssignment assignment(String mode, int diameter, List<Integer> widths) {
        return new ProcessAiAssignment(List.of("R1"), "R1", List.of(), "REWIND",
                new ProcessAiRewindIntent(mode,
                        new ProcessAiDiameterRule("EXPLICIT", 1,
                                List.of(BigDecimal.valueOf(100)),
                                new ProcessAiMeasurement(BigDecimal.valueOf(diameter), "mm", "EXPLICIT")),
                        null, new ProcessAiWidthRule("EXPLICIT", widths, "mm", null)),
                null, null, List.of(
                        new ProcessAiEvidence("diameterRule", "目标直径" + diameter + "mm"),
                        new ProcessAiEvidence("widthRule", "成品门幅500mm+500mm")));
    }

    private ProcessPlanDTO plan(int mode, Integer diameter, List<Integer> widths) {
        RewindSegmentPlanDTO segment = new RewindSegmentPlanDTO();
        segment.setSegmentSort(1);
        segment.setSegmentRatio(BigDecimal.valueOf(100));
        segment.setTargetDiameter(diameter);
        segment.setLayoutItems(widths.stream().map(width -> {
            RewindLayoutItemPlanDTO item = new RewindLayoutItemPlanDTO();
            item.setItemType("FINISH");
            item.setWidth(width);
            item.setQuantity(1);
            return item;
        }).toList());
        ProcessPlanDTO plan = new ProcessPlanDTO();
        plan.setRewindMode(mode);
        plan.setSegments(List.of(segment));
        return plan;
    }
}
