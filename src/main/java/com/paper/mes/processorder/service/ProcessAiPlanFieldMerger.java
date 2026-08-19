package com.paper.mes.processorder.service;

import com.paper.mes.ai.process.compile.ProcessAiCompiledPlan;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.dto.RewindSegmentPlanDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
class ProcessAiPlanFieldMerger {

    ProcessPlanDTO merge(ProcessPlanDTO current, ProcessAiCompiledPlan candidate,
                         List<String> acceptedPaths) {
        Set<String> accepted = new HashSet<>(acceptedPaths);
        String base = "/assignments/" + candidate.ownerRollRef();
        ProcessPlanDTO source = candidate.plan();
        ProcessPlanDTO result = current == null ? new ProcessPlanDTO() : copy(current);
        if (accepted.contains(base + "/machineUuid")) {
            result.setMachineUuid(source.getMachineUuid());
        }
        boolean processType = accepted.contains(base + "/processType");
        if (processType) applyProcessType(result, source);
        if (acceptedBelow(accepted, base + "/sawIntent")) applySaw(result, source);
        if (accepted.contains(base + "/rewindIntent/modeIntent")) {
            boolean modeChanged = !Objects.equals(result.getRewindMode(), source.getRewindMode());
            result.setRewindMode(source.getRewindMode());
            if (modeChanged) normalizeModeDependentFields(result, source.getRewindMode());
        }
        applySegments(result, source, accepted, base, current == null);
        return result;
    }

    private void applyProcessType(ProcessPlanDTO target, ProcessPlanDTO source) {
        target.setProcessMode(source.getProcessMode());
        target.setMainStepType(source.getMainStepType());
        target.setSpareCount(source.getSpareCount());
        if (Integer.valueOf(1).equals(source.getMainStepType())) {
            target.setRewindMode(null);
            target.setSegments(null);
        } else {
            target.setKnifeCount(null);
            target.setFinishSpecs(List.of());
        }
    }

    private void applySaw(ProcessPlanDTO target, ProcessPlanDTO source) {
        target.setKnifeCount(source.getKnifeCount());
        target.setFinishSpecs(copy(source).getFinishSpecs());
        target.setWidthDifferencePolicy(source.getWidthDifferencePolicy());
    }

    private void applySegments(ProcessPlanDTO target, ProcessPlanDTO source, Set<String> accepted,
                               String base, boolean newProcess) {
        boolean diameter = acceptedBelow(accepted, base + "/rewindIntent/diameterRule");
        boolean core = accepted.contains(base + "/rewindIntent/core");
        boolean width = acceptedBelow(accepted, base + "/rewindIntent/widthRule");
        boolean sources = accepted.contains(base + "/sourceRollRefs");
        if (!diameter && !core && !width && !sources) return;
        List<RewindSegmentPlanDTO> candidateSegments = source.getSegments();
        if (candidateSegments == null || candidateSegments.isEmpty()) return;
        List<RewindSegmentPlanDTO> current = target.getSegments();
        if (newProcess || current == null || current.isEmpty()) {
            target.setSegments(newSegments(candidateSegments, diameter, core, width, sources));
        } else {
            target.setSegments(mergeExisting(current, candidateSegments,
                    diameter, core, width, sources));
        }
        if (diameter) target.setAllocationRule(source.getAllocationRule());
        if (width) target.setWidthDifferencePolicy(source.getWidthDifferencePolicy());
        target.setFinishSpecs(List.of());
    }

    /** Keep persisted drafts aligned with the mode-dependent fields used by the editor and preview. */
    private void normalizeModeDependentFields(ProcessPlanDTO plan, Integer rewindMode) {
        if (rewindMode == null) return;
        if (rewindMode != 2 && rewindMode != 3 && plan.getSegments() != null) {
            plan.getSegments().forEach(segment -> {
                segment.setTargetDiameter(null);
                segment.setFinishCoreDiameter(null);
            });
        }
        if (rewindMode != 2 && rewindMode != 3) plan.setAllocationRule(null);
        if (rewindMode != 1 && rewindMode != 3) plan.setWidthDifferencePolicy(null);
    }

    private List<RewindSegmentPlanDTO> newSegments(
            List<RewindSegmentPlanDTO> candidates,
            boolean diameter, boolean core, boolean width, boolean sources) {
        return candidates.stream().map(candidate -> {
            RewindSegmentPlanDTO segment = new RewindSegmentPlanDTO();
            segment.setSegmentSort(candidate.getSegmentSort());
            if (diameter) {
                segment.setSegmentRatio(candidate.getSegmentRatio());
                segment.setTargetDiameter(candidate.getTargetDiameter());
                segment.setRepeatCount(candidate.getRepeatCount());
            }
            if (core) segment.setFinishCoreDiameter(candidate.getFinishCoreDiameter());
            if (width) segment.setLayoutItems(copySegment(candidate).getLayoutItems());
            if (sources) segment.setSources(copySegment(candidate).getSources());
            return segment;
        }).toList();
    }

    private List<RewindSegmentPlanDTO> mergeExisting(
            List<RewindSegmentPlanDTO> current, List<RewindSegmentPlanDTO> candidates,
            boolean diameter, boolean core, boolean width, boolean sources) {
        List<RewindSegmentPlanDTO> result = new ArrayList<>(current.size());
        for (int index = 0; index < current.size(); index++) {
            RewindSegmentPlanDTO existing = copySegment(current.get(index));
            RewindSegmentPlanDTO candidate = candidates.get(Math.min(index, candidates.size() - 1));
            if (diameter) {
                existing.setSegmentRatio(candidate.getSegmentRatio());
                existing.setTargetDiameter(candidate.getTargetDiameter());
                existing.setRepeatCount(candidate.getRepeatCount());
            }
            if (core) existing.setFinishCoreDiameter(candidate.getFinishCoreDiameter());
            if (width) existing.setLayoutItems(copySegment(candidate).getLayoutItems());
            if (sources) existing.setSources(copySegment(candidate).getSources());
            result.add(existing);
        }
        return result;
    }

    private boolean acceptedBelow(Set<String> paths, String prefix) {
        return paths.stream().anyMatch(path -> path.startsWith(prefix + "/"));
    }

    private ProcessPlanDTO copy(ProcessPlanDTO value) {
        return ProcessPlanCopies.copy(value);
    }

    private RewindSegmentPlanDTO copySegment(RewindSegmentPlanDTO value) {
        return ProcessPlanCopies.copySegment(value);
    }
}
