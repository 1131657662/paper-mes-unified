package com.paper.mes.ai.process.compile;

import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.ai.process.context.ProcessAiRollContext;
import com.paper.mes.ai.process.intent.ProcessAiAncillaryRequirements;
import com.paper.mes.ai.process.intent.ProcessAiAssignment;
import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;
import com.paper.mes.ai.process.security.ProcessTextRedactor.ExtractedCharge;
import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.PlanPreviewVO;
import com.paper.mes.processorder.service.ProcessOrderDraftService;
import com.paper.mes.processorder.service.ProcessModePolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class ProcessAiPlanCompilationService {

    private final ProcessAiPlanCompiler compiler;
    private final ProcessOrderDraftService draftService;
    private final ProcessAiPackagingCandidateCompiler packagingCompiler;
    private final ProcessAiNewPlanCompletenessGuard completenessGuard;
    private final ProcessAiQuantityAssignmentExpander quantityExpander;
    private final ProcessAiPlanEvidenceConsistencyGuard evidenceConsistencyGuard =
            new ProcessAiPlanEvidenceConsistencyGuard();

    /** Compatibility constructor for legacy compiler fixtures without quantity semantics. */
    ProcessAiPlanCompilationService(ProcessAiPlanCompiler compiler,
                                    ProcessOrderDraftService draftService,
                                    ProcessAiPackagingCandidateCompiler packagingCompiler,
                                    ProcessAiNewPlanCompletenessGuard completenessGuard) {
        this(compiler, draftService, packagingCompiler, completenessGuard, null);
    }

    @Autowired
    public ProcessAiPlanCompilationService(ProcessAiPlanCompiler compiler,
                                           ProcessOrderDraftService draftService,
                                           ProcessAiPackagingCandidateCompiler packagingCompiler,
                                           ProcessAiNewPlanCompletenessGuard completenessGuard,
                                           ProcessAiQuantityAssignmentExpander quantityExpander) {
        this.compiler = compiler;
        this.draftService = draftService;
        this.packagingCompiler = packagingCompiler;
        this.completenessGuard = completenessGuard;
        this.quantityExpander = quantityExpander;
    }

    public ProcessAiCompilationResult compile(ProcessAiExtractionResult extraction,
                                              ProcessAiOrderContext context) {
        return compile(extraction, context, List.of());
    }

    public ProcessAiCompilationResult compile(ProcessAiExtractionResult extraction,
                                              ProcessAiOrderContext context,
                                              List<ExtractedCharge> charges) {
        List<ProcessAiAssignment> assignments;
        try {
            assignments = quantityExpander == null ? extraction.assignments()
                    : quantityExpander.expand(extraction);
        } catch (BusinessException ex) {
            return new ProcessAiCompilationResult(false, List.of(), List.of(), List.of(),
                    List.of(ex.getMessage()), List.of());
        }
        List<String> errors = blockers(extraction);
        if (!errors.isEmpty()) {
            return new ProcessAiCompilationResult(
                    false, List.of(), List.of(), List.of(), errors, List.of());
        }
        CompilationState state = new CompilationState();
        List<ProcessAiAssignment> planAssignments = assignments.stream()
                .filter(this::requiresPlan)
                .toList();
        assignments.forEach(assignment -> state.rollConfigurations.add(configuration(assignment, context)));
        for (ProcessAiAssignment assignment : planAssignments) {
            compileAssignment(assignment, context, state);
        }
        ProcessAiPackagingCompilation packaging = packagingCompiler.compile(
                assignments, state.plans, charges, context.rolls());
        state.packagingCandidates.addAll(packaging.candidates());
        state.errors.addAll(packaging.errors());
        state.warnings.addAll(packaging.warnings());
        errors.addAll(state.errors);
        boolean eligible = errors.isEmpty()
                && state.plans.size() == planAssignments.size()
                && packagingComplete(assignments, state.packagingCandidates);
        return new ProcessAiCompilationResult(
                eligible, state.rollConfigurations, state.plans, state.packagingCandidates,
                errors, state.warnings);
    }

    private boolean requiresPlan(ProcessAiAssignment assignment) {
        return "REWIND".equals(assignment.processType()) || "SAW".equals(assignment.processType());
    }

    private boolean isServiceOnly(ProcessAiAssignment assignment) {
        return "SERVICE_ONLY".equals(assignment.processType())
                || "ANCILLARY_ONLY".equals(assignment.processType());
    }

    private ProcessAiRollConfiguration configuration(ProcessAiAssignment assignment,
                                                      ProcessAiOrderContext context) {
        List<String> originalUuids = assignment.sourceRollRefs().stream()
                .map(ref -> originalUuid(context, ref)).toList();
        int mode = processMode(assignment);
        Integer mainStep = requiresPlan(assignment)
                ? "SAW".equals(assignment.processType()) ? 1 : 2 : null;
        return new ProcessAiRollConfiguration(assignment.ownerRollRef(), originalUuids, mode, mainStep);
    }

    private String originalUuid(ProcessAiOrderContext context, String ref) {
        return context.rolls().stream().filter(roll -> roll.shortRef().equals(ref))
                .map(ProcessAiRollContext::originalUuid).findFirst()
                .orElseThrow(() -> new IllegalStateException("validated AI source reference is missing"));
    }

    private int processMode(ProcessAiAssignment assignment) {
        String mode = assignment.processMode();
        if (mode == null) {
            if (isServiceOnly(assignment)) return ProcessModePolicy.SERVICE_ONLY;
            if ("DIRECT_SHIP".equals(assignment.processType())) return ProcessModePolicy.DIRECT_SHIP;
            return ProcessModePolicy.STANDARD;
        }
        return switch (mode) {
            case "ON_SITE" -> ProcessModePolicy.ON_SITE;
            case "DIRECT_SHIP" -> ProcessModePolicy.DIRECT_SHIP;
            case "SERVICE_ONLY" -> ProcessModePolicy.SERVICE_ONLY;
            default -> ProcessModePolicy.STANDARD;
        };
    }

    private boolean packagingComplete(List<ProcessAiAssignment> assignments,
                                      List<ProcessAiPackagingCandidate> candidates) {
        long required = assignments.stream()
                .filter(assignment -> assignment.ancillaryRequirements() != null
                        && assignment.ancillaryRequirements().packaging() != null)
                .count();
        return required == 0 || candidates.size() == required;
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
        if (extraction.assignments().stream().flatMap(assignment -> assignment.evidence().stream())
                .anyMatch(evidence -> Objects.equals("MODEL_INFERENCE", evidence.sourceType()))) {
            errors.add("AI解析包含无法由服务端核验的模型推断");
        }
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
        if (ancillary.packaging() != null) warnings.add(ownerRef + ": 附加工艺将按当前母卷保存");
    }

    private static final class CompilationState {
        private final List<ProcessAiRollConfiguration> rollConfigurations = new ArrayList<>();
        private final List<ProcessAiCompiledPlan> plans = new ArrayList<>();
        private final List<ProcessAiPackagingCandidate> packagingCandidates = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
    }
}
