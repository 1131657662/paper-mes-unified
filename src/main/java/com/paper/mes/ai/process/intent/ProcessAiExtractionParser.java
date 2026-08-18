package com.paper.mes.ai.process.intent;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.paper.mes.ai.process.model.ProcessAiProviderException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ProcessAiExtractionParser {

    private final ObjectReader reader;
    private final Validator validator;

    public ProcessAiExtractionParser(ObjectMapper objectMapper, Validator validator) {
        this.reader = objectMapper.readerFor(ProcessAiExtractionResult.class)
                .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        this.validator = validator;
    }

    public ProcessAiExtractionResult parse(String content) {
        try {
            ProcessAiExtractionResult result = reader.readValue(content);
            Set<ConstraintViolation<ProcessAiExtractionResult>> violations = validator.validate(result);
            if (!violations.isEmpty()) {
                logValidationFailure(violations);
                throw invalid();
            }
            return result;
        } catch (ProcessAiProviderException ex) {
            throw ex;
        } catch (JsonProcessingException ex) {
            logJsonFailure(ex);
            throw invalid();
        } catch (Exception ex) {
            log.warn("AI extraction rejected: category=UNEXPECTED exceptionType={}",
                    ex.getClass().getSimpleName());
            throw invalid();
        }
    }

    private void logValidationFailure(Set<ConstraintViolation<ProcessAiExtractionResult>> violations) {
        String fields = violations.stream()
                .sorted(Comparator.comparing(value -> value.getPropertyPath().toString()))
                .map(value -> safeToken(value.getPropertyPath().toString()) + ":"
                        + value.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName())
                .collect(Collectors.joining(","));
        log.warn("AI extraction rejected: category=BEAN_VALIDATION fields={}", fields);
    }

    private void logJsonFailure(JsonProcessingException exception) {
        if (exception instanceof UnrecognizedPropertyException unknown) {
            log.warn("AI extraction rejected: category=UNKNOWN_FIELD path={} field={}",
                    safePath(unknown.getPath()), safeToken(unknown.getPropertyName()));
            return;
        }
        if (exception instanceof MismatchedInputException mismatch) {
            log.warn("AI extraction rejected: category=MISMATCHED_INPUT path={} targetType={}",
                    safePath(mismatch.getPath()), safeType(mismatch.getTargetType()));
            return;
        }
        if (exception instanceof JsonParseException malformed) {
            log.warn("AI extraction rejected: category=MALFORMED_JSON line={} column={}",
                    malformed.getLocation().getLineNr(), malformed.getLocation().getColumnNr());
            return;
        }
        log.warn("AI extraction rejected: category=JSON_PROCESSING exceptionType={}",
                exception.getClass().getSimpleName());
    }

    private String safePath(List<JsonMappingException.Reference> path) {
        if (path == null || path.isEmpty()) return "<root>";
        return path.stream().map(reference -> reference.getFieldName() == null
                        ? "[" + reference.getIndex() + "]" : safeToken(reference.getFieldName()))
                .collect(Collectors.joining("."));
    }

    private String safeToken(String value) {
        if (value == null || !value.matches("[A-Za-z][A-Za-z0-9_.\\[\\]-]{0,99}")
                || value.matches(".*\\d{4,}.*")) return "<redacted>";
        return value;
    }

    private String safeType(Class<?> type) {
        return type == null ? "unknown" : safeToken(type.getSimpleName());
    }

    private ProcessAiProviderException invalid() {
        return new ProcessAiProviderException(
                "AI_MODEL_RESULT_INVALID", false, "AI解析结果不符合安全契约");
    }
}
