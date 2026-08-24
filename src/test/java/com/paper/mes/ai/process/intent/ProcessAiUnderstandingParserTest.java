package com.paper.mes.ai.process.intent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.process.model.ProcessAiProviderException;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessAiUnderstandingParserTest {

    @Test
    void parserAddsSafeFallbackQuestionWhenUnderstandingOmitsOne() {
        ProcessAiUnderstandingResult result = new ProcessAiUnderstandingParser(
                new ObjectMapper(), Validation.buildDefaultValidatorFactory().getValidator())
                .parse("""
                        {"parseId":"p1","schemaVersion":"2.0","conclusion":"需要确认数量范围",
                         "evidence":[],"assumptions":[],"risks":["数量范围不明确"],
                         "clarificationQuestions":[],"needsClarification":true}
                        """);

        assertThat(result.schemaVersion()).isEqualTo("2.0");
        assertThat(result.clarificationQuestions()).singleElement().satisfies(question -> {
            assertThat(question.field()).isEqualTo("clarification");
            assertThat(question.options()).extracting(ProcessAiClarificationOption::code)
                    .containsExactly("ANSWER_TEXT");
        });
    }

    @Test
    void parserRejectsAssignmentsInUnderstandingContract() {
        assertThatThrownBy(() -> new ProcessAiUnderstandingParser(
                new ObjectMapper(), Validation.buildDefaultValidatorFactory().getValidator())
                .parse("""
                        {"parseId":"p1","schemaVersion":"2.0","conclusion":"x",
                         "assignments":[],"evidence":[],"assumptions":[],"risks":[],
                         "clarificationQuestions":[],"needsClarification":true}
                        """))
                .isInstanceOf(ProcessAiProviderException.class)
                .hasMessageContaining("安全契约");
    }

    @Test
    void parserRejectsUnknownQuestionFieldOrOption() {
        assertThatThrownBy(() -> new ProcessAiUnderstandingParser(
                new ObjectMapper(), Validation.buildDefaultValidatorFactory().getValidator())
                .parse("""
                        {"parseId":"p1","schemaVersion":"2.0","conclusion":"x",
                         "evidence":[],"assumptions":[],"risks":[],
                         "clarificationQuestions":[{"questionId":"q1","field":"sql",
                           "parseRevision":1,"question":"x",
                           "options":[{"code":"EXECUTE","label":"x"}],"allowUnknown":true}],
                         "needsClarification":true}
                        """))
                .isInstanceOf(ProcessAiProviderException.class)
                .hasMessageContaining("安全契约");
    }
}
