package com.paper.mes.ai.process.intent;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.paper.mes.ai.process.model.ProcessAiProviderException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ProcessAiUnderstandingParser {

    private static final Map<String, Set<String>> QUESTION_OPTIONS = Map.of(
            "quantityScope", Set.of("PER_SOURCE", "TOTAL"),
            "remainderPolicy", Set.of("TRIM", "FINISH"),
            "sourceBinding", Set.of("SPLIT_SOURCE_ROLLS"),
            "finishCoreDiameter", Set.of("ANSWER_TEXT"),
            "widthMm", Set.of("ANSWER_TEXT"),
            "clarification", Set.of("ANSWER_TEXT"));
    private static final ProcessAiClarificationQuestion FALLBACK_QUESTION =
            new ProcessAiClarificationQuestion("clarification-additional-details", "clarification",
                    1, "请补充可以确定的工艺信息，或选择不确定后转人工处理",
                    java.util.List.of(new ProcessAiClarificationOption(
                            "ANSWER_TEXT", "补充说明")), true);

    private final ObjectReader reader;
    private final Validator validator;

    public ProcessAiUnderstandingParser(ObjectMapper mapper, Validator validator) {
        reader = mapper.readerFor(ProcessAiUnderstandingResult.class)
                .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        this.validator = validator;
    }

    public ProcessAiUnderstandingResult parse(String content) {
        try {
            ProcessAiUnderstandingResult result = reader.readValue(content);
            Set<ConstraintViolation<ProcessAiUnderstandingResult>> violations =
                    validator.validate(result);
            if (!violations.isEmpty() || !result.needsClarification()) {
                String fields = violations.stream()
                        .sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
                        .map(v -> v.getPropertyPath() + ":"
                                + v.getConstraintDescriptor().getAnnotation()
                                .annotationType().getSimpleName())
                        .collect(Collectors.joining(","));
                log.warn("AI understanding rejected: category={} fields={}",
                        violations.isEmpty() ? "MISSING_CLARIFICATION" : "BEAN_VALIDATION", fields);
                throw invalid();
            }
            ProcessAiUnderstandingResult completed = withFallbackQuestion(result);
            validateQuestions(completed);
            return completed;
        } catch (ProcessAiProviderException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("AI understanding rejected: category=INVALID_JSON type={}",
                    ex.getClass().getSimpleName());
            throw invalid();
        }
    }

    private void validateQuestions(ProcessAiUnderstandingResult result) {
        Set<String> questionIds = new HashSet<>();
        result.clarificationQuestions().forEach(question -> {
            Set<String> allowed = QUESTION_OPTIONS.get(question.field());
            if (allowed == null || !questionIds.add(question.questionId())
                    || question.options().stream().map(ProcessAiClarificationOption::code)
                    .distinct().count() != question.options().size()
                    || question.options().stream().anyMatch(option -> !allowed.contains(option.code()))) {
                throw invalid();
            }
            if ("quantityScope".equals(question.field())
                    && question.options().size() != 2
                    || "remainderPolicy".equals(question.field())
                    && question.options().size() != 2) {
                throw invalid();
            }
        });
    }

    private ProcessAiUnderstandingResult withFallbackQuestion(ProcessAiUnderstandingResult result) {
        if (!result.clarificationQuestions().isEmpty()) return result;
        return new ProcessAiUnderstandingResult(result.parseId(), result.schemaVersion(),
                result.conclusion(), result.evidence(), result.assumptions(), result.risks(),
                java.util.List.of(FALLBACK_QUESTION), true);
    }

    private ProcessAiProviderException invalid() {
        return new ProcessAiProviderException(
                "AI_MODEL_RESULT_INVALID", false, "AI解析结果不符合安全契约");
    }
}
