package com.paper.mes.ai.process.parse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.process.audit.ProcessAiAuditHasher;
import com.paper.mes.ai.process.compile.ProcessAiDefaultResolver;
import com.paper.mes.ai.process.compile.ProcessAiPreviewHashInput;
import com.paper.mes.ai.process.compile.ProcessAiPreviewHashService;
import com.paper.mes.ai.process.compile.ProcessAiCompilationResult;
import com.paper.mes.ai.process.compile.ProcessAiPlanCompilationService;
import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;
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

    @Test
    void confirmUsesTheSameExtractionHashAsPreviewGeneration() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ProcessAiDefaultResolver defaults = mock(ProcessAiDefaultResolver.class);
        ProcessAiPreviewHashService hashes = new ProcessAiPreviewHashService(mapper);
        ProcessAiConfirmationCodec codec = mock(ProcessAiConfirmationCodec.class);
        ProcessAiConfirmResponse committed = response();
        ProcessAiExtractionResult extraction = ProcessAiConfirmationTestFixtures.extraction();
        ProcessAiCompilationResult compilation = new ProcessAiCompilationResult(
                true, List.of(), List.of(), List.of(), List.of());
        when(defaults.resolve(extraction, context())).thenReturn(List.of());
        when(codec.write(extraction)).thenReturn("extraction-json");
        when(codec.write(compilation.rollConfigurations())).thenReturn("[]");
        when(codec.write(compilation.plans())).thenReturn("[]");
        when(codec.write(compilation.packagingCandidates())).thenReturn("[]");
        when(codec.correctionsHash(org.mockito.ArgumentMatchers.any())).thenReturn(null);
        String extractionHash = ProcessAiAuditHasher.sha256("extraction-json");
        String previewHash = hashes.hash(new ProcessAiPreviewHashInput(
                "order-1", 7, "conversation-1", 1, "1.0.0", "checksum",
                extractionHash, extractionHash, null, List.of(),
                mapper.readTree("[]"), mapper.readTree("[]"), mapper.readTree("[]")));
        ProcessAiParseRecord record = new ProcessAiParseRecord(
                "row-1", "order-1", "conversation-1", "parse-1", 2, 1,
                "request-1", 7, "READY", "DEEPSEEK", "deepseek-v4-pro", "PRO", "1.0",
                "1.0.0", "checksum", "[]", null, "legacy-ordinary-sha",
                ProcessAiParseConfirmation.empty(), java.time.LocalDateTime.now(),
                "PREVIEW_READY", "EXTRACTION", 2, null, null, null, null, null,
                previewHash, null, null, "[]", null);
        ProcessAiConfirmationLoad load = new ProcessAiConfirmationLoad(
                record, extraction, List.of(), "apply-1", null, previewHash, List.of());
        ProcessAiConfirmationPreparation preparation = new ProcessAiConfirmationPreparation(
                "user-1", load, context(), "",
                new ProcessTextRedactionResult("", List.of(), false));
        ProcessAiConfirmationPreparationService preparationService = mock(
                ProcessAiConfirmationPreparationService.class);
        ProcessAiPlanCompilationService compiler = mock(ProcessAiPlanCompilationService.class);
        ProcessAiConfirmationCommitter committer = mock(ProcessAiConfirmationCommitter.class);
        ProcessAiParseConfirmationService service = new ProcessAiParseConfirmationService(
                preparationService, compiler, defaults, hashes, codec, mapper, committer);
        ProcessAiConfirmRequest request = new ProcessAiConfirmRequest(
                "conversation-1", "parse-1", 7, "apply-1", List.of(), 2, previewHash, List.of());
        when(preparationService.prepare("order-1", request)).thenReturn(preparation);
        when(compiler.compile(extraction, context(), List.of())).thenReturn(compilation);
        when(committer.commit(preparation, compilation)).thenReturn(committed);

        assertThat(service.confirm("order-1", request)).isEqualTo(committed);
    }

    private ProcessAiConfirmationPreparation preparation(ProcessAiConfirmationLoad load) {
        return new ProcessAiConfirmationPreparation("user-1", load,
                new ProcessAiOrderContext("order-1", 7, "", List.of()), "客户原话",
                new ProcessTextRedactionResult("客户原话", List.of(), false));
    }

    private ProcessAiOrderContext context() {
        return new ProcessAiOrderContext("order-1", 7, "", List.of());
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
