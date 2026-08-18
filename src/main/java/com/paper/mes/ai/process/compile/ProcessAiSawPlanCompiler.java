package com.paper.mes.ai.process.compile;

import com.paper.mes.ai.process.context.ProcessAiRollContext;
import com.paper.mes.ai.process.intent.ProcessAiAssignment;
import com.paper.mes.ai.process.intent.ProcessAiSawIntent;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import com.paper.mes.processorder.dto.FinishConfigSpecDTO;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
class ProcessAiSawPlanCompiler {

    ProcessPlanDTO compile(ProcessAiAssignment assignment, ProcessAiRollContext owner) {
        requireSingleSource(assignment);
        List<Integer> widths = finishWidths(assignment.sawIntent(), owner);
        List<FinishConfigSpecDTO> specs = finishSpecs(widths, owner.originalWidth());
        ProcessPlanDTO plan = new ProcessPlanDTO();
        plan.setProcessMode(processMode(owner));
        plan.setMainStepType(1);
        plan.setSpareCount(0);
        plan.setKnifeCount(Math.max(0, specs.size() - 1));
        plan.setWidthDifferencePolicy("REMAINDER");
        plan.setFinishSpecs(specs);
        return plan;
    }

    private List<Integer> finishWidths(ProcessAiSawIntent intent, ProcessAiRollContext owner) {
        if (owner.originalWidth() == null || owner.originalWidth() <= 0) {
            throw invalid("AI_SAW_SOURCE_WIDTH_MISSING", "母卷门幅缺失，无法编译锯纸方案");
        }
        if ("EXPLICIT_WIDTHS".equals(intent.type())) {
            if (intent.widths() == null || intent.widths().isEmpty()) {
                throw invalid("AI_SAW_WIDTHS_MISSING", "显式锯纸方案缺少成品门幅");
            }
            return intent.widths();
        }
        if (intent.knifeCount() == null) {
            throw invalid("AI_SAW_KNIFE_COUNT_MISSING", "平均锯纸方案缺少刀数");
        }
        return distribute(owner.originalWidth(), intent.knifeCount() + 1);
    }

    private List<Integer> distribute(int width, int parts) {
        int base = width / parts;
        int remainder = width % parts;
        if (base <= 0) throw invalid("AI_SAW_PARTS_INVALID", "锯纸件数超过母卷可分配门幅");
        List<Integer> result = new ArrayList<>(parts);
        for (int index = 0; index < parts; index++) {
            result.add(base + (index >= parts - remainder ? 1 : 0));
        }
        return result;
    }

    private List<FinishConfigSpecDTO> finishSpecs(List<Integer> widths, int sourceWidth) {
        int used = widths.stream().mapToInt(Integer::intValue).sum();
        List<FinishConfigSpecDTO> result = new ArrayList<>();
        widths.forEach(width -> result.add(spec("FINISH", width)));
        if (used < sourceWidth) result.add(spec("TRIM", sourceWidth - used));
        return result;
    }

    private FinishConfigSpecDTO spec(String type, int width) {
        FinishConfigSpecDTO spec = new FinishConfigSpecDTO();
        spec.setItemType(type);
        spec.setFinishWidth(width);
        spec.setCount(1);
        return spec;
    }

    private void requireSingleSource(ProcessAiAssignment assignment) {
        if (assignment.sourceRollRefs().size() != 1) {
            throw invalid("AI_SAW_MULTI_SOURCE_UNSUPPORTED", "锯纸方案必须逐卷编译");
        }
    }

    private int processMode(ProcessAiRollContext owner) {
        return Integer.valueOf(2).equals(owner.processMode()) ? 2 : 1;
    }

    private BusinessException invalid(String code, String message) {
        return new BusinessException(ResultCode.BAD_REQUEST, code, message);
    }
}
