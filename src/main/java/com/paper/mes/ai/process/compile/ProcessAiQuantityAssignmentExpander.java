package com.paper.mes.ai.process.compile;

import com.paper.mes.ai.process.intent.ProcessAiAssignment;
import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;
import com.paper.mes.ai.process.intent.ProcessAiQuantityIntent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Turns semantic quantity scope into one deterministic compiler assignment per source. */
@Component
public class ProcessAiQuantityAssignmentExpander {

    private final ProcessAiQuantityExpansionService expansionService;

    public ProcessAiQuantityAssignmentExpander(ProcessAiQuantityExpansionService expansionService) {
        this.expansionService = expansionService;
    }

    public List<ProcessAiAssignment> expand(ProcessAiExtractionResult extraction) {
        List<ProcessAiAssignment> result = new ArrayList<>();
        for (ProcessAiAssignment assignment : extraction.assignments()) {
            ProcessAiQuantityIntent quantity = assignment.rewindIntent() == null
                    ? null : assignment.rewindIntent().quantityIntent();
            if (quantity == null) {
                result.add(assignment);
                continue;
            }
            List<ProcessAiQuantityExpansion> expansions = expansionService.expand(
                    quantity, assignment.sourceRollRefs());
            for (ProcessAiQuantityExpansion expansion : expansions) {
                result.add(withSourceQuantity(assignment, expansion));
            }
        }
        return List.copyOf(result);
    }

    private ProcessAiAssignment withSourceQuantity(ProcessAiAssignment assignment,
                                                    ProcessAiQuantityExpansion expansion) {
        var rewind = assignment.rewindIntent();
        var quantity = new ProcessAiQuantityIntent(
                rewind.quantityIntent().type(), rewind.quantityIntent().widthMm(),
                expansion.pieceCount(), "PER_SOURCE", List.of());
        String mode = "MULTI_SOURCE".equals(rewind.modeIntent())
                ? "CHANGE_WIDTH" : rewind.modeIntent();
        var revised = new com.paper.mes.ai.process.intent.ProcessAiRewindIntent(
                mode, rewind.diameterRule(), rewind.core(), rewind.widthRule(), quantity);
        return new ProcessAiAssignment(List.of(expansion.sourceRollRef()),
                expansion.sourceRollRef(), List.of(), assignment.processType(), assignment.processMode(), revised,
                assignment.sawIntent(), assignment.ancillaryRequirements(), assignment.evidence(),
                assignment.customerSpecs());
    }
}
