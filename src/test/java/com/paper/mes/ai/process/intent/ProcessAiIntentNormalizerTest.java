package com.paper.mes.ai.process.intent;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessAiIntentNormalizerTest {

    private final ProcessAiIntentNormalizer normalizer = new ProcessAiIntentNormalizer();

    @Test
    void normalizeWidthSplitInTwoConvertsSingleExplicitWidthToOneKnife() {
        ProcessAiExtractionResult result = result("门幅一分二");

        ProcessAiRewindIntent rewind = normalizer.normalize(
                result, "2000的直径一复二，门幅一分二")
                .assignments().getFirst().rewindIntent();

        assertThat(rewind.modeIntent()).isEqualTo("CHANGE_WIDTH_AND_DIAMETER");
        assertThat(rewind.widthRule()).isEqualTo(
                new ProcessAiWidthRule("KNIFE_COUNT", null, "mm", 1));
    }

    @Test
    void normalizeDoesNotTrustEvidenceMissingFromCurrentRequirement() {
        ProcessAiExtractionResult result = result("门幅一分二");

        ProcessAiRewindIntent rewind = normalizer.normalize(result, "门幅改1000")
                .assignments().getFirst().rewindIntent();

        assertThat(rewind.widthRule()).isEqualTo(
                new ProcessAiWidthRule("EXPLICIT", List.of(1000), "mm", null));
    }

    private ProcessAiExtractionResult result(String evidence) {
        ProcessAiDiameterRule diameter = new ProcessAiDiameterRule(
                "WEIGHT_SPLIT", 2, List.of(new BigDecimal("50"), new BigDecimal("50")),
                new ProcessAiMeasurement(new BigDecimal("1200"), "mm", "DEFAULT"));
        ProcessAiRewindIntent rewind = new ProcessAiRewindIntent(
                "CHANGE_DIAMETER", diameter,
                new ProcessAiMeasurement(new BigDecimal("3"), "inch", "DEFAULT"),
                new ProcessAiWidthRule("EXPLICIT", List.of(1000), "mm", null));
        ProcessAiAssignment assignment = new ProcessAiAssignment(
                List.of("R1"), "R1", List.of(), "REWIND", rewind, null, null,
                List.of(new ProcessAiEvidence("widthRule", evidence)));
        return new ProcessAiExtractionResult(
                "parse-1", "1.0", List.of(assignment),
                List.of(), List.of(), false, List.of());
    }
}
