package com.paper.mes.ai.process.parse;

import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;
import com.paper.mes.ai.process.parse.dto.ProcessAiConfirmRequest;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class ProcessAiConfirmCandidateLoader {

    private final ProcessAiParseRepository repository;
    private final ProcessAiConfirmationCodec codec;
    private final ProcessAiAcceptedFieldPathValidator fieldPathValidator;

    ProcessAiConfirmationLoad load(String orderUuid, ProcessAiConfirmRequest request) {
        ProcessAiParseRecord record = repository.findByParseIdForUpdate(request.parseId())
                .orElseThrow(this::notFound);
        requireIdentity(record, orderUuid, request);
        ProcessAiExtractionResult extraction = codec.readExtraction(record);
        List<String> paths = fieldPathValidator.validate(
                extraction, request.acceptedFieldPaths());
        if ("CONFIRMED".equals(record.status())) {
            return replay(record, extraction, paths, request.applyIdempotencyKey());
        }
        if (!"READY".equals(record.status())) {
            throw conflict("AI_PARSE_NOT_READY", "Only READY AI parses can be confirmed");
        }
        return new ProcessAiConfirmationLoad(
                record, extraction, paths, request.applyIdempotencyKey(), null);
    }

    private ProcessAiConfirmationLoad replay(ProcessAiParseRecord record,
                                              ProcessAiExtractionResult extraction,
                                              List<String> paths, String idempotencyKey) {
        ProcessAiParseConfirmation confirmation = record.confirmation();
        if (!idempotencyKey.equals(confirmation.applyIdempotencyKey())) {
            throw conflict("AI_PARSE_ALREADY_CONFIRMED", "This AI parse is already confirmed");
        }
        List<String> storedPaths = codec.readPaths(confirmation.acceptedFieldPathsJson());
        if (!paths.equals(storedPaths)) {
            throw conflict("AI_CONFIRM_IDEMPOTENCY_MISMATCH",
                    "The idempotency key was reused with different accepted fields");
        }
        return new ProcessAiConfirmationLoad(record, extraction, paths, idempotencyKey,
                codec.readResponse(confirmation.confirmedResultJson(),
                        record.conversationId(), record.parseRevision()));
    }

    private void requireIdentity(ProcessAiParseRecord record, String orderUuid,
                                 ProcessAiConfirmRequest request) {
        boolean matches = record.orderUuid().equals(orderUuid)
                && record.conversationId().equals(request.conversationId());
        if (!matches) throw notFound();
        if (record.expectedVersion() != request.expectedVersion()) {
            throw conflict("AI_PARSE_VERSION_CONFLICT", "The AI parse version has changed");
        }
    }

    private BusinessException notFound() {
        return new BusinessException(ResultCode.NOT_FOUND,
                "AI_PARSE_NOT_FOUND", "AI parse not found");
    }

    private BusinessException conflict(String code, String message) {
        return new BusinessException(ResultCode.CONFLICT, code, message);
    }
}
