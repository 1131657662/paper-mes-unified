package com.paper.mes.ai.process.compile;

import com.paper.mes.ai.process.intent.ProcessAiAssignment;
import com.paper.mes.ai.process.intent.ProcessAiCustomerSpec;
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

    @Test
    void rejectsCustomerSpecWithoutTrustedEvidence() {
        ProcessAiAssignment assignment = new ProcessAiAssignment(
                List.of("R1"), "R1", List.of(), "SAW", null,
                new com.paper.mes.ai.process.intent.ProcessAiSawIntent(
                        "EXPLICIT_WIDTHS", null, List.of(800), "mm"), null,
                List.of(new ProcessAiEvidence("customerSpec", "客户品名")),
                List.of(new ProcessAiCustomerSpec(0, "客户白卡", 250, 800, "客户合同")));

        assertThat(guard.validate(assignment, plan(1, null, List.of(800))))
                .contains("客户品名没有可核验的客户要求依据")
                .contains("客户克重没有可核验的客户要求依据")
                .contains("客户门幅没有可核验的客户要求依据");
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
