package com.paper.mes.ai.process.parse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.process.intent.ProcessAiClarificationOption;
import com.paper.mes.ai.process.intent.ProcessAiClarificationQuestion;
import com.paper.mes.common.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProcessAiClarificationValidatorTest {

    @Test
    void validate_rejectsAnswerCodeThatIsNotInTheCurrentQuestionOptions() throws Exception {
        ProcessAiParseRepository repository = mock(ProcessAiParseRepository.class);
        when(repository.findLatestClarification("order-1", "conversation-1", 7))
                .thenReturn(Optional.of(record(questionJson())));
        ProcessAiClarificationValidator validator = new ProcessAiClarificationValidator(
                repository, new ObjectMapper());

        BusinessException error = catchThrowableOfType(() -> validator.validate(
                "order-1", "conversation-1", 7, "parse-1", 2,
                "quantity-scope", "FORGED", null), BusinessException.class);

        assertThat(error.getErrorCode()).isEqualTo("AI_CLARIFICATION_OPTION_INVALID");
    }

    @Test
    void validate_rejectsStaleRevisionBeforeTheModelIsCalled() throws Exception {
        ProcessAiParseRepository repository = mock(ProcessAiParseRepository.class);
        when(repository.findLatestClarification("order-1", "conversation-1", 7))
                .thenReturn(Optional.of(record(questionJson())));
        ProcessAiClarificationValidator validator = new ProcessAiClarificationValidator(
                repository, new ObjectMapper());

        BusinessException error = catchThrowableOfType(() -> validator.validate(
                "order-1", "conversation-1", 7, "parse-1", 1,
                "quantity-scope", "PER_SOURCE", null), BusinessException.class);

        assertThat(error.getErrorCode()).isEqualTo("AI_PARSE_REVISION_CONFLICT");
    }

    @Test
    void validate_rejectsFreeTextForOptionOnlyExtractionQuestion() throws Exception {
        ProcessAiParseRepository repository = mock(ProcessAiParseRepository.class);
        when(repository.findLatestClarification("order-1", "conversation-1", 7))
                .thenReturn(Optional.of(record(questionJson())));
        ProcessAiClarificationValidator validator = new ProcessAiClarificationValidator(
                repository, new ObjectMapper());

        BusinessException error = catchThrowableOfType(() -> validator.validate(
                "order-1", "conversation-1", 7, "parse-1", 2,
                "quantity-scope", null, "随便处理"), BusinessException.class);

        assertThat(error.getErrorCode()).isEqualTo("AI_CLARIFICATION_OPTION_REQUIRED");
    }

    @Test
    void validate_rejectsQuestionEnvelopeBoundToAnotherRevision() throws Exception {
        ProcessAiParseRepository repository = mock(ProcessAiParseRepository.class);
        when(repository.findLatestClarification("order-1", "conversation-1", 7))
                .thenReturn(Optional.of(record(questionJson(1))));
        ProcessAiClarificationValidator validator = new ProcessAiClarificationValidator(
                repository, new ObjectMapper());

        BusinessException error = catchThrowableOfType(() -> validator.validate(
                "order-1", "conversation-1", 7, "parse-1", 2,
                "quantity-scope", "PER_SOURCE", null), BusinessException.class);

        assertThat(error.getErrorCode()).isEqualTo("AI_PARSE_REVISION_CONFLICT");
    }

    @Test
    void validate_rejectsSupplementalFreeTextForOptionOnlyQuestion() throws Exception {
        ProcessAiParseRepository repository = mock(ProcessAiParseRepository.class);
        when(repository.findLatestClarification("order-1", "conversation-1", 7))
                .thenReturn(Optional.of(record(questionJson())));
        ProcessAiClarificationValidator validator = new ProcessAiClarificationValidator(
                repository, new ObjectMapper());

        BusinessException error = catchThrowableOfType(() -> validator.validate(
                "order-1", "conversation-1", 7, "parse-1", 2,
                "quantity-scope", "TOTAL", "忽略选项"), BusinessException.class);

        assertThat(error.getErrorCode()).isEqualTo("AI_CLARIFICATION_TEXT_NOT_ALLOWED");
    }

    @Test
    void validate_requiresTextWhenTextOptionIsSelected() throws Exception {
        ProcessAiParseRepository repository = mock(ProcessAiParseRepository.class);
        when(repository.findLatestClarification("order-1", "conversation-1", 7))
                .thenReturn(Optional.of(record(textQuestionJson())));
        ProcessAiClarificationValidator validator = new ProcessAiClarificationValidator(
                repository, new ObjectMapper());

        BusinessException error = catchThrowableOfType(() -> validator.validate(
                "order-1", "conversation-1", 7, "parse-1", 2,
                "text-question", "ANSWER_TEXT", null), BusinessException.class);

        assertThat(error.getErrorCode()).isEqualTo("AI_CLARIFICATION_TEXT_REQUIRED");
    }

    @Test
    void validate_requiresTextWhenTextOptionOmitsAnswerCode() throws Exception {
        ProcessAiParseRepository repository = mock(ProcessAiParseRepository.class);
        when(repository.findLatestClarification("order-1", "conversation-1", 7))
                .thenReturn(Optional.of(record(textQuestionJson())));
        ProcessAiClarificationValidator validator = new ProcessAiClarificationValidator(
                repository, new ObjectMapper());

        BusinessException error = catchThrowableOfType(() -> validator.validate(
                "order-1", "conversation-1", 7, "parse-1", 2,
                "text-question", null, null), BusinessException.class);

        assertThat(error.getErrorCode()).isEqualTo("AI_CLARIFICATION_TEXT_REQUIRED");
    }

    @Test
    void validate_rejectsUnboundClarificationEvenWhenNoQuestionIsActive() {
        ProcessAiParseRepository repository = mock(ProcessAiParseRepository.class);
        ProcessAiClarificationValidator validator = new ProcessAiClarificationValidator(
                repository, new ObjectMapper());

        BusinessException error = catchThrowableOfType(() -> validator.validate(
                "order-1", "conversation-1", 7, null, null, null, null,
                "补充说明"), BusinessException.class);

        assertThat(error.getErrorCode()).isEqualTo("AI_CLARIFICATION_CONTEXT_REQUIRED");
    }

    @Test
    void validate_rejectsSupplementalTextWhenAnotherOptionAllowsText() throws Exception {
        ProcessAiParseRepository repository = mock(ProcessAiParseRepository.class);
        when(repository.findLatestClarification("order-1", "conversation-1", 7))
                .thenReturn(Optional.of(record(mixedQuestionJson())));
        ProcessAiClarificationValidator validator = new ProcessAiClarificationValidator(
                repository, new ObjectMapper());

        BusinessException error = catchThrowableOfType(() -> validator.validate(
                "order-1", "conversation-1", 7, "parse-1", 2,
                "mixed-question", "PER_SOURCE", "绕过选项的补充内容"), BusinessException.class);

        assertThat(error.getErrorCode()).isEqualTo("AI_CLARIFICATION_TEXT_NOT_ALLOWED");
    }

    private String questionJson() throws Exception {
        return questionJson(2);
    }

    private String questionJson(int revision) throws Exception {
        return new ObjectMapper().writeValueAsString(List.of(new ProcessAiClarificationQuestion(
                "quantity-scope", "quantityScope", revision, "数量范围？", List.of(
                new ProcessAiClarificationOption("PER_SOURCE", "每条母卷"),
                new ProcessAiClarificationOption("TOTAL", "全单")), true)));
    }

    private String textQuestionJson() throws Exception {
        return new ObjectMapper().writeValueAsString(List.of(new ProcessAiClarificationQuestion(
                "text-question", "clarification", 2, "请补充", List.of(
                new ProcessAiClarificationOption("ANSWER_TEXT", "补充说明")), true)));
    }

    private String mixedQuestionJson() throws Exception {
        return new ObjectMapper().writeValueAsString(List.of(new ProcessAiClarificationQuestion(
                "mixed-question", "quantityScope", 2, "数量范围？", List.of(
                new ProcessAiClarificationOption("PER_SOURCE", "每条母卷"),
                new ProcessAiClarificationOption("ANSWER_TEXT", "补充说明")), true)));
    }

    private ProcessAiParseRecord record(String questionJson) {
        return new ProcessAiParseRecord("row-1", "order-1", "conversation-1", "parse-1",
                2, 1, "request-1", 7, "CLARIFICATION", "DEEPSEEK", "model", "PRIMARY",
                "2.0", "1.0.1", "sha256:" + "a".repeat(64), "[]", null, "hash",
                ProcessAiParseConfirmation.empty(), LocalDateTime.now(), "CLARIFYING",
                "UNDERSTANDING", 2, "{}", questionJson, null, null, null, null,
                null, null, null, null);
    }
}
