package com.paper.mes.ai.process.intent;

import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.ai.process.security.ProcessTextRedactor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessAiEvidenceVerifierTest {

    private final ProcessAiEvidenceVerifier verifier =
            new ProcessAiEvidenceVerifier(new ProcessTextRedactor());

    @Test
    void fabricatedEvidenceIsMarkedAsModelInferenceAndBlocksReady() {
        ProcessAiExtractionResult result = extraction(
                new ProcessAiEvidence("diameterRule", "目标直径1300mm"));

        ProcessAiExtractionResult verified = verifier.verify(result, "改门幅1000mm",
                new ProcessAiOrderContext("order-1", 1, "改门幅1000mm", List.of()), List.of());

        assertThat(verified.needsClarification()).isTrue();
        assertThat(verified.assignments().getFirst().evidence().getFirst().sourceType())
                .isEqualTo("MODEL_INFERENCE");
        assertThat(verified.assignments().getFirst().evidence().getFirst().sourceRef())
                .isEqualTo("model-inference");
    }

    @Test
    void customerTextEvidenceMustBeAContainedRedactedFragment() {
        ProcessAiExtractionResult result = extraction(
                new ProcessAiEvidence("sawIntent", "切两刀", "CUSTOMER_TEXT", "customerRequirement"));

        ProcessAiExtractionResult verified = verifier.verify(result, "客户要求：切两刀",
                new ProcessAiOrderContext("order-1", 1, "客户要求：切两刀", List.of()), List.of());

        assertThat(verified.needsClarification()).isFalse();
        assertThat(verified.assignments().getFirst().evidence().getFirst().sourceType())
                .isEqualTo("CUSTOMER_TEXT");
    }

    @Test
    void numericCustomerEvidenceDoesNotMatchPartOfALargerNumber() {
        ProcessAiExtractionResult result = extraction(
                new ProcessAiEvidence("gramWeight", "80", "CUSTOMER_TEXT", "customerRequirement"));

        ProcessAiExtractionResult verified = verifier.verify(result, "客户要求克重800g",
                new ProcessAiOrderContext("order-1", 1, "客户要求克重800g", List.of()), List.of());

        assertThat(verified.needsClarification()).isTrue();
        assertThat(verified.assignments().getFirst().evidence().getFirst().sourceType())
                .isEqualTo("MODEL_INFERENCE");
    }

    @Test
    void explicitCustomerSpecValuesBecomeVerifiedCustomerEvidence() {
        ProcessAiCustomerSpec spec = new ProcessAiCustomerSpec(
                0, "测试成品", 80, 800, "客户要求调整销售规格");
        ProcessAiAssignment assignment = new ProcessAiAssignment(
                List.of("R1"), "R1", List.of(), "REWIND", null, null, null,
                List.of(new ProcessAiEvidence("rewindIntent", "标准复卷")), List.of(spec));
        ProcessAiExtractionResult result = new ProcessAiExtractionResult(
                "parse-1", "1.0", List.of(assignment), List.of(), List.of(), false, List.of());

        ProcessAiExtractionResult verified = verifier.verify(result,
                "客户要求：标准复卷，成品品名改为测试成品，客户克重80克，客户门幅800毫米",
                new ProcessAiOrderContext("order-1", 1, "需求", List.of()), List.of());

        assertThat(verified.needsClarification()).isFalse();
        assertThat(verified.assignments().getFirst().evidence())
                .extracting(ProcessAiEvidence::field)
                .contains("customerSpecs.paperName", "customerSpecs.gramWeight",
                        "customerSpecs.finishWidth");
        assertThat(verified.assignments().getFirst().evidence())
                .filteredOn(item -> item.field().startsWith("customerSpecs."))
                .allSatisfy(item -> {
                    assertThat(item.sourceType()).isEqualTo("CUSTOMER_TEXT");
                    assertThat(item.sourceRef()).isEqualTo("customerRequirement");
                });
    }

    @Test
    void customerSpecValueAbsentFromCurrentTextDoesNotGainSyntheticEvidence() {
        ProcessAiCustomerSpec spec = new ProcessAiCustomerSpec(
                0, "模型猜测品名", 80, null, "客户要求调整销售规格");
        ProcessAiAssignment assignment = new ProcessAiAssignment(
                List.of("R1"), "R1", List.of(), "SAW", null,
                new ProcessAiSawIntent("CUTS", 2, null, "mm"), null,
                List.of(new ProcessAiEvidence("sawIntent", "切两刀")), List.of(spec));
        ProcessAiExtractionResult result = new ProcessAiExtractionResult(
                "parse-1", "1.0", List.of(assignment), List.of(), List.of(), false, List.of());

        ProcessAiExtractionResult verified = verifier.verify(result,
                "客户要求克重80克", new ProcessAiOrderContext("order-1", 1, "需求", List.of()), List.of());

        assertThat(verified.assignments().getFirst().evidence())
                .noneMatch(item -> "customerSpecs.paperName".equals(item.field()));
        assertThat(verified.assignments().getFirst().evidence())
                .anyMatch(item -> "customerSpecs.gramWeight".equals(item.field())
                        && "CUSTOMER_TEXT".equals(item.sourceType()));
    }

    private ProcessAiExtractionResult extraction(ProcessAiEvidence evidence) {
        ProcessAiAssignment assignment = new ProcessAiAssignment(List.of("R1"), "R1", List.of(),
                "SAW", null, new ProcessAiSawIntent("CUTS", 2, null, "mm"), null, List.of(evidence));
        return new ProcessAiExtractionResult("parse-1", "1.0", List.of(assignment),
                List.of(), List.of(), false, List.of());
    }
}
