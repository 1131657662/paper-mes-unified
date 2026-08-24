package com.paper.mes.ai.process.parse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.process.audit.ProcessAiAuditHasher;
import com.paper.mes.ai.process.compile.ProcessAiCompilationResult;
import com.paper.mes.ai.process.compile.ProcessAiDefaultResolver;
import com.paper.mes.ai.process.compile.ProcessAiDefaultValue;
import com.paper.mes.ai.process.compile.ProcessAiPlanCompilationService;
import com.paper.mes.ai.process.compile.ProcessAiPreviewHashInput;
import com.paper.mes.ai.process.compile.ProcessAiPreviewHashService;
import com.paper.mes.ai.process.parse.dto.ProcessAiConfirmRequest;
import com.paper.mes.ai.process.parse.dto.ProcessAiConfirmResponse;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProcessAiParseConfirmationService {

    private final ProcessAiConfirmationPreparationService preparationService;
    private final ProcessAiPlanCompilationService compilationService;
    private final ProcessAiDefaultResolver defaultResolver;
    private final ProcessAiPreviewHashService previewHashService;
    private final ProcessAiConfirmationCodec codec;
    private final ObjectMapper objectMapper;
    private final ProcessAiConfirmationCommitter committer;

    @Autowired
    public ProcessAiParseConfirmationService(ProcessAiConfirmationPreparationService preparationService,
                                             ProcessAiPlanCompilationService compilationService,
                                             ProcessAiDefaultResolver defaultResolver,
                                             ProcessAiPreviewHashService previewHashService,
                                             ProcessAiConfirmationCodec codec,
                                             ObjectMapper objectMapper,
                                             ProcessAiConfirmationCommitter committer) {
        this.preparationService = preparationService;
        this.compilationService = compilationService;
        this.defaultResolver = defaultResolver;
        this.previewHashService = previewHashService;
        this.codec = codec;
        this.objectMapper = objectMapper;
        this.committer = committer;
    }

    /** Compatibility constructor retained for schema-v1 unit fixtures. */
    ProcessAiParseConfirmationService(ProcessAiConfirmationPreparationService preparationService,
                                      ProcessAiPlanCompilationService compilationService,
                                      ProcessAiConfirmationCommitter committer) {
        this(preparationService, compilationService, null, null, null, null, committer);
    }

    @Transactional
    public ProcessAiConfirmResponse confirm(String orderUuid, ProcessAiConfirmRequest request) {
        ProcessAiConfirmationPreparation preparation = preparationService.prepare(
                orderUuid, request);
        if (preparation.isReplay()) return preparation.load().replay();
        ProcessAiCompilationResult compilation = compilationService.compile(
                preparation.load().extraction(), preparation.context(),
                preparation.redaction().charges());
        requireEligible(compilation);
        requireCurrentPreview(preparation, compilation);
        return committer.commit(preparation, compilation);
    }

    private void requireEligible(ProcessAiCompilationResult compilation) {
        if (!compilation.eligible()) {
            throw new BusinessException(ResultCode.CONFLICT,
                    "AI_PARSE_NOT_APPLICABLE", "AI candidate no longer passes process preview");
        }
    }

    private void requireCurrentPreview(ProcessAiConfirmationPreparation preparation,
                                        ProcessAiCompilationResult compilation) {
        if (defaultResolver == null || previewHashService == null || codec == null
                || objectMapper == null) return;
        ProcessAiParseRecord record = preparation.load().record();
        List<ProcessAiDefaultValue> defaults = defaultResolver.resolve(
                preparation.load().extraction(), preparation.context());
        List<String> defaultIds = defaults.stream().map(ProcessAiDefaultValue::defaultId).toList();
        if (!defaultIds.equals(preparation.load().acknowledgedDefaultIds())) {
            throw conflict("AI_DEFAULTS_CHANGED", "AI默认值已变化，请重新预览");
        }
        try {
            String extractionJson = codec.write(preparation.load().extraction());
            String hash = previewHashService.hash(new ProcessAiPreviewHashInput(
                    record.orderUuid(), record.expectedVersion(), record.conversationId(),
                    record.memoryGeneration(), record.projectMemoryVersion(),
                    record.projectMemoryChecksum(), ProcessAiAuditHasher.sha256(extractionJson),
                    ProcessAiAuditHasher.sha256(extractionJson), codec.correctionsHash(record),
                    defaultIds, objectMapper.readTree(codec.write(compilation.rollConfigurations())),
                    objectMapper.readTree(codec.write(compilation.plans())),
                    objectMapper.readTree(codec.write(compilation.packagingCandidates()))));
            if (!hash.equals(record.previewHash())) {
                throw conflict("AI_PREVIEW_HASH_CONFLICT", "AI工艺预览已变化，请重新预览");
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("AI preview hash input is invalid", ex);
        }
    }

    private BusinessException conflict(String code, String message) {
        return new BusinessException(ResultCode.CONFLICT, code, message);
    }
}
