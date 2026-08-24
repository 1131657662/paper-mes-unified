package com.paper.mes.ai.process.parse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.process.parse.dto.ProcessAiConfirmResponse;
import com.paper.mes.common.BusinessException;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProcessAiConfirmCandidateLoaderTest {

    private final ProcessAiParseRepository repository = mock(ProcessAiParseRepository.class);
    private final ObjectMapper mapper = ProcessAiConfirmationTestFixtures.mapper();
    private final ProcessAiConfirmationCodec codec = new ProcessAiConfirmationCodec(
            mapper, Validation.buildDefaultValidatorFactory().getValidator(),
            ProcessAiConfirmationTestFixtures.structuredCipher(),
            ProcessAiConfirmationTestFixtures.intentCipher());
    private ProcessAiConfirmCandidateLoader loader;

    @BeforeEach
    void setUp() {
        loader = new ProcessAiConfirmCandidateLoader(
                repository, codec, new ProcessAiAcceptedFieldPathValidator());
    }

    @Test
    void loadReturnsAReadyCandidateWithNormalizedAcceptedPaths() {
        when(repository.findByParseIdForUpdate("parse-1")).thenReturn(Optional.of(
                ProcessAiConfirmationTestFixtures.record(
                        mapper, "READY", ProcessAiParseConfirmation.empty())));

        ProcessAiConfirmationLoad result = loader.load("order-1",
                ProcessAiConfirmationTestFixtures.request("apply-1", List.of(
                        ProcessAiConfirmationTestFixtures.ACCEPTED_PATH,
                        "/assignments/R1/processType")));

        assertThat(result.isReplay()).isFalse();
        assertThat(result.applyIdempotencyKey()).isEqualTo("apply-1");
        assertThat(result.acceptedFieldPaths()).isSorted();
    }

    @Test
    void loadReplaysTheStoredResponseForTheSameIdempotencyKey() {
        ProcessAiConfirmResponse stored = storedResponse();
        ProcessAiParseConfirmation confirmation = new ProcessAiParseConfirmation(
                "apply-1", codec.write(List.of(ProcessAiConfirmationTestFixtures.ACCEPTED_PATH)),
                "c".repeat(64), 8, codec.writeResponse(stored), "user-1", LocalDateTime.now());
        when(repository.findByParseIdForUpdate("parse-1")).thenReturn(Optional.of(
                ProcessAiConfirmationTestFixtures.record(mapper, "CONFIRMED", confirmation)));

        ProcessAiConfirmationLoad result = loader.load("order-1",
                ProcessAiConfirmationTestFixtures.request("apply-1",
                        List.of(ProcessAiConfirmationTestFixtures.ACCEPTED_PATH)));

        assertThat(result.replay()).isEqualTo(stored);
    }

    @Test
    void loadRejectsASecondIdempotencyKeyForAnAlreadyConfirmedParse() {
        ProcessAiParseConfirmation confirmation = new ProcessAiParseConfirmation(
                "apply-1", codec.write(List.of(ProcessAiConfirmationTestFixtures.ACCEPTED_PATH)),
                "c".repeat(64), 8, codec.writeResponse(storedResponse()),
                "user-1", LocalDateTime.now());
        when(repository.findByParseIdForUpdate("parse-1")).thenReturn(Optional.of(
                ProcessAiConfirmationTestFixtures.record(mapper, "CONFIRMED", confirmation)));

        BusinessException error = catchThrowableOfType(
                () -> loader.load("order-1", ProcessAiConfirmationTestFixtures.request(
                        "apply-2", List.of(ProcessAiConfirmationTestFixtures.ACCEPTED_PATH))),
                BusinessException.class);

        assertThat(error.getErrorCode()).isEqualTo("AI_PARSE_ALREADY_CONFIRMED");
    }

    @Test
    void loadRejectsTheSameIdempotencyKeyWithDifferentAcceptedFields() {
        ProcessAiParseConfirmation confirmation = new ProcessAiParseConfirmation(
                "apply-1", codec.write(List.of(ProcessAiConfirmationTestFixtures.ACCEPTED_PATH)),
                "c".repeat(64), 8, codec.writeResponse(storedResponse()),
                "user-1", LocalDateTime.now());
        when(repository.findByParseIdForUpdate("parse-1")).thenReturn(Optional.of(
                ProcessAiConfirmationTestFixtures.record(mapper, "CONFIRMED", confirmation)));

        BusinessException error = catchThrowableOfType(
                () -> loader.load("order-1", ProcessAiConfirmationTestFixtures.request(
                        "apply-1", List.of("/assignments/R1/processType"))),
                BusinessException.class);

        assertThat(error.getErrorCode()).isEqualTo("AI_CONFIRM_IDEMPOTENCY_MISMATCH");
    }

    @Test
    void loadRejectsAParseThatStillNeedsClarification() {
        when(repository.findByParseIdForUpdate("parse-1")).thenReturn(Optional.of(
                ProcessAiConfirmationTestFixtures.record(
                        mapper, "CLARIFICATION", ProcessAiParseConfirmation.empty())));

        BusinessException error = catchThrowableOfType(
                () -> loader.load("order-1", ProcessAiConfirmationTestFixtures.request(
                        "apply-1", List.of(ProcessAiConfirmationTestFixtures.ACCEPTED_PATH))),
                BusinessException.class);

        assertThat(error.getErrorCode()).isEqualTo("AI_PARSE_NOT_READY");
    }

    @Test
    void loadRejectsAV2RevisionThatIsNotPreviewReadyBeforeDecodingItsExtraction() {
        when(repository.findByParseIdForUpdate("parse-1")).thenReturn(Optional.of(
                v2Record("READY", "REVISING", "not-a-decryptable-extraction")));

        BusinessException error = catchThrowableOfType(
                () -> loader.load("order-1", v2Request()), BusinessException.class);

        assertThat(error.getErrorCode()).isEqualTo("AI_PARSE_NOT_READY");
    }

    private ProcessAiParseRecord v2Record(String status, String dialogueState, String intentJson) {
        return new ProcessAiParseRecord("row-1", "order-1", "conversation-1", "parse-1",
                2, 1, "request-1", 7, status, "DEEPSEEK", "model", "PRIMARY", "1.0",
                "1.0.0", "sha256:" + "a".repeat(64), "[]", intentJson, "b".repeat(64),
                ProcessAiParseConfirmation.empty(), LocalDateTime.now(), dialogueState,
                "EXTRACTION", 2, null, null, null, null, null, "c".repeat(64),
                null, null, "[]", null);
    }

    private com.paper.mes.ai.process.parse.dto.ProcessAiConfirmRequest v2Request() {
        return new com.paper.mes.ai.process.parse.dto.ProcessAiConfirmRequest(
                "conversation-1", "parse-1", 7, "apply-1",
                List.of(ProcessAiConfirmationTestFixtures.ACCEPTED_PATH), 2,
                "c".repeat(64), List.of());
    }

    private ProcessAiConfirmResponse storedResponse() {
        return new ProcessAiConfirmResponse(
                "conversation-1", "parse-1", 2, 7, 8, "CONFIRMED",
                List.of(ProcessAiConfirmationTestFixtures.ACCEPTED_PATH), Map.of(),
                List.of(), List.of(), "customer requirement", "c".repeat(64));
    }
}
