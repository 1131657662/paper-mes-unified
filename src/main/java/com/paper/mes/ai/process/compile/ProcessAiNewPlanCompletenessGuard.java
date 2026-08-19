package com.paper.mes.ai.process.compile;

import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.ai.process.intent.ProcessAiAssignment;
import com.paper.mes.ai.process.intent.ProcessAiRewindIntent;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.dto.RewindSegmentPlanDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Ensures a new AI plan does not rely on fields that cannot be selected at confirmation. */
@Component
class ProcessAiNewPlanCompletenessGuard {

    List<String> validate(ProcessAiAssignment assignment, ProcessAiPlanCandidate candidate,
                          ProcessAiOrderContext context) {
        if (hasExistingPlan(assignment.ownerRollRef(), context)
                || !"REWIND".equals(assignment.processType())) {
            return List.of();
        }
        ProcessAiRewindIntent intent = assignment.rewindIntent();
        ProcessPlanDTO plan = candidate.plan();
        List<String> missing = new ArrayList<>();
        if (hasLayout(plan) && intent.widthRule() == null) {
            missing.add("请明确成品门幅，或明确保持母卷门幅");
        }
        if (hasTargetDiameter(plan) && intent.diameterRule() == null) {
            missing.add("请明确目标直径，或明确保持母卷直径");
        }
        if (hasCore(plan) && intent.core() == null) {
            missing.add("请明确成品纸芯规格");
        }
        return missing;
    }

    private boolean hasExistingPlan(String ownerRollRef, ProcessAiOrderContext context) {
        return context.baseline().plans().stream()
                .anyMatch(plan -> plan.ownerRollRef().equals(ownerRollRef)
                        && plan.plan() != null);
    }

    private boolean hasLayout(ProcessPlanDTO plan) {
        return plan.getSegments() != null && plan.getSegments().stream()
                .anyMatch(segment -> segment.getLayoutItems() != null
                        && !segment.getLayoutItems().isEmpty());
    }

    private boolean hasTargetDiameter(ProcessPlanDTO plan) {
        return segments(plan).stream().anyMatch(segment -> segment.getTargetDiameter() != null);
    }

    private boolean hasCore(ProcessPlanDTO plan) {
        return segments(plan).stream().anyMatch(segment -> segment.getFinishCoreDiameter() != null);
    }

    private List<RewindSegmentPlanDTO> segments(ProcessPlanDTO plan) {
        return plan.getSegments() == null ? List.of() : plan.getSegments();
    }
}
