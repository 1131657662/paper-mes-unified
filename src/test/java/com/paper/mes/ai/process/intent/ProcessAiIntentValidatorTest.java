package com.paper.mes.ai.process.intent;

import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.ai.process.context.ProcessAiRollContext;
import com.paper.mes.common.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class ProcessAiIntentValidatorTest {

    private final ProcessAiIntentValidator validator = new ProcessAiIntentValidator();

    @Test
    void validateAcceptsClosedOwnerCoverageAndWeightRatios() {
        ProcessAiExtractionResult result = result(List.of(
                assignment(List.of("R1", "R2"), "R1", List.of("R2"), List.of(50, 50), null)));

        validator.validate(result, context());
    }

    @Test
    void validateRejectsUnknownSourceReferences() {
        ProcessAiExtractionResult result = result(List.of(
                assignment(List.of("R9"), "R9", List.of(), List.of(50, 50), null)));

        BusinessException error = catchThrowableOfType(
                () -> validator.validate(result, context()), BusinessException.class);

        assertThat(error.getErrorCode()).isEqualTo("AI_INTENT_SOURCE_INVALID");
    }

    @Test
    void validateRejectsCoveredReferencesThatDoNotMatchTheOwner() {
        ProcessAiExtractionResult result = result(List.of(
                assignment(List.of("R1", "R2"), "R1", List.of(), List.of(50, 50), null)));

        BusinessException error = catchThrowableOfType(
                () -> validator.validate(result, context()), BusinessException.class);

        assertThat(error.getErrorCode()).isEqualTo("AI_INTENT_COVERAGE_INVALID");
    }

    @Test
    void validateRejectsWeightSplitRatiosThatDoNotTotalOneHundred() {
        ProcessAiExtractionResult result = result(List.of(
                assignment(List.of("R1"), "R1", List.of(), List.of(40, 50), null)));

        BusinessException error = catchThrowableOfType(
                () -> validator.validate(result, context()), BusinessException.class);

        assertThat(error.getErrorCode()).isEqualTo("AI_INTENT_WEIGHT_SPLIT_INVALID");
    }

    @Test
    void validateRejectsLabelsThatAttemptToCreateAServiceStep() {
        ProcessAiAncillaryRequirements ancillary = new ProcessAiAncillaryRequirements(
                new ProcessAiLabelRequirement(true, "贴标签", true), null);
        ProcessAiExtractionResult result = result(List.of(
                assignment(List.of("R1"), "R1", List.of(), List.of(50, 50), ancillary)));

        BusinessException error = catchThrowableOfType(
                () -> validator.validate(result, context()), BusinessException.class);

        assertThat(error.getErrorCode()).isEqualTo("AI_INTENT_LABEL_STEP_FORBIDDEN");
    }

    @Test
    void validateAllowsTemporaryOverlapWhenAClarificationBlocksCompilation() {
        ProcessAiExtractionResult result = new ProcessAiExtractionResult(
                "parse-1", "1.0", List.of(
                assignment(List.of("R1"), "R1", List.of(), List.of(50, 50), null),
                assignment(List.of("R1"), "R1", List.of(), List.of(50, 50), null)),
                List.of(), List.of(), true, List.of("请确认两组分别对应哪条母卷"));

        validator.validate(result, context());
    }

    private ProcessAiAssignment assignment(List<String> sources, String owner, List<String> covered,
                                            List<Integer> ratios,
                                            ProcessAiAncillaryRequirements ancillary) {
        ProcessAiDiameterRule diameter = new ProcessAiDiameterRule("WEIGHT_SPLIT", ratios.size(),
                ratios.stream().map(BigDecimal::valueOf).toList(),
                new ProcessAiMeasurement(new BigDecimal("1200"), "mm", "DEFAULT"));
        ProcessAiRewindIntent rewind = new ProcessAiRewindIntent(
                "CHANGE_DIAMETER", diameter, null, null);
        return new ProcessAiAssignment(sources, owner, covered, "REWIND", rewind, null,
                ancillary, List.of(new ProcessAiEvidence("diameterRule", "直径一分为二")));
    }

    private ProcessAiExtractionResult result(List<ProcessAiAssignment> assignments) {
        return new ProcessAiExtractionResult("parse-1", "1.0", assignments,
                List.of(), List.of(), false, List.of());
    }

    private ProcessAiOrderContext context() {
        return new ProcessAiOrderContext("order-1", 7, "客户原话", List.of(
                roll("R1", "roll-1", 1), roll("R2", "roll-2", 2)));
    }

    private ProcessAiRollContext roll(String ref, String uuid, int sort) {
        return new ProcessAiRollContext(ref, uuid, sort, "白卡纸", 250,
                2000, 1500, 3, new BigDecimal("1000"), 1, 1, 2);
    }
}
