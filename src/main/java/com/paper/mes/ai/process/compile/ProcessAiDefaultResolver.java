package com.paper.mes.ai.process.compile;

import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.ai.process.context.ProcessAiRollContext;
import com.paper.mes.ai.process.intent.ProcessAiAssignment;
import com.paper.mes.ai.process.intent.ProcessAiMeasurement;
import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;
import com.paper.mes.ai.process.intent.ProcessAiRewindIntent;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ProcessAiDefaultResolver {

    public List<ProcessAiDefaultValue> resolve(ProcessAiExtractionResult extraction,
                                               ProcessAiOrderContext context) {
        List<ProcessAiDefaultValue> defaults = new ArrayList<>();
        for (ProcessAiAssignment assignment : extraction.assignments()) {
            ProcessAiRewindIntent rewind = assignment.rewindIntent();
            if (rewind == null) continue;
            ProcessAiRollContext owner = owner(assignment, context);
            if ("KEEP_SPEC".equals(rewind.modeIntent())) {
                if (owner.coreDiameter() == null || owner.coreDiameter() <= 0) {
                    throw invalid("AI_KEEP_SPEC_CORE_MISSING", "同规格复卷缺少母卷纸芯规格");
                }
                continue;
            }
            if (isDefaultThreeInch(rewind.core())) {
                defaults.add(new ProcessAiDefaultValue(
                        "REWIND_FINISH_CORE_3_INCH", "/assignments/" + assignment.ownerRollRef()
                                + "/rewindIntent/core", "3 inch", "DEFAULT"));
            }
        }
        return List.copyOf(defaults);
    }

    private ProcessAiRollContext owner(ProcessAiAssignment assignment,
                                       ProcessAiOrderContext context) {
        return context.rolls().stream()
                .filter(roll -> roll.shortRef().equals(assignment.ownerRollRef()))
                .findFirst()
                .orElseThrow(() -> invalid("AI_INTENT_OWNER_INVALID", "AI解析的owner不存在"));
    }

    private boolean isDefaultThreeInch(ProcessAiMeasurement core) {
        if (core == null) return true;
        return "DEFAULT".equals(core.source()) && "inch".equals(core.unit())
                && core.value().compareTo(java.math.BigDecimal.valueOf(3)) == 0;
    }

    private BusinessException invalid(String code, String message) {
        return new BusinessException(ResultCode.BAD_REQUEST, code, message);
    }
}
