package com.paper.mes.ai.process.compile;

import com.paper.mes.ai.process.intent.ProcessAiAncillaryRequirements;
import com.paper.mes.ai.process.intent.ProcessAiAssignment;
import com.paper.mes.ai.process.intent.ProcessAiPackagingRequirement;
import com.paper.mes.ai.process.intent.ProcessAiSawIntent;
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
    void compilePiecePriceUsesPreviewFinishCountAndLocalAmount() {
        ProcessAiPackagingCompilation result = compiler.compile(
                List.of(assignment("R1", "PIECE", "[金额]")),
                List.of(plan("R1", "roll-1", 6, "1200")),
                List.of(new ExtractedCharge(new BigDecimal("20"), "PIECE")));

        assertThat(result.errors()).isEmpty();
        assertThat(result.candidates()).singleElement().satisfies(candidate -> {
            assertThat(candidate.stepType()).isEqualTo(4);
            assertThat(candidate.billingBasis()).isEqualTo("PIECE");
            assertThat(candidate.billingMode()).isEqualTo(2);
            assertThat(candidate.serviceQuantity()).isEqualByComparingTo("6");
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
    void compilePiecePriceWithOnSitePreviewLeavesQuantityForManualConfirmation() {
        ProcessAiPackagingCompilation result = compiler.compile(
                List.of(assignment("R1", "PIECE", "[金额]")),
                List.of(plan("R1", "roll-1", 0, "0")),
                List.of(new ExtractedCharge(new BigDecimal("20"), "PIECE")));

        assertThat(result.errors()).isEmpty();
        assertThat(result.candidates()).singleElement().satisfies(candidate -> {
            assertThat(candidate.serviceQuantity()).isNull();
            assertThat(candidate.unitPrice()).isEqualByComparingTo("20");
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

    private ProcessAiAssignment assignment(String owner, String unit, String token) {
        ProcessAiPackagingRequirement packaging = new ProcessAiPackagingRequirement(
                "FILM", token, unit, true);
        return new ProcessAiAssignment(
                List.of(owner), owner, List.of(), "SAW", null,
                new ProcessAiSawIntent("CUTS", 2, null, "mm"),
                new ProcessAiAncillaryRequirements(null, packaging), List.of());
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
}
