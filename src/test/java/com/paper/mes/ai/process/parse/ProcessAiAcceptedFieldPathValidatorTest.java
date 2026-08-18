package com.paper.mes.ai.process.parse;

import com.paper.mes.common.BusinessException;
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
}
