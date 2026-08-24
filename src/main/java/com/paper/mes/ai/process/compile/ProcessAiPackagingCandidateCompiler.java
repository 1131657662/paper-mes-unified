package com.paper.mes.ai.process.compile;

import com.paper.mes.ai.process.intent.ProcessAiAssignment;
import com.paper.mes.ai.process.intent.ProcessAiPackagingRequirement;
import com.paper.mes.ai.process.security.ProcessTextRedactor.ExtractedCharge;
import com.paper.mes.ai.process.context.ProcessAiRollContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
class ProcessAiPackagingCandidateCompiler {

    ProcessAiPackagingCompilation compile(List<ProcessAiAssignment> assignments,
                                          List<ProcessAiCompiledPlan> plans,
                                          List<ExtractedCharge> charges) {
        return compile(assignments, plans, charges, List.of());
    }

    ProcessAiPackagingCompilation compile(List<ProcessAiAssignment> assignments,
                                          List<ProcessAiCompiledPlan> plans,
                                          List<ExtractedCharge> charges,
                                          List<ProcessAiRollContext> sourceRolls) {
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
        Map<String, ProcessAiRollContext> sourceByRef = sourceRolls.stream()
                .collect(Collectors.toMap(ProcessAiRollContext::shortRef,
                        Function.identity(), (left, right) -> left));
        for (ProcessAiAssignment assignment : packaging) {
            ProcessAiCompiledPlan plan = byOwner.get(assignment.ownerRollRef());
            if (plan == null && !isServiceOnly(assignment)) continue;
            ExtractedCharge charge = chargeMapping.byOwner().get(assignment.ownerRollRef());
            if (isServiceOnly(assignment)) {
                candidates.add(candidate(assignment, null, charge,
                        sourceByRef.get(assignment.ownerRollRef()), assignment.ownerRollRef()));
            } else {
                candidates.add(candidate(assignment, plan, charge,
                        sourceByRef.get(assignment.ownerRollRef()), assignment.ownerRollRef()));
            }
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
        if (canReuseSingleCharge(tokenAssignments, charges)) {
            return sharedCharge(tokenAssignments, charges.getFirst());
        }
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

    private boolean canReuseSingleCharge(List<ProcessAiAssignment> assignments,
                                         List<ExtractedCharge> charges) {
        if (assignments.size() < 2 || charges.size() != 1
                || assignments.stream().anyMatch(assignment -> !isServiceOnly(assignment))) {
            return false;
        }
        ProcessAiPackagingRequirement first = assignments.getFirst()
                .ancillaryRequirements().packaging();
        return assignments.stream()
                .map(assignment -> assignment.ancillaryRequirements().packaging())
                .allMatch(item -> first.type().equals(item.type())
                        && first.unit().equals(item.unit())
                        && first.chargeToken().equals(item.chargeToken()));
    }

    private ChargeMapping sharedCharge(List<ProcessAiAssignment> assignments,
                                       ExtractedCharge charge) {
        Map<String, ExtractedCharge> byOwner = assignments.stream().collect(Collectors.toMap(
                ProcessAiAssignment::ownerRollRef, ignored -> charge, (first, ignored) -> first));
        return new ChargeMapping(byOwner, List.of());
    }

    private ProcessAiPackagingCandidate candidate(ProcessAiAssignment assignment,
                                                   ProcessAiCompiledPlan plan,
                                                   ExtractedCharge charge,
                                                   ProcessAiRollContext source,
                                                   String ownerRollRef) {
        ProcessAiPackagingRequirement requirement = assignment.ancillaryRequirements().packaging();
        String unit = normalizedUnit(requirement.unit(), charge);
        boolean fixed = "FIXED".equals(unit);
        BigDecimal amount = charge == null ? null : charge.amount();
        String originalUuid = plan == null ? source == null ? null : source.originalUuid()
                : plan.originalUuid();
        List<String> covered = plan == null ? List.of() : plan.coveredOriginalUuids();
        return new ProcessAiPackagingCandidate(
                ownerRollRef, originalUuid, covered,
                stepType(requirement.type()), requirement.type(), stepName(requirement.type()),
                fixed ? null : unit, null,
                fixed ? 3 : billingMode(requirement), fixed ? null : amount, fixed ? amount : null,
                stepName(requirement.type()) + "；按当前母卷的权威件数或吨位计费");
    }

    private boolean hasPackaging(ProcessAiAssignment assignment) {
        return assignment.ancillaryRequirements() != null
                && assignment.ancillaryRequirements().packaging() != null;
    }

    private boolean isServiceOnly(ProcessAiAssignment assignment) {
        return "SERVICE_ONLY".equals(assignment.processType())
                || "ANCILLARY_ONLY".equals(assignment.processType());
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
            case "STRIP_SORT" -> "剥损整理";
            default -> "重新包装";
        };
    }

    private int stepType(String type) {
        return "STRIP_SORT".equals(type) ? 3 : 4;
    }

    private int billingMode(ProcessAiPackagingRequirement requirement) {
        return "SPECIFIED".equals(requirement.quantityMode()) ? 2 : 1;
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
