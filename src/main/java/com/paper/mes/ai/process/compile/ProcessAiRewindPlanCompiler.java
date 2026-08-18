package com.paper.mes.ai.process.compile;

import com.paper.mes.ai.config.AiProperties;
import com.paper.mes.ai.process.context.ProcessAiRollContext;
import com.paper.mes.ai.process.intent.ProcessAiAssignment;
import com.paper.mes.ai.process.intent.ProcessAiDiameterRule;
import com.paper.mes.ai.process.intent.ProcessAiRewindIntent;
import com.paper.mes.ai.process.intent.ProcessAiWidthRule;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import com.paper.mes.processorder.dto.RewindSegmentPlanDTO;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
class ProcessAiRewindPlanCompiler {

    private final ProcessAiDiameterStorageConverter diameterConverter;
    private final ProcessAiRewindSegmentCompiler segmentCompiler;
    private final AiProperties properties;

    ProcessPlanDTO compile(ProcessAiAssignment assignment, ProcessAiRollContext owner,
                           List<ProcessAiRollContext> sources) {
        ProcessAiRewindIntent intent = assignment.rewindIntent();
        int mode = rewindMode(intent.modeIntent());
        requireSupportedMode(mode, sources);
        requireCompatibleDiameterRule(mode, intent.diameterRule());
        Integer target = targetDiameter(mode, intent.diameterRule(), owner);
        int core = coreDiameter(mode, intent, owner);
        List<Integer> widths = widths(intent.widthRule(), owner);
        List<RewindSegmentPlanDTO> segments = segmentCompiler.compile(
                new ProcessAiRewindSegmentInput(owner, intent.diameterRule(),
                        mode, target, core, widths, sources));
        ProcessPlanDTO plan = new ProcessPlanDTO();
        plan.setProcessMode(processMode(owner));
        plan.setMainStepType(2);
        plan.setSpareCount(0);
        plan.setRewindMode(mode);
        plan.setAllocationRule(isWeightSplit(intent.diameterRule()) ? "WEIGHT_SPLIT" : null);
        plan.setWidthDifferencePolicy(supportsWidthPolicy(mode) ? "REMAINDER" : null);
        plan.setSegments(segments);
        plan.setFinishSpecs(List.of());
        return plan;
    }

    private List<Integer> widths(ProcessAiWidthRule rule, ProcessAiRollContext owner) {
        int width = requiredSourceWidth(owner);
        if (rule == null || "KEEP_SPEC".equals(rule.type())) return List.of(width);
        if ("EXPLICIT".equals(rule.type())) {
            if (rule.values() == null || rule.values().isEmpty()) {
                throw invalid("AI_REWIND_WIDTHS_MISSING", "显式复卷方案缺少成品门幅");
            }
            return rule.values();
        }
        if (rule.knifeCount() == null) {
            throw invalid("AI_REWIND_KNIFE_COUNT_MISSING", "平均复卷方案缺少刀数");
        }
        return distribute(width, rule.knifeCount() + 1);
    }

    private List<Integer> distribute(int width, int parts) {
        int base = width / parts;
        int remainder = width % parts;
        if (base <= 0) throw invalid("AI_REWIND_PARTS_INVALID", "复卷件数超过可分配门幅");
        List<Integer> result = new ArrayList<>(parts);
        for (int index = 0; index < parts; index++) {
            result.add(base + (index >= parts - remainder ? 1 : 0));
        }
        return result;
    }

    private Integer targetDiameter(int mode, ProcessAiDiameterRule rule,
                                   ProcessAiRollContext owner) {
        if (mode == 1) return null;
        if (mode == 6) return required(owner.originalDiameter(), "母卷直径");
        if (rule != null && "KEEP_SPEC".equals(rule.type())) {
            return required(owner.originalDiameter(), "母卷直径");
        }
        return diameterConverter.targetDiameter(
                rule == null ? null : rule.targetDiameter(),
                properties.getDefaultTargetDiameterMm());
    }

    private int coreDiameter(int mode, ProcessAiRewindIntent intent,
                             ProcessAiRollContext owner) {
        if (mode == 6) return required(owner.coreDiameter(), "母卷纸芯");
        return diameterConverter.coreDiameter(intent.core());
    }

    private int rewindMode(String value) {
        return switch (value) {
            case "CHANGE_WIDTH" -> 1;
            case "CHANGE_DIAMETER" -> 2;
            case "CHANGE_WIDTH_AND_DIAMETER" -> 3;
            case "LAYERED" -> 4;
            case "MULTI_SOURCE" -> 5;
            case "KEEP_SPEC" -> 6;
            default -> throw invalid("AI_REWIND_MODE_INVALID", "无法识别复卷模式");
        };
    }

    private void requireSupportedMode(int mode, List<ProcessAiRollContext> sources) {
        if (mode == 4) throw invalid("AI_LAYERED_DETAILS_REQUIRED", "分层复卷必须人工补充分层参数");
        if (mode == 5 && sources.size() < 2) {
            throw invalid("AI_MULTI_SOURCE_REQUIRED", "多来源复卷至少需要两件母卷");
        }
        if (mode != 5 && sources.size() != 1) {
            throw invalid("AI_REWIND_SOURCES_INVALID", "只有多来源复卷可以引用多件母卷");
        }
    }

    private void requireCompatibleDiameterRule(int mode, ProcessAiDiameterRule rule) {
        if (isWeightSplit(rule) && mode != 2 && mode != 3) {
            throw invalid("AI_WEIGHT_SPLIT_MODE_INVALID", "重量分卷只允许改直径或门幅加直径模式");
        }
        if (mode == 6 && rule != null && !"KEEP_SPEC".equals(rule.type())) {
            throw invalid("AI_KEEP_SPEC_RULE_INVALID", "同规格复卷必须保持母卷直径");
        }
    }

    private int requiredSourceWidth(ProcessAiRollContext owner) {
        return required(owner.originalWidth(), "母卷门幅");
    }

    private int required(Integer value, String label) {
        if (value == null || value <= 0) {
            throw invalid("AI_REWIND_SOURCE_FIELD_MISSING", label + "缺失，无法编译复卷方案");
        }
        return value;
    }

    private boolean isWeightSplit(ProcessAiDiameterRule rule) {
        return rule != null && "WEIGHT_SPLIT".equals(rule.type());
    }

    private boolean supportsWidthPolicy(int mode) {
        return mode == 1 || mode == 3;
    }

    private int processMode(ProcessAiRollContext owner) {
        return Integer.valueOf(2).equals(owner.processMode()) ? 2 : 1;
    }

    private BusinessException invalid(String code, String message) {
        return new BusinessException(ResultCode.BAD_REQUEST, code, message);
    }
}
