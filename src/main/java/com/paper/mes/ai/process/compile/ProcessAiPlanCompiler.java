package com.paper.mes.ai.process.compile;

import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.ai.process.context.ProcessAiRollContext;
import com.paper.mes.ai.process.intent.ProcessAiAssignment;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class ProcessAiPlanCompiler {

    private final ProcessAiRewindPlanCompiler rewindCompiler;
    private final ProcessAiSawPlanCompiler sawCompiler;
    private final ProcessAiPlanMachineResolver machineResolver;

    ProcessAiPlanCandidate compile(ProcessAiAssignment assignment,
                                   ProcessAiOrderContext context) {
        Map<String, ProcessAiRollContext> rolls = context.rolls().stream()
                .collect(Collectors.toMap(ProcessAiRollContext::shortRef, roll -> roll));
        ProcessAiRollContext owner = rolls.get(assignment.ownerRollRef());
        if (owner == null) {
            throw invalid("AI_COMPILE_OWNER_MISSING", "AI方案引用的owner母卷不存在");
        }
        List<ProcessAiRollContext> sources = assignment.sourceRollRefs().stream()
                .map(rolls::get).toList();
        var plan = "REWIND".equals(assignment.processType())
                ? rewindCompiler.compile(assignment, owner, sources)
                : sawCompiler.compile(assignment, owner);
        machineResolver.resolve(owner, plan);
        List<String> covered = assignment.coveredRollRefs().stream()
                .map(rolls::get)
                .map(ProcessAiRollContext::originalUuid)
                .toList();
        return new ProcessAiPlanCandidate(owner.shortRef(), owner.originalUuid(), covered, plan);
    }

    private BusinessException invalid(String code, String message) {
        return new BusinessException(ResultCode.BAD_REQUEST, code, message);
    }
}
