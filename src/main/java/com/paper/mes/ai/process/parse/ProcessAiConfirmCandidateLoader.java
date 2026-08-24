package com.paper.mes.ai.process.parse;

import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;
import com.paper.mes.ai.process.parse.dto.ProcessAiConfirmRequest;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

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
        requireWorkflow(record, request);
        if ("UNDERSTANDING".equals(record.resultKind()) || "FAILURE".equals(record.resultKind())) {
            throw conflict("AI_PARSE_NOT_READY", "该AI结果仍需澄清，不能确认");
        }
        if (!"CONFIRMED".equals(record.status())) requirePreviewReady(record);
        ProcessAiExtractionResult extraction = codec.readExtraction(record);
        List<String> paths = fieldPathValidator.validate(
                extraction, request.acceptedFieldPaths());
        List<String> acknowledged = validateDefaults(record, request);
        if ("CONFIRMED".equals(record.status())) {
            return replay(record, extraction, paths, request.applyIdempotencyKey(),
                    request.previewHash(), acknowledged);
        }
        return new ProcessAiConfirmationLoad(
                record, extraction, paths, request.applyIdempotencyKey(), null,
                request.previewHash(), acknowledged);
    }

    private ProcessAiConfirmationLoad replay(ProcessAiParseRecord record,
                                              ProcessAiExtractionResult extraction,
                                              List<String> paths, String idempotencyKey,
                                              String previewHash, List<String> acknowledged) {
        ProcessAiParseConfirmation confirmation = record.confirmation();
        if (!idempotencyKey.equals(confirmation.applyIdempotencyKey())) {
            throw conflict("AI_PARSE_ALREADY_CONFIRMED", "This AI parse is already confirmed");
        }
        List<String> storedPaths = codec.readPaths(confirmation.acceptedFieldPathsJson());
        if (!paths.equals(storedPaths)) {
            throw conflict("AI_CONFIRM_IDEMPOTENCY_MISMATCH",
                    "The idempotency key was reused with different accepted fields");
        }
        if (isV2(record) && (!Objects.equals(record.previewHash(), previewHash)
                || !acknowledged.equals(codec.readPaths(record.acknowledgedDefaultIds())))) {
            throw conflict("AI_CONFIRM_IDEMPOTENCY_MISMATCH",
                    "相同幂等键不能复用不同的预览版本或默认值确认");
        }
        return new ProcessAiConfirmationLoad(record, extraction, paths, idempotencyKey,
                codec.readResponse(confirmation.confirmedResultJson(),
                        record.conversationId(), record.parseRevision()), previewHash, acknowledged);
    }

    private void requireWorkflow(ProcessAiParseRecord record, ProcessAiConfirmRequest request) {
        if (!isV2(record)) return;
        if (request.parseRevision() == null || request.parseRevision() != record.parseRevision()) {
            throw conflict("AI_PARSE_REVISION_CONFLICT", "AI解析版本已过期，请重新预览");
        }
        if (request.previewHash() == null || !request.previewHash().equals(record.previewHash())) {
            throw conflict("AI_PREVIEW_HASH_CONFLICT", "AI工艺预览已过期，请重新预览");
        }
    }

    private List<String> validateDefaults(ProcessAiParseRecord record,
                                          ProcessAiConfirmRequest request) {
        List<String> required = record.requiredDefaultIds() == null
                ? List.of() : codec.readPaths(record.requiredDefaultIds());
        List<String> acknowledged = request.acknowledgedDefaultIds().stream().sorted().toList();
        required = required.stream().sorted().toList();
        if (!required.equals(acknowledged)) {
            throw conflict("AI_DEFAULT_ACKNOWLEDGEMENT_REQUIRED", "请先确认AI预览中的默认值");
        }
        return acknowledged;
    }

    private boolean isV2(ProcessAiParseRecord record) {
        return record.workflowVersion() == 2;
    }

    private void requirePreviewReady(ProcessAiParseRecord record) {
        boolean ready = "READY".equals(record.status());
        boolean v2PreviewReady = !isV2(record) || "PREVIEW_READY".equals(record.dialogueState());
        if (!ready || !v2PreviewReady) {
            throw conflict("AI_PARSE_NOT_READY", "Only preview-ready AI parses can be confirmed");
        }
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
