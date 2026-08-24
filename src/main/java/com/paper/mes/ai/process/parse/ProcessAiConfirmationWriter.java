package com.paper.mes.ai.process.parse;

import com.paper.mes.ai.process.compile.ProcessAiCompilationResult;
import com.paper.mes.ai.process.compile.ProcessAiCompiledPlan;
import com.paper.mes.ai.process.parse.dto.ProcessAiConfirmResponse;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import com.paper.mes.processorder.service.ProcessAiDraftApplicationService;
import com.paper.mes.processorder.service.ProcessAiDraftApplyCommand;
import com.paper.mes.processorder.service.ProcessAiDraftApplyResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
class ProcessAiConfirmationWriter {

    private final ProcessAiParseRepository repository;
    private final ProcessAiConfirmationCodec codec;
    private final ProcessAiDraftApplicationService draftApplicationService;

    ProcessAiConfirmResponse confirm(ProcessAiConfirmationWriteCommand command) {
        ProcessAiConfirmationMaterial material = material(command);
        String requirementJson = codec.write(requirement(command, material));
        ProcessAiDraftApplyResult applied = draftApplicationService.apply(
                new ProcessAiDraftApplyCommand(
                        command.load().record().orderUuid(),
                        command.load().record().expectedVersion(),
                        command.load().record().parseId(), requirementJson,
                        command.customerRequirement(), command.load().acceptedFieldPaths(),
                        command.compilation()));
        ProcessAiConfirmResponse response = response(command, material, applied);
        ProcessAiParseConfirmation confirmation = confirmation(command, response, material);
        persist(command, confirmation);
        return response;
    }

    private ProcessAiConfirmationMaterial material(ProcessAiConfirmationWriteCommand command) {
        Map<String, ProcessAiCompiledPlan> plans = indexedPlans(command.compilation());
        String planHash = codec.sha256(codec.write(new ConfirmationHashPayload(
                command.compilation().rollConfigurations(), plans,
                command.compilation().packagingCandidates())));
        int nextVersion = command.load().record().expectedVersion() + 1;
        return new ProcessAiConfirmationMaterial(
                plans, planHash, nextVersion, LocalDateTime.now());
    }

    private Map<String, ProcessAiCompiledPlan> indexedPlans(ProcessAiCompilationResult result) {
        Map<String, ProcessAiCompiledPlan> plans = new LinkedHashMap<>();
        for (ProcessAiCompiledPlan plan : result.plans()) {
            if (plans.putIfAbsent(plan.originalUuid(), plan) != null) {
                throw conflict("AI_CONFIRM_PLAN_CONFLICT", "AI candidate has duplicate owners");
            }
        }
        return plans;
    }

    private ProcessAiConfirmResponse response(ProcessAiConfirmationWriteCommand command,
                                               ProcessAiConfirmationMaterial material,
                                               ProcessAiDraftApplyResult applied) {
        ProcessAiParseRecord record = command.load().record();
        return new ProcessAiConfirmResponse(
                record.conversationId(), record.parseId(), record.parseRevision(),
                record.expectedVersion(), applied.nextVersion(), "CONFIRMED",
                command.load().acceptedFieldPaths(), applied.plans(),
                applied.packagingCandidates(),
                command.compilation().warnings(), command.customerRequirement(),
                material.planHash(), record.parseRevision(), command.load().previewHash(),
                command.load().acknowledgedDefaultIds());
    }

    private ProcessAiParseConfirmation confirmation(ProcessAiConfirmationWriteCommand command,
                                                     ProcessAiConfirmResponse response,
                                                     ProcessAiConfirmationMaterial material) {
        return new ProcessAiParseConfirmation(
                command.load().applyIdempotencyKey(),
                codec.write(command.load().acceptedFieldPaths()), material.planHash(),
                material.nextVersion(), codec.writeResponse(response), command.confirmedBy(),
                material.confirmedAt(), codec.write(command.load().acknowledgedDefaultIds()));
    }

    private ProcessAiConfirmedRequirement requirement(ProcessAiConfirmationWriteCommand command,
                                                       ProcessAiConfirmationMaterial material) {
        ProcessAiParseRecord record = command.load().record();
        return new ProcessAiConfirmedRequirement(
                record.schemaVersion(), record.conversationId(), record.parseId(),
                record.parseRevision(), command.load().acceptedFieldPaths(),
                record.projectMemoryVersion(), record.projectMemoryChecksum(),
                material.planHash(), command.confirmedBy(),
                material.confirmedAt());
    }

    private void persist(ProcessAiConfirmationWriteCommand command,
                         ProcessAiParseConfirmation confirmation) {
        ProcessAiParseRecord record = command.load().record();
        if (repository.confirm(record.parseId(), confirmation) != 1) {
            throw conflict("AI_PARSE_CONFIRM_CONFLICT", "AI parse could not be confirmed");
        }
    }

    private BusinessException conflict(String code, String message) {
        return new BusinessException(ResultCode.CONFLICT, code, message);
    }

    private record ConfirmationHashPayload(
            java.util.List<com.paper.mes.ai.process.compile.ProcessAiRollConfiguration> rollConfigurations,
            Map<String, ProcessAiCompiledPlan> plans,
            java.util.List<com.paper.mes.ai.process.compile.ProcessAiPackagingCandidate> packagingCandidates) {
    }
}
