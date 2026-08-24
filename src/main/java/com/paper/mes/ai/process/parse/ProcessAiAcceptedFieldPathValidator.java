package com.paper.mes.ai.process.parse;

import com.paper.mes.ai.process.intent.ProcessAiAssignment;
import com.paper.mes.ai.process.intent.ProcessAiCustomerSpec;
import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
class ProcessAiAcceptedFieldPathValidator {

    List<String> validate(ProcessAiExtractionResult extraction, List<String> requested) {
        List<String> normalized = requested.stream().sorted().toList();
        if (new HashSet<>(normalized).size() != normalized.size()) {
            throw invalid("acceptedFieldPaths cannot contain duplicates");
        }
        Set<String> allowed = allowedPaths(extraction);
        if (!allowed.containsAll(normalized)) {
            throw invalid("acceptedFieldPaths contains an unavailable AI field");
        }
        return normalized;
    }

    private Set<String> allowedPaths(ProcessAiExtractionResult extraction) {
        Set<String> paths = new HashSet<>();
        extraction.assignments().forEach(assignment -> addAssignment(paths, assignment));
        return paths;
    }

    private void addAssignment(Set<String> paths, ProcessAiAssignment assignment) {
        String base = "/assignments/" + assignment.ownerRollRef();
        add(paths, base, "processType", "processMode", "sourceRollRefs", "coveredRollRefs", "machineUuid");
        addRewind(paths, base, assignment);
        addSaw(paths, base, assignment);
        addCustomerSpecs(paths, base, assignment);
        addAncillary(paths, base, assignment);
    }

    private void addCustomerSpecs(Set<String> paths, String base,
                                  ProcessAiAssignment assignment) {
        for (ProcessAiCustomerSpec spec : assignment.customerSpecs()) {
            if (spec.outputIndex() == null) continue;
            String prefix = base + "/customerSpecs/" + spec.outputIndex();
            if (spec.paperName() != null) paths.add(prefix + "/paperName");
            if (spec.gramWeight() != null) paths.add(prefix + "/gramWeight");
            if (spec.finishWidth() != null) paths.add(prefix + "/finishWidth");
            if (spec.overrideReason() != null) paths.add(prefix + "/overrideReason");
        }
    }

    private void addRewind(Set<String> paths, String base, ProcessAiAssignment assignment) {
        if (assignment.rewindIntent() == null) return;
        add(paths, base + "/rewindIntent", "modeIntent");
        if (assignment.rewindIntent().diameterRule() != null) {
            addDiameterRule(paths, base, assignment);
        }
        if (assignment.rewindIntent().core() != null) {
            paths.add(base + "/rewindIntent/core");
        }
        if (assignment.rewindIntent().widthRule() != null) {
            addWidthRule(paths, base, assignment);
        }
    }

    private void addDiameterRule(Set<String> paths, String base,
                                 ProcessAiAssignment assignment) {
        String path = base + "/rewindIntent/diameterRule";
        add(paths, path, "type");
        if (assignment.rewindIntent().diameterRule().parts() != null) {
            paths.add(path + "/parts");
        }
        if (assignment.rewindIntent().diameterRule().ratios() != null) {
            paths.add(path + "/ratios");
        }
        if (assignment.rewindIntent().diameterRule().targetDiameter() != null) {
            paths.add(path + "/targetDiameter");
        }
    }

    private void addWidthRule(Set<String> paths, String base, ProcessAiAssignment assignment) {
        String path = base + "/rewindIntent/widthRule";
        add(paths, path, "type");
        if (assignment.rewindIntent().widthRule().values() != null) {
            paths.add(path + "/values");
        }
        if (assignment.rewindIntent().widthRule().knifeCount() != null) {
            paths.add(path + "/knifeCount");
        }
    }

    private void addSaw(Set<String> paths, String base, ProcessAiAssignment assignment) {
        if (assignment.sawIntent() == null) return;
        add(paths, base + "/sawIntent", "type");
        if (assignment.sawIntent().knifeCount() != null) {
            paths.add(base + "/sawIntent/knifeCount");
        }
        if (assignment.sawIntent().widths() != null) {
            paths.add(base + "/sawIntent/widths");
        }
    }

    private void addAncillary(Set<String> paths, String base, ProcessAiAssignment assignment) {
        if (assignment.ancillaryRequirements() == null) return;
        if (assignment.ancillaryRequirements().label() != null) {
            paths.add(base + "/ancillaryRequirements/label");
        }
        if (assignment.ancillaryRequirements().packaging() != null) {
            paths.add(base + "/ancillaryRequirements/packaging");
        }
    }

    private void add(Set<String> paths, String base, String... fields) {
        for (String field : fields) paths.add(base + "/" + field);
    }

    private BusinessException invalid(String message) {
        return new BusinessException(ResultCode.BAD_REQUEST,
                "AI_CONFIRM_FIELDS_INVALID", message);
    }
}
