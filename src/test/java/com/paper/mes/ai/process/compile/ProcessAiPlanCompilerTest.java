package com.paper.mes.ai.process.compile;

import com.paper.mes.ai.config.AiProperties;
import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.ai.process.context.ProcessAiRollContext;
import com.paper.mes.ai.process.intent.ProcessAiAssignment;
import com.paper.mes.ai.process.intent.ProcessAiDiameterRule;
import com.paper.mes.ai.process.intent.ProcessAiMeasurement;
import com.paper.mes.ai.process.intent.ProcessAiRewindIntent;
import com.paper.mes.ai.process.intent.ProcessAiSawIntent;
import com.paper.mes.ai.process.intent.ProcessAiWidthRule;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class ProcessAiPlanCompilerTest {

    private final ProcessAiPlanCompiler compiler = compiler();

    @Test
    void compile_weightSplit_createsTwoFiftyPercentSegments() {
        ProcessAiDiameterRule diameter = new ProcessAiDiameterRule(
                "WEIGHT_SPLIT", 2, decimals("50", "50"), null);
        ProcessAiRewindIntent rewind = new ProcessAiRewindIntent(
                "CHANGE_DIAMETER", diameter, measurement("3", "inch"),
                new ProcessAiWidthRule("KEEP_SPEC", null, null, null));

        ProcessPlanDTO plan = compiler.compile(
                assignment("REWIND", rewind, null), context()).plan();

        assertEquals("WEIGHT_SPLIT", plan.getAllocationRule());
        assertEquals(List.of(new BigDecimal("50"), new BigDecimal("50")),
                plan.getSegments().stream().map(segment -> segment.getSegmentRatio()).toList());
        assertEquals(List.of(1200, 1200),
                plan.getSegments().stream().map(segment -> segment.getTargetDiameter()).toList());
        assertEquals(List.of(3, 3),
                plan.getSegments().stream().map(segment -> segment.getFinishCoreDiameter()).toList());
    }

    @Test
    void compile_weightAndWidthSplit_appliesEveryWidthToEveryDiameterSegment() {
        ProcessAiDiameterRule diameter = new ProcessAiDiameterRule(
                "WEIGHT_SPLIT", 2, decimals("50", "50"), null);
        ProcessAiRewindIntent rewind = new ProcessAiRewindIntent(
                "CHANGE_WIDTH_AND_DIAMETER", diameter, measurement("3", "inch"),
                new ProcessAiWidthRule("KNIFE_COUNT", null, "mm", 1));

        ProcessPlanDTO plan = compiler.compile(
                assignment("REWIND", rewind, null), context()).plan();

        assertEquals(2, plan.getSegments().size());
        plan.getSegments().forEach(segment -> {
            assertEquals(List.of(1000, 1000), segment.getLayoutItems().stream()
                    .map(item -> item.getWidth()).toList());
            assertEquals(List.of("FINISH", "FINISH"), segment.getLayoutItems().stream()
                    .map(item -> item.getItemType()).toList());
        });
    }

    @Test
    void compile_sameSpecRewind_inheritsSourceDimensions() {
        ProcessAiDiameterRule diameter = new ProcessAiDiameterRule(
                "KEEP_SPEC", 1, List.of(BigDecimal.valueOf(100)), null);
        ProcessAiRewindIntent rewind = new ProcessAiRewindIntent(
                "KEEP_SPEC", diameter, null,
                new ProcessAiWidthRule("KEEP_SPEC", null, null, null));

        ProcessPlanDTO plan = compiler.compile(
                assignment("REWIND", rewind, null), context()).plan();

        assertEquals(6, plan.getRewindMode());
        assertEquals(48, plan.getSegments().getFirst().getTargetDiameter());
        assertEquals(6, plan.getSegments().getFirst().getFinishCoreDiameter());
        assertEquals(2000, plan.getSegments().getFirst().getLayoutItems().getFirst().getWidth());
    }

    @Test
    void compile_twoSawCuts_distributesIntegerRemainderEvenly() {
        ProcessAiSawIntent saw = new ProcessAiSawIntent(
                "CUTS", 2, null, "mm");

        ProcessPlanDTO plan = compiler.compile(
                assignment("SAW", null, saw), context()).plan();

        assertEquals(2, plan.getKnifeCount());
        assertEquals(List.of(666, 667, 667), plan.getFinishSpecs().stream()
                .map(spec -> spec.getFinishWidth()).toList());
    }

    @Test
    void compile_explicitSawWidth_keepsTheRemainderAsTrim() {
        ProcessAiSawIntent saw = new ProcessAiSawIntent(
                "EXPLICIT_WIDTHS", null, List.of(900), "mm");

        ProcessPlanDTO plan = compiler.compile(
                assignment("SAW", null, saw), context(1000)).plan();

        assertEquals(List.of("FINISH", "TRIM"), plan.getFinishSpecs().stream()
                .map(spec -> spec.getItemType()).toList());
        assertEquals(List.of(900, 100), plan.getFinishSpecs().stream()
                .map(spec -> spec.getFinishWidth()).toList());
    }

    @Test
    void compile_multiSourceRewind_consumesEveryCoveredSource() {
        ProcessAiRewindIntent rewind = new ProcessAiRewindIntent(
                "MULTI_SOURCE", new ProcessAiDiameterRule(
                "EXPLICIT", 1, decimals("100"), measurement("1200", "mm")),
                measurement("3", "inch"),
                new ProcessAiWidthRule("KEEP_SPEC", null, null, null));
        ProcessAiAssignment assignment = new ProcessAiAssignment(
                List.of("R1", "R2"), "R1", List.of("R2"), "REWIND",
                rewind, null, null, List.of());

        ProcessPlanDTO plan = compiler.compile(assignment, multiSourceContext()).plan();

        assertEquals(5, plan.getRewindMode());
        assertEquals(List.of("roll-1", "roll-2"), plan.getSegments().getFirst().getSources()
                .stream().map(source -> source.getOriginalUuid()).toList());
        assertEquals(List.of(new BigDecimal("100"), new BigDecimal("100")),
                plan.getSegments().getFirst().getSources().stream()
                        .map(source -> source.getConsumeRatio()).toList());
    }

    private ProcessAiPlanCompiler compiler() {
        AiProperties properties = new AiProperties();
        properties.setDefaultTargetDiameterMm(1200);
        ProcessAiRewindPlanCompiler rewind = new ProcessAiRewindPlanCompiler(
                new ProcessAiDiameterStorageConverter(),
                new ProcessAiRewindSegmentCompiler(), properties);
        return new ProcessAiPlanCompiler(rewind, new ProcessAiSawPlanCompiler(),
                mock(ProcessAiPlanMachineResolver.class));
    }

    private ProcessAiOrderContext context() {
        return context(2000);
    }

    private ProcessAiOrderContext context(int width) {
        ProcessAiRollContext roll = new ProcessAiRollContext(
                "R1", "roll-1", 1, "白卡纸", 80, width, 48, 6,
                new BigDecimal("800"), 1, 1, 2);
        return new ProcessAiOrderContext("order-1", 3, null, List.of(roll));
    }

    private ProcessAiOrderContext multiSourceContext() {
        ProcessAiRollContext second = new ProcessAiRollContext(
                "R2", "roll-2", 2, "白卡纸", 80, 2000, 48, 6,
                new BigDecimal("600"), 1, 1, 2);
        return new ProcessAiOrderContext(
                "order-1", 3, null, List.of(context().rolls().getFirst(), second));
    }

    private ProcessAiAssignment assignment(String type, ProcessAiRewindIntent rewind,
                                            ProcessAiSawIntent saw) {
        return new ProcessAiAssignment(List.of("R1"), "R1", List.of(), type,
                rewind, saw, null, List.of());
    }

    private ProcessAiMeasurement measurement(String value, String unit) {
        return new ProcessAiMeasurement(new BigDecimal(value), unit, "EXPLICIT");
    }

    private List<BigDecimal> decimals(String... values) {
        return java.util.Arrays.stream(values).map(BigDecimal::new).toList();
    }
}
