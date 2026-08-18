package com.paper.mes.ai.process.parse;

import com.paper.mes.ai.process.security.ProcessTextRedactionResult;
import com.paper.mes.ai.process.security.ProcessTextRedactor;
import com.paper.mes.ai.process.session.dto.ProcessAiMessageResponse;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class ProcessAiCustomerRequirementResolver {

    private static final int MAX_REQUIREMENT_CHARS = 2_000;

    private final ProcessTextRedactor redactor;

    String resolve(String existing, List<ProcessAiMessageResponse> messages) {
        String result = normalize(existing);
        if (result.isEmpty()) {
            result = messages.stream()
                    .filter(message -> "USER".equals(message.role()))
                    .map(ProcessAiMessageResponse::content)
                    .map(this::normalize)
                    .filter(value -> !value.isEmpty())
                    .findFirst()
                    .orElse("");
        }
        if (result.length() > MAX_REQUIREMENT_CHARS) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "AI_REQUIREMENT_TOO_LONG", "客户加工要求不能超过2000字，请整理为完整简版后再应用");
        }
        return result;
    }

    ProcessTextRedactionResult redactionForConfirmation(
            String currentMessage, List<ProcessAiMessageResponse> messages) {
        ProcessTextRedactionResult current = redactor.redact(currentMessage);
        if (!current.charges().isEmpty()) return current;
        for (int index = messages.size() - 1; index >= 0; index--) {
            ProcessAiMessageResponse message = messages.get(index);
            if (!"USER".equals(message.role()) || message.content().isBlank()) continue;
            ProcessTextRedactionResult earlier = redactor.redact(message.content());
            if (!earlier.charges().isEmpty()) {
                return new ProcessTextRedactionResult(
                        current.sanitizedText(), earlier.charges(), current.modified());
            }
        }
        return current;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
