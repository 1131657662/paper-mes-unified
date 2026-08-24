package com.paper.mes.ai.process.parse;

import com.paper.mes.common.BusinessException;
import com.paper.mes.ai.process.intent.ProcessAiAssignment;
import com.paper.mes.ai.process.intent.ProcessAiCustomerSpec;
import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;
import com.paper.mes.ai.process.intent.ProcessAiEvidence;
import com.paper.mes.ai.process.intent.ProcessAiSawIntent;
import com.paper.mes.ai.process.intent.ProcessAiAncillaryRequirements;
import com.paper.mes.ai.process.intent.ProcessAiPackagingRequirement;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class ProcessAiAcceptedFieldPathValidatorTest {

    private final ProcessAiAcceptedFieldPathValidator validator =
            new ProcessAiAcceptedFieldPathValidator();

    @Test
    void validateAcceptsOnlyAvailableSemanticFieldPathsAndSortsThem() {
        List<String> result = validator.validate(
                ProcessAiConfirmationTestFixtures.extraction(),
                List.of(ProcessAiConfirmationTestFixtures.ACCEPTED_PATH,
                        "/assignments/R1/processType"));

        assertThat(result).containsExactly(
                "/assignments/R1/processType",
                ProcessAiConfirmationTestFixtures.ACCEPTED_PATH);
    }

    @Test
    void validateRejectsAFieldThatWasNotPresentInTheParse() {
        BusinessException error = catchThrowableOfType(
                () -> validator.validate(ProcessAiConfirmationTestFixtures.extraction(),
                        List.of("/assignments/R1/rewindIntent/core")),
                BusinessException.class);

        assertThat(error.getErrorCode()).isEqualTo("AI_CONFIRM_FIELDS_INVALID");
    }

    @Test
    void validateRejectsDuplicatePaths() {
        BusinessException error = catchThrowableOfType(
                () -> validator.validate(ProcessAiConfirmationTestFixtures.extraction(),
                        List.of(ProcessAiConfirmationTestFixtures.ACCEPTED_PATH,
                                ProcessAiConfirmationTestFixtures.ACCEPTED_PATH)),
                BusinessException.class);

        assertThat(error.getErrorCode()).isEqualTo("AI_CONFIRM_FIELDS_INVALID");
    }

    @Test
    void validateAllowsBackendResolvedMachineAsAnIndependentField() {
        List<String> result = validator.validate(
                ProcessAiConfirmationTestFixtures.extraction(),
                List.of("/assignments/R1/machineUuid"));

        assertThat(result).containsExactly("/assignments/R1/machineUuid");
    }

    @Test
    void validateAllowsCustomerSalesSpecificationPathsPresentInTheParse() {
        ProcessAiAssignment assignment = new ProcessAiAssignment(
                List.of("R1"), "R1", List.of(), "SAW", null,
                new ProcessAiSawIntent("EXPLICIT_WIDTHS", null, List.of(800), "mm"),
                null, List.of(new ProcessAiEvidence("customerSpec", "客户白卡250g")),
                List.of(new ProcessAiCustomerSpec(0, "客户白卡", 250, 800, "合同要求")));
        ProcessAiExtractionResult extraction = new ProcessAiExtractionResult(
                "parse-1", "1.0", List.of(assignment), List.of(), List.of(), false, List.of());

        assertThat(validator.validate(extraction, List.of(
                "/assignments/R1/customerSpecs/0/paperName",
                "/assignments/R1/customerSpecs/0/gramWeight",
                "/assignments/R1/customerSpecs/0/finishWidth",
                "/assignments/R1/customerSpecs/0/overrideReason")))
                .hasSize(4);
    }

    @Test
    void validateAllowsAllFieldsForEveryServiceOnlyRoll() {
        ProcessAiPackagingRequirement packaging = new ProcessAiPackagingRequirement(
                "STRIP_SORT", "[金额]", "PIECE", "STANDARD", true);
        ProcessAiAssignment first = serviceOnly("R1", packaging);
        ProcessAiAssignment second = serviceOnly("R2", packaging);
        ProcessAiExtractionResult extraction = new ProcessAiExtractionResult(
                "parse-1", "1.0", List.of(first, second), List.of(), List.of(), false, List.of());

        List<String> paths = List.of(
                "/assignments/R1/sourceRollRefs", "/assignments/R1/coveredRollRefs",
                "/assignments/R1/processMode", "/assignments/R1/ancillaryRequirements/packaging",
                "/assignments/R2/sourceRollRefs", "/assignments/R2/coveredRollRefs",
                "/assignments/R2/processMode", "/assignments/R2/ancillaryRequirements/packaging");

        assertThat(validator.validate(extraction, paths)).containsExactlyElementsOf(paths.stream()
                .sorted().toList());
    }

    private ProcessAiAssignment serviceOnly(String owner,
                                             ProcessAiPackagingRequirement packaging) {
        return new ProcessAiAssignment(List.of(owner), owner, List.of(),
                "SERVICE_ONLY", "SERVICE_ONLY", null, null,
                new ProcessAiAncillaryRequirements(null, packaging),
                List.of(new ProcessAiEvidence("packaging", "全部剥破损包装，每件20元")), List.of());
    }
}
