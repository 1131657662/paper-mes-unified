package com.paper.mes.ai.process.parse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.process.parse.dto.ProcessAiConfirmResponse;
import com.paper.mes.processorder.service.ProcessAiDraftApplicationService;
import com.paper.mes.processorder.service.ProcessAiDraftApplyCommand;
import com.paper.mes.processorder.service.ProcessAiDraftApplyResult;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessAiConfirmationWriterTest {

    @Test
    void confirmAppliesTheDraftAndStoresEncryptedReplayData() {
        ProcessAiParseRepository repository = mock(ProcessAiParseRepository.class);
        ProcessAiDraftApplicationService draftApplication =
                mock(ProcessAiDraftApplicationService.class);
        ProcessAiConfirmationCodec codec = codec();
        ProcessAiPackagingCandidateRegistrar registrar =
                mock(ProcessAiPackagingCandidateRegistrar.class);
        ProcessAiConfirmationWriter writer = new ProcessAiConfirmationWriter(
                repository, codec, draftApplication, registrar);
        var compilation = ProcessAiConfirmationTestFixtures.compilation();
        when(draftApplication.apply(any())).thenReturn(new ProcessAiDraftApplyResult(
                8, Map.of("original-1", compilation.plans().getFirst()), List.of()));
        when(repository.confirm(any(), any())).thenReturn(1);

        ProcessAiConfirmResponse response = writer.confirm(command());

        assertThat(response.nextVersion()).isEqualTo(8);
        assertThat(response.plans()).containsOnlyKeys("original-1");
        assertThat(response.planHash()).hasSize(64);
        assertStoredConfirmation(repository, codec, response);
        assertDraftApplicationCommand(draftApplication);
        verify(registrar).register(any(), any(),
                org.mockito.ArgumentMatchers.eq("user-1"));
    }

    private ProcessAiConfirmationWriteCommand command() {
        ObjectMapper mapper = ProcessAiConfirmationTestFixtures.mapper();
        ProcessAiParseRecord record = ProcessAiConfirmationTestFixtures.record(
                mapper, "READY", ProcessAiParseConfirmation.empty());
        ProcessAiConfirmationLoad load = new ProcessAiConfirmationLoad(
                record, ProcessAiConfirmationTestFixtures.extraction(),
                List.of(ProcessAiConfirmationTestFixtures.ACCEPTED_PATH), "apply-1", null);
        return new ProcessAiConfirmationWriteCommand(
                load, ProcessAiConfirmationTestFixtures.compilation(), "user-1",
                "cut 2000 mm twice");
    }

    private void assertStoredConfirmation(ProcessAiParseRepository repository,
                                          ProcessAiConfirmationCodec codec,
                                          ProcessAiConfirmResponse response) {
        ArgumentCaptor<ProcessAiParseConfirmation> captor =
                ArgumentCaptor.forClass(ProcessAiParseConfirmation.class);
        verify(repository).confirm(org.mockito.ArgumentMatchers.eq("parse-1"), captor.capture());
        ProcessAiParseConfirmation stored = captor.getValue();
        assertThat(stored.applyIdempotencyKey()).isEqualTo("apply-1");
        assertThat(stored.nextVersion()).isEqualTo(8);
        assertThat(stored.confirmedResultJson()).doesNotContain("original-1");
        assertThat(codec.readResponse(stored.confirmedResultJson(),
                response.conversationId(), response.parseRevision())).isEqualTo(response);
    }

    private void assertDraftApplicationCommand(ProcessAiDraftApplicationService service) {
        ArgumentCaptor<ProcessAiDraftApplyCommand> captor =
                ArgumentCaptor.forClass(ProcessAiDraftApplyCommand.class);
        verify(service).apply(captor.capture());
        ProcessAiDraftApplyCommand command = captor.getValue();
        assertThat(command.orderUuid()).isEqualTo("order-1");
        assertThat(command.expectedVersion()).isEqualTo(7);
        assertThat(command.parseId()).isEqualTo("parse-1");
        assertThat(command.finalCustomerRequirement()).isEqualTo("cut 2000 mm twice");
        assertThat(command.aiRequirementJson()).contains("parse-1");
    }

    private ProcessAiConfirmationCodec codec() {
        return new ProcessAiConfirmationCodec(ProcessAiConfirmationTestFixtures.mapper(),
                Validation.buildDefaultValidatorFactory().getValidator(),
                ProcessAiConfirmationTestFixtures.structuredCipher(),
                ProcessAiConfirmationTestFixtures.intentCipher());
    }
}
