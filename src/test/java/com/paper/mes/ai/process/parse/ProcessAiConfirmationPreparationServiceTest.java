package com.paper.mes.ai.process.parse;

import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.ai.process.parse.dto.ProcessAiConfirmRequest;
import com.paper.mes.ai.process.security.ProcessTextRedactor;
import com.paper.mes.ai.process.session.ProcessAiMessageService;
import com.paper.mes.ai.process.session.dto.ProcessAiMessageResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProcessAiConfirmationPreparationServiceTest {

    @Test
    void resolverUsesFirstUserMessageWhenBaseRequirementIsEmpty() {
        ProcessAiCustomerRequirementResolver resolver =
                new ProcessAiCustomerRequirementResolver(new ProcessTextRedactor());

        String result = resolver.resolve("", List.of(
                messageStatic(1, "USER", "客户原话"),
                messageStatic(2, "USER", "后续澄清")));

        assertThat(result).isEqualTo("客户原话");
    }

    @Test
    void prepareKeepsOriginalRequirementSeparateFromConversationClarifications() {
        ProcessAiConfirmCandidateLoader loader = mock(ProcessAiConfirmCandidateLoader.class);
        ProcessAiConfirmationContextGuard guard = mock(ProcessAiConfirmationContextGuard.class);
        ProcessAiMessageService messages = mock(ProcessAiMessageService.class);
        ProcessAiConfirmationPreparationService service = new ProcessAiConfirmationPreparationService(
                loader, guard, messages,
                new ProcessAiCustomerRequirementResolver(new ProcessTextRedactor()));
        ProcessAiConfirmRequest request = ProcessAiConfirmationTestFixtures.request(
                "apply-1", List.of(ProcessAiConfirmationTestFixtures.ACCEPTED_PATH));
        ProcessAiConfirmationLoad load = readyLoad();
        when(guard.requireOwner("order-1", "conversation-1")).thenReturn("user-1");
        when(loader.load("order-1", request)).thenReturn(load);
        when(guard.lockAndRead("order-1", "conversation-1", 7, 1))
                .thenReturn(new ProcessAiOrderContext(
                        "order-1", 7, "existing requirement", List.of()));
        when(messages.restore("order-1", "conversation-1", 7)).thenReturn(List.of(
                message(1, "USER", "cut 2000 mm twice"),
                message(2, "ASSISTANT", "parsed"),
                message(3, "USER", "also add a label")));
        when(messages.restoreUserMessage(
                "order-1", "conversation-1", 7, "request-1"))
                .thenReturn("film wrap 20 yuan per piece");

        ProcessAiConfirmationPreparation result = service.prepare("order-1", request);

        assertThat(result.customerRequirement()).isEqualTo("existing requirement");
    }

    private ProcessAiConfirmationLoad readyLoad() {
        ProcessAiParseRecord record = ProcessAiConfirmationTestFixtures.record(
                ProcessAiConfirmationTestFixtures.mapper(),
                "READY", ProcessAiParseConfirmation.empty());
        return new ProcessAiConfirmationLoad(
                record, ProcessAiConfirmationTestFixtures.extraction(),
                List.of(ProcessAiConfirmationTestFixtures.ACCEPTED_PATH), "apply-1", null);
    }

    private ProcessAiMessageResponse message(int sequence, String role, String content) {
        return messageStatic(sequence, role, content);
    }

    private static ProcessAiMessageResponse messageStatic(int sequence, String role, String content) {
        return new ProcessAiMessageResponse(
                sequence, role, "FINAL", content, null, LocalDateTime.now());
    }
}
