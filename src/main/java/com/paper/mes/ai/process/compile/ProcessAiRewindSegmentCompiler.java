package com.paper.mes.ai.process.compile;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import com.paper.mes.ai.process.intent.ProcessAiCustomerSpec;
import com.paper.mes.processorder.service.FinishCustomerSpecificationPolicy;
import com.paper.mes.processorder.dto.RewindLayoutItemPlanDTO;
import com.paper.mes.processorder.dto.RewindSegmentPlanDTO;
import com.paper.mes.processorder.dto.RewindSourcePlanDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
class ProcessAiRewindSegmentCompiler {

    List<RewindSegmentPlanDTO> compile(ProcessAiRewindSegmentInput input) {
        return segmentInputs(input).stream().map(this::segment).toList();
    }

    private List<SegmentInput> segmentInputs(ProcessAiRewindSegmentInput input) {
        if (!weightSplit(input)) {
            return List.of(new SegmentInput(input, 1, BigDecimal.ONE, input.widths()));
        }
        List<SegmentInput> result = new ArrayList<>();
        for (int index = 0; index < input.diameterRule().parts(); index++) {
            result.add(new SegmentInput(input, index + 1,
                    input.diameterRule().ratios().get(index), input.widths()));
        }
        return result;
    }

    private RewindSegmentPlanDTO segment(SegmentInput value) {
        ProcessAiRewindSegmentInput input = value.input();
        RewindSegmentPlanDTO segment = new RewindSegmentPlanDTO();
        segment.setSegmentSort(value.sort());
        segment.setSegmentRatio(value.ratio());
        segment.setTargetDiameter(input.targetDiameter());
        segment.setFinishCoreDiameter(input.coreDiameter());
        segment.setRepeatCount(1);
        segment.setSources(sources(input));
        segment.setLayoutItems(layout(value));
        return segment;
    }

    private List<RewindLayoutItemPlanDTO> layout(SegmentInput value) {
        ProcessAiRewindSegmentInput input = value.input();
        int sourceWidth = input.owner().originalWidth();
        int used = value.widths().stream().mapToInt(Integer::intValue).sum();
        if (used > sourceWidth) {
            throw invalid("AI_REWIND_WIDTH_OVERFLOW", "复卷成品门幅合计超过母卷门幅");
        }
        boolean remainder = input.rewindMode() == 1 || input.rewindMode() == 3;
        if (!remainder && used != sourceWidth) {
            throw invalid("AI_REWIND_WIDTH_CHANGE_FORBIDDEN", "当前复卷模式不允许改变门幅");
        }
        List<RewindLayoutItemPlanDTO> result = new ArrayList<>();
        for (int index = 0; index < value.widths().size(); index++) {
            result.add(item("FINISH", value.widths().get(index), index, input, value.input().customerSpecs()));
        }
        if (remainder && used < sourceWidth) result.add(item("TRIM", sourceWidth - used));
        return result;
    }

    private RewindLayoutItemPlanDTO item(String type, int width) {
        return item(type, width, -1, null, List.of());
    }

    private RewindLayoutItemPlanDTO item(String type, int width, int index,
                                         ProcessAiRewindSegmentInput input,
                                         List<ProcessAiCustomerSpec> specs) {
        RewindLayoutItemPlanDTO item = new RewindLayoutItemPlanDTO();
        item.setItemType(type);
        item.setWidth(width);
        item.setQuantity(1);
        if ("FINISH".equals(type) && input != null) {
            specs.stream().filter(spec -> spec.outputIndex().equals(index)).findFirst()
                    .ifPresent(spec -> applyCustomerSpec(item, spec, input.owner(), width));
        }
        return item;
    }

    private void applyCustomerSpec(RewindLayoutItemPlanDTO item, ProcessAiCustomerSpec spec,
                                   com.paper.mes.ai.process.context.ProcessAiRollContext owner,
                                   int physicalWidth) {
        FinishCustomerSpecificationPolicy.requireOverrideReason(owner.paperName(), owner.gramWeight(),
                physicalWidth, spec.paperName(), spec.gramWeight(), spec.finishWidth(),
                spec.overrideReason());
        item.setCustomerPaperName(spec.paperName());
        item.setCustomerGramWeight(spec.gramWeight());
        item.setCustomerFinishWidth(spec.finishWidth());
        item.setCustomerSpecOverrideReason(spec.overrideReason());
    }

    private List<RewindSourcePlanDTO> sources(ProcessAiRewindSegmentInput input) {
        List<RewindSourcePlanDTO> result = new ArrayList<>(input.sources().size());
        for (int index = 0; index < input.sources().size(); index++) {
            result.add(source(input, index));
        }
        return result;
    }

    private RewindSourcePlanDTO source(ProcessAiRewindSegmentInput input, int index) {
        RewindSourcePlanDTO source = new RewindSourcePlanDTO();
        source.setOriginalUuid(input.sources().get(index).originalUuid());
        source.setSourceSort(index + 1);
        if (input.sources().size() == 1) source.setShareRatio(new BigDecimal("100"));
        source.setConsumeRatio(new BigDecimal("100"));
        return source;
    }

    private boolean weightSplit(ProcessAiRewindSegmentInput input) {
        return input.diameterRule() != null
                && "WEIGHT_SPLIT".equals(input.diameterRule().type());
    }

    private BusinessException invalid(String code, String message) {
        return new BusinessException(ResultCode.BAD_REQUEST, code, message);
    }

    private record SegmentInput(
            ProcessAiRewindSegmentInput input,
            int sort,
            BigDecimal ratio,
            List<Integer> widths) {
    }
}
