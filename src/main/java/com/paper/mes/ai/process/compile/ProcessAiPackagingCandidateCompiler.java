package com.paper.mes.ai.process.compile;

import com.paper.mes.ai.process.intent.ProcessAiAssignment;
import com.paper.mes.ai.process.intent.ProcessAiPackagingRequirement;
import com.paper.mes.ai.process.security.ProcessTextRedactor.ExtractedCharge;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
class ProcessAiPackagingCandidateCompiler {

    private static final int REPACKAGE_STEP_TYPE = 4;

    ProcessAiPackagingCompilation compile(List<ProcessAiAssignment> assignments,
                                          List<ProcessAiCompiledPlan> plans,
                                          List<ExtractedCharge> charges) {
        List<ProcessAiAssignment> packaging = assignments.stream()
                .filter(this::hasPackaging)
                .toList();
        if (packaging.isEmpty()) return empty();
        ChargeMapping chargeMapping = mapCharges(packaging, charges);
        if (!chargeMapping.errors().isEmpty()) {
            return new ProcessAiPackagingCompilation(
                    List.of(), chargeMapping.errors(), List.of());
        }
        Map<String, ProcessAiCompiledPlan> byOwner = plans.stream()
                .collect(Collectors.toMap(ProcessAiCompiledPlan::ownerRollRef, Function.identity()));
        List<ProcessAiPackagingCandidate> candidates = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        for (ProcessAiAssignment assignment : packaging) {
            ProcessAiCompiledPlan plan = byOwner.get(assignment.ownerRollRef());
            if (plan == null) continue;
            ExtractedCharge charge = chargeMapping.byOwner().get(assignment.ownerRollRef());
            candidates.add(candidate(assignment, plan, charge));
            warnings.add(assignment.ownerRollRef()
                    + ": 包装候选尚未保存，请人工确认机台、数量和价格");
        }
        return new ProcessAiPackagingCompilation(candidates, List.of(), warnings);
    }

    private ChargeMapping mapCharges(List<ProcessAiAssignment> assignments,
                                     List<ExtractedCharge> charges) {
        List<ProcessAiAssignment> tokenAssignments = assignments.stream()
                .filter(assignment -> assignment.ancillaryRequirements()
                        .packaging().chargeToken() != null)
                .toList();
        if (tokenAssignments.isEmpty() && charges.isEmpty()) return ChargeMapping.empty();
        if (tokenAssignments.size() != charges.size()) {
            return ChargeMapping.error("包装金额占位符与本地提取金额数量不一致，请重新说明包装价格");
        }
        if (tokenAssignments.size() > 1) {
            return ChargeMapping.error("同一轮包含多个包装金额，无法安全对应，请分开说明每项包装价格");
        }
        ProcessAiAssignment priced = tokenAssignments.get(0);
        ProcessAiPackagingRequirement requirement = priced
                .ancillaryRequirements().packaging();
        ExtractedCharge charge = charges.get(0);
        if (!compatibleUnit(requirement.unit(), charge.unit())) {
            return ChargeMapping.error("包装计费单位与客户原话不一致，请确认按件、按吨或固定金额");
        }
        return new ChargeMapping(Map.of(priced.ownerRollRef(), charge), List.of());
    }

    private ProcessAiPackagingCandidate candidate(ProcessAiAssignment assignment,
                                                   ProcessAiCompiledPlan plan,
                                                   ExtractedCharge charge) {
        ProcessAiPackagingRequirement requirement = assignment.ancillaryRequirements().packaging();
        String unit = normalizedUnit(requirement.unit(), charge);
        boolean fixed = "FIXED".equals(unit);
        BigDecimal amount = charge == null ? null : charge.amount();
        return new ProcessAiPackagingCandidate(
                assignment.ownerRollRef(), plan.originalUuid(), plan.coveredOriginalUuids(),
                REPACKAGE_STEP_TYPE, requirement.type(), stepName(requirement.type()),
                fixed ? null : unit, fixed ? null : quantity(unit, plan),
                fixed ? 3 : 2, fixed ? null : amount, fixed ? amount : null,
                stepName(requirement.type()) + "；AI识别候选，保存前请确认机台、数量和价格");
    }

    private BigDecimal quantity(String unit, ProcessAiCompiledPlan plan) {
        Integer finishCount = plan.preview().getFinishCount();
        if ("PIECE".equals(unit) && finishCount != null && finishCount > 0) {
            return BigDecimal.valueOf(finishCount);
        }
        BigDecimal weight = plan.preview().getTotalEstimateWeight();
        if ("TON".equals(unit) && weight != null && weight.signum() > 0) {
            return weight
                    .divide(new BigDecimal("1000"), 3, RoundingMode.HALF_UP);
        }
        return null;
    }

    private boolean hasPackaging(ProcessAiAssignment assignment) {
        return assignment.ancillaryRequirements() != null
                && assignment.ancillaryRequirements().packaging() != null;
    }

    private boolean compatibleUnit(String modelUnit, String extractedUnit) {
        if ("UNSPECIFIED".equals(extractedUnit)) return "FIXED".equals(modelUnit);
        return extractedUnit.equals(modelUnit);
    }

    private String normalizedUnit(String modelUnit, ExtractedCharge charge) {
        if (charge != null && "UNSPECIFIED".equals(charge.unit())) return "FIXED";
        return modelUnit;
    }

    private String stepName(String type) {
        return switch (type) {
            case "FILM" -> "包膜";
            case "BOX" -> "装盒";
            default -> "重新包装";
        };
    }

    private ProcessAiPackagingCompilation empty() {
        return new ProcessAiPackagingCompilation(List.of(), List.of(), List.of());
    }

    private record ChargeMapping(
            Map<String, ExtractedCharge> byOwner,
            List<String> errors) {

        private static ChargeMapping empty() {
            return new ChargeMapping(Map.of(), List.of());
        }

        private static ChargeMapping error(String message) {
            return new ChargeMapping(Map.of(), List.of(message));
        }
    }
}
