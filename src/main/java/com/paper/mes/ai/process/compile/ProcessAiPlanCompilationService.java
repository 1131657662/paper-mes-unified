package com.paper.mes.ai.process.compile;

import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.ai.process.intent.ProcessAiAncillaryRequirements;
import com.paper.mes.ai.process.intent.ProcessAiAssignment;
import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;
import com.paper.mes.ai.process.security.ProcessTextRedactor.ExtractedCharge;
import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.PlanPreviewVO;
import com.paper.mes.processorder.service.ProcessOrderDraftService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProcessAiPlanCompilationService {

    private final ProcessAiPlanCompiler compiler;
    private final ProcessOrderDraftService draftService;
    private final ProcessAiPackagingCandidateCompiler packagingCompiler;
    private final ProcessAiNewPlanCompletenessGuard completenessGuard;
    private final ProcessAiPlanEvidenceConsistencyGuard evidenceConsistencyGuard =
            new ProcessAiPlanEvidenceConsistencyGuard();

    public ProcessAiCompilationResult compile(ProcessAiExtractionResult extraction,
                                              ProcessAiOrderContext context) {
        return compile(extraction, context, List.of());
    }

    public ProcessAiCompilationResult compile(ProcessAiExtractionResult extraction,
                                              ProcessAiOrderContext context,
                                              List<ExtractedCharge> charges) {
        List<String> errors = blockers(extraction);
        if (!errors.isEmpty()) {
            return new ProcessAiCompilationResult(
                    false, List.of(), List.of(), errors, List.of());
        }
        CompilationState state = new CompilationState();
        for (ProcessAiAssignment assignment : extraction.assignments()) {
            compileAssignment(assignment, context, state);
        }
        ProcessAiPackagingCompilation packaging = packagingCompiler.compile(
                extraction.assignments(), state.plans, charges);
        state.packagingCandidates.addAll(packaging.candidates());
        state.errors.addAll(packaging.errors());
        state.warnings.addAll(packaging.warnings());
        errors.addAll(state.errors);
        boolean eligible = errors.isEmpty()
                && state.plans.size() == extraction.assignments().size();
        return new ProcessAiCompilationResult(
                eligible, state.plans, state.packagingCandidates, errors, state.warnings);
    }

    private void compileAssignment(ProcessAiAssignment assignment, ProcessAiOrderContext context,
                                   CompilationState state) {
        if (isRouteBaseline(assignment.ownerRollRef(), context)) {
            state.errors.add(assignment.ownerRollRef()
                    + ": 当前母卷使用链式工艺，AI 单道方案不能覆盖，请人工调整链式路线");
            return;
        }
        try {
            ProcessAiPlanCandidate candidate = compiler.compile(assignment, context);
            PlanPreviewVO preview = draftService.previewProcessPlan(
                    context.orderUuid(), candidate.originalUuid(), candidate.plan(),
                    context.draftVersion());
            state.plans.add(new ProcessAiCompiledPlan(candidate.ownerRollRef(),
                    candidate.originalUuid(), candidate.coveredOriginalUuids(),
                    candidate.plan(), preview));
            if (!preview.isReady()) {
                state.errors.addAll(prefixed(candidate.ownerRollRef(), preview.getErrors()));
            }
            List<String> completenessErrors = completenessGuard.validate(
                    assignment, candidate, context);
            if (!completenessErrors.isEmpty()) {
                state.errors.addAll(prefixed(candidate.ownerRollRef(), completenessErrors));
            }
            List<String> evidenceErrors = evidenceConsistencyGuard.validate(
                    assignment, candidate.plan());
            if (!evidenceErrors.isEmpty()) {
                state.errors.addAll(prefixed(candidate.ownerRollRef(), evidenceErrors));
            }
            addAncillaryWarnings(candidate.ownerRollRef(), assignment.ancillaryRequirements(),
                    state.warnings);
        } catch (BusinessException ex) {
            state.errors.add(assignment.ownerRollRef() + ": " + ex.getMessage());
        }
    }

    private boolean isRouteBaseline(String ownerRollRef, ProcessAiOrderContext context) {
        return context.baseline().plans().stream()
                .anyMatch(plan -> plan.ownerRollRef().equals(ownerRollRef) && plan.route());
    }

    private List<String> blockers(ProcessAiExtractionResult extraction) {
        List<String> errors = new ArrayList<>();
        if (extraction.needsClarification()) errors.add("AI解析仍需要补充说明");
        if (!extraction.conflicts().isEmpty()) errors.add("AI解析存在未解决冲突");
        if (!extraction.unmappedText().isEmpty()) errors.add("AI解析存在未映射的客户要求");
        if (!extraction.clarificationQuestions().isEmpty()) errors.add("AI解析仍有待确认问题");
        return errors;
    }

    private List<String> prefixed(String ownerRef, List<String> errors) {
        if (errors == null || errors.isEmpty()) return List.of(ownerRef + ": 工艺预览未通过");
        return errors.stream().map(error -> ownerRef + ": " + error).toList();
    }

    private void addAncillaryWarnings(String ownerRef, ProcessAiAncillaryRequirements ancillary,
                                      List<String> warnings) {
        if (ancillary == null) return;
        if (ancillary.label() != null && ancillary.label().required()) {
            warnings.add(ownerRef + ": 标签要求仅进入加工单备注，不生成附加工序");
        }
        if (ancillary.packaging() != null) {
            warnings.add(ownerRef + ": 包装要求须人工确认数量、单价和附加工序");
        }
    }

    private static final class CompilationState {
        private final List<ProcessAiCompiledPlan> plans = new ArrayList<>();
        private final List<ProcessAiPackagingCandidate> packagingCandidates = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
    }
}
