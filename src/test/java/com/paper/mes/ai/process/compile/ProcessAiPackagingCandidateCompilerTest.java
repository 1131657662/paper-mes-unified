package com.paper.mes.ai.process.compile;

import com.paper.mes.ai.process.intent.ProcessAiAncillaryRequirements;
import com.paper.mes.ai.process.intent.ProcessAiAssignment;
import com.paper.mes.ai.process.intent.ProcessAiPackagingRequirement;
import com.paper.mes.ai.process.intent.ProcessAiSawIntent;
import com.paper.mes.ai.process.context.ProcessAiRollContext;
import com.paper.mes.ai.process.security.ProcessTextRedactor.ExtractedCharge;
import com.paper.mes.processorder.dto.PlanPreviewVO;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessAiPackagingCandidateCompilerTest {

    private final ProcessAiPackagingCandidateCompiler compiler =
            new ProcessAiPackagingCandidateCompiler();

    @Test
    void compileServiceOnlyStripSortUsesStandardPiecePricingWithoutAiQuantity() {
        ProcessAiPackagingCompilation result = compiler.compile(
                List.of(serviceOnlyAssignment("R1", "STRIP_SORT", "PIECE", "STANDARD", "[金额]")),
                List.of(),
                List.of(new ExtractedCharge(new BigDecimal("20"), "PIECE")),
                List.of(source("R1", 2, "400", null, "800")));

        assertThat(result.errors()).isEmpty();
        assertThat(result.candidates()).singleElement().satisfies(candidate -> {
            assertThat(candidate.stepType()).isEqualTo(3);
            assertThat(candidate.stepName()).isEqualTo("剥损整理");
            assertThat(candidate.billingBasis()).isEqualTo("PIECE");
            assertThat(candidate.billingMode()).isEqualTo(1);
            assertThat(candidate.serviceQuantity()).isNull();
            assertThat(candidate.unitPrice()).isEqualByComparingTo("20");
            assertThat(candidate.billingAmount()).isNull();
        });
    }

    @Test
    void compileFixedPriceUsesBillingAmount() {
        ProcessAiPackagingCompilation result = compiler.compile(
                List.of(assignment("R1", "FIXED", "[金额]")),
                List.of(plan("R1", "roll-1", 3, "800")),
                List.of(new ExtractedCharge(new BigDecimal("60"), "UNSPECIFIED")));

        assertThat(result.candidates()).singleElement().satisfies(candidate -> {
            assertThat(candidate.billingMode()).isEqualTo(3);
            assertThat(candidate.billingBasis()).isNull();
            assertThat(candidate.billingAmount()).isEqualByComparingTo("60");
        });
    }

    @Test
    void compileRepackageMapsToRepackageStepWithoutAiQuantity() {
        ProcessAiPackagingCompilation result = compiler.compile(
                List.of(serviceOnlyAssignment("R1", "REPACKAGE", "PIECE", "STANDARD", "[金额]")),
                List.of(),
                List.of(new ExtractedCharge(new BigDecimal("20"), "PIECE")),
                List.of(source("R1", 0, "400", null, null)));

        assertThat(result.errors()).isEmpty();
        assertThat(result.candidates()).singleElement().satisfies(candidate -> {
            assertThat(candidate.stepType()).isEqualTo(4);
            assertThat(candidate.stepName()).isEqualTo("重新包装");
            assertThat(candidate.serviceQuantity()).isNull();
            assertThat(candidate.unitPrice()).isEqualByComparingTo("20");
        });
    }

    @Test
    void compileTonPriceDoesNotDeriveSourceWeight() {
        ProcessAiPackagingCompilation result = compiler.compile(
                List.of(assignment("R1", "TON", "[金额]")),
                List.of(plan("R1", "roll-1", 3, "1862")),
                List.of(new ExtractedCharge(new BigDecimal("180"), "TON")),
                List.of(source("R1", 3, "500", null, "900")));

        assertThat(result.candidates()).singleElement().satisfies(candidate -> {
            assertThat(candidate.billingBasis()).isEqualTo("TON");
            assertThat(candidate.billingMode()).isEqualTo(1);
            assertThat(candidate.serviceQuantity()).isNull();
        });
    }

    @Test
    void compileSpecifiedQuantityKeepsTheModeButDoesNotSetAQuantity() {
        ProcessAiPackagingCompilation result = compiler.compile(
                List.of(serviceOnlyAssignment("R1", "STRIP_SORT", "TON", "SPECIFIED", "[金额]")),
                List.of(),
                List.of(new ExtractedCharge(new BigDecimal("180"), "TON")),
                List.of(source("R1", 3, "500", "1200", "900")));

        assertThat(result.candidates()).singleElement().satisfies(candidate -> {
            assertThat(candidate.billingMode()).isEqualTo(2);
            assertThat(candidate.serviceQuantity()).isNull();
        });
    }

    @Test
    void compileMultiplePricesRejectsAmbiguousMapping() {
        ProcessAiPackagingCompilation result = compiler.compile(
                List.of(
                        assignment("R1", "PIECE", "[金额]"),
                        assignment("R2", "PIECE", "[金额]")),
                List.of(
                        plan("R1", "roll-1", 3, "800"),
                        plan("R2", "roll-2", 3, "800")),
                List.of(
                        new ExtractedCharge(new BigDecimal("20"), "PIECE"),
                        new ExtractedCharge(new BigDecimal("30"), "PIECE")));

        assertThat(result.candidates()).isEmpty();
        assertThat(result.errors()).containsExactly(
                "同一轮包含多个包装金额，无法安全对应，请分开说明每项包装价格");
    }

    @Test
    void compileServiceOnlyDoesNotRequireMainPlan() {
        ProcessAiPackagingCompilation result = compiler.compile(
                List.of(serviceOnlyAssignment("R1", "STRIP_SORT", "PIECE", "STANDARD", "[金额]")),
                List.of(),
                List.of(new ExtractedCharge(new BigDecimal("20"), "PIECE")),
                List.of(source("R1", 4, "400", null, "800")));

        assertThat(result.errors()).isEmpty();
        assertThat(result.candidates()).singleElement().satisfies(candidate -> {
            assertThat(candidate.ownerRollRef()).isEqualTo("R1");
            assertThat(candidate.originalUuid()).isEqualTo("uuid-R1");
            assertThat(candidate.serviceQuantity()).isNull();
        });
    }

    @Test
    void compileAllServiceOnlyCandidatesReuseTheOnePerPieceCharge() {
        ProcessAiPackagingCompilation result = compiler.compile(
                List.of(
                        serviceOnlyAssignment("R1", "STRIP_SORT", "PIECE", "STANDARD", "[金额]"),
                        serviceOnlyAssignment("R2", "STRIP_SORT", "PIECE", "STANDARD", "[金额]")),
                List.of(), List.of(new ExtractedCharge(new BigDecimal("20"), "PIECE")),
                List.of(source("R1", 4, "400", null, "800"),
                        source("R2", 3, "400", null, "600")));

        assertThat(result.errors()).isEmpty();
        assertThat(result.candidates()).extracting(
                candidate -> candidate.billingMode() + "@" + candidate.unitPrice())
                .containsExactly("1@20", "1@20");
    }

    private ProcessAiAssignment assignment(String owner, String unit, String token) {
        ProcessAiPackagingRequirement packaging = new ProcessAiPackagingRequirement(
                "FILM", token, unit, true);
        return new ProcessAiAssignment(
                List.of(owner), owner, List.of(), "SAW", null,
                new ProcessAiSawIntent("CUTS", 2, null, "mm"),
                new ProcessAiAncillaryRequirements(null, packaging), List.of());
    }

    private ProcessAiAssignment serviceOnlyAssignment(String owner, String type, String unit,
                                                       String quantityMode, String token) {
        ProcessAiPackagingRequirement packaging = new ProcessAiPackagingRequirement(
                type, token, unit, quantityMode, true);
        return new ProcessAiAssignment(
                List.of(owner), owner, List.of(), "SERVICE_ONLY", "SERVICE_ONLY", null, null,
                new ProcessAiAncillaryRequirements(null, packaging), List.of(), List.of());
    }

    private ProcessAiCompiledPlan plan(String owner, String originalUuid,
                                       int finishCount, String weight) {
        PlanPreviewVO preview = new PlanPreviewVO();
        preview.setFinishCount(finishCount);
        preview.setTotalEstimateWeight(new BigDecimal(weight));
        preview.setReady(true);
        return new ProcessAiCompiledPlan(
                owner, originalUuid, List.of(), new ProcessPlanDTO(), preview);
    }

    private ProcessAiRollContext source(String ref, int pieces, String rollWeight,
                                        String actualWeight, String totalWeight) {
        return new ProcessAiRollContext(ref, "uuid-" + ref, 1, "纸", 250,
                2400, 1200, 76, new BigDecimal(rollWeight), pieces,
                1, 1,
                actualWeight == null ? null : new BigDecimal(actualWeight),
                totalWeight == null ? null : new BigDecimal(totalWeight));
    }
}
