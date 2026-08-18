package com.paper.mes.ai.process.parse;

import com.paper.mes.ai.process.compile.ProcessAiCompilationResult;
import com.paper.mes.ai.process.compile.ProcessAiPlanCompilationService;
import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.ai.process.parse.dto.ProcessAiConfirmRequest;
import com.paper.mes.ai.process.parse.dto.ProcessAiConfirmResponse;
import com.paper.mes.ai.process.security.ProcessTextRedactionResult;
import com.paper.mes.common.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessAiParseConfirmationServiceTest {

    private final ProcessAiConfirmationPreparationService preparationService =
            mock(ProcessAiConfirmationPreparationService.class);
    private final ProcessAiPlanCompilationService compilationService =
            mock(ProcessAiPlanCompilationService.class);
    private final ProcessAiConfirmationCommitter committer =
            mock(ProcessAiConfirmationCommitter.class);
    private final ProcessAiParseConfirmationService service =
            new ProcessAiParseConfirmationService(
                    preparationService, compilationService, committer);

    @Test
    void confirmReturnsAStoredReplayWithoutRecompiling() {
        ProcessAiConfirmRequest request = request();
        ProcessAiConfirmResponse replay = response();
        ProcessAiConfirmationLoad load = load(replay);
        when(preparationService.prepare("order-1", request)).thenReturn(
                new ProcessAiConfirmationPreparation("user-1", load, null, null, null));

        ProcessAiConfirmResponse result = service.confirm("order-1", request);

        assertThat(result).isEqualTo(replay);
        verify(compilationService, never()).compile(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(committer, never()).commit(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void confirmRejectsACandidateThatNoLongerPassesPreview() {
        ProcessAiConfirmRequest request = request();
        ProcessAiConfirmationPreparation preparation = preparation(load(null));
        when(preparationService.prepare("order-1", request)).thenReturn(preparation);
        when(compilationService.compile(preparation.load().extraction(), preparation.context(),
                preparation.redaction().charges()))
                .thenReturn(new ProcessAiCompilationResult(
                        false, List.of(), List.of(), List.of("preview failed"), List.of()));

        BusinessException error = catchThrowableOfType(
                () -> service.confirm("order-1", request), BusinessException.class);

        assertThat(error.getErrorCode()).isEqualTo("AI_PARSE_NOT_APPLICABLE");
        verify(committer, never()).commit(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private ProcessAiConfirmationPreparation preparation(ProcessAiConfirmationLoad load) {
        return new ProcessAiConfirmationPreparation("user-1", load,
                new ProcessAiOrderContext("order-1", 7, "", List.of()), "客户原话",
                new ProcessTextRedactionResult("客户原话", List.of(), false));
    }

    private ProcessAiConfirmationLoad load(ProcessAiConfirmResponse replay) {
        ProcessAiParseRecord record = ProcessAiConfirmationTestFixtures.record(
                ProcessAiConfirmationTestFixtures.mapper(),
                replay == null ? "READY" : "CONFIRMED", ProcessAiParseConfirmation.empty());
        return new ProcessAiConfirmationLoad(
                record, ProcessAiConfirmationTestFixtures.extraction(),
                List.of(ProcessAiConfirmationTestFixtures.ACCEPTED_PATH), "apply-1", replay);
    }

    private ProcessAiConfirmRequest request() {
        return ProcessAiConfirmationTestFixtures.request(
                "apply-1", List.of(ProcessAiConfirmationTestFixtures.ACCEPTED_PATH));
    }

    private ProcessAiConfirmResponse response() {
        return new ProcessAiConfirmResponse(
                "conversation-1", "parse-1", 2, 7, 8, "CONFIRMED",
                List.of(ProcessAiConfirmationTestFixtures.ACCEPTED_PATH), Map.of(),
                List.of(), List.of(), "customer requirement", "c".repeat(64));
    }
}
