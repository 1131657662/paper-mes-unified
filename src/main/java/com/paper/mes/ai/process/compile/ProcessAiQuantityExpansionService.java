package com.paper.mes.ai.process.compile;

import com.paper.mes.ai.process.intent.ProcessAiQuantityIntent;
import com.paper.mes.ai.process.intent.ProcessAiSourceAllocation;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Expands semantic quantity only after source binding has been validated. */
@Service
public class ProcessAiQuantityExpansionService {

    public List<ProcessAiQuantityExpansion> expand(ProcessAiQuantityIntent intent,
                                                   List<String> selectedSources) {
        if (intent == null) return List.of();
        requireSources(selectedSources);
        int width = exactWidth(intent);
        if ("PER_SOURCE".equals(intent.scope())) {
            if (!intent.sourceAllocation().isEmpty()) {
                throw invalid("AI_QUANTITY_ALLOCATION_INVALID", "每条母卷模式不应携带分配表");
            }
            return selectedSources.stream()
                    .map(source -> expansion(source, width, intent.count())).toList();
        }
        if (!"TOTAL".equals(intent.scope())) {
            throw invalid("AI_QUANTITY_SCOPE_INVALID", "数量范围无效");
        }
        return total(intent, selectedSources, width);
    }

    private List<ProcessAiQuantityExpansion> total(ProcessAiQuantityIntent intent,
                                                   List<String> selectedSources, int width) {
        Set<String> allowed = new HashSet<>(selectedSources);
        Set<String> seen = new HashSet<>();
        List<ProcessAiQuantityExpansion> result = new ArrayList<>();
        int total = 0;
        for (ProcessAiSourceAllocation allocation : intent.sourceAllocation()) {
            if (!allowed.contains(allocation.sourceRollRef())
                    || !seen.add(allocation.sourceRollRef())) {
                throw invalid("AI_QUANTITY_SOURCE_INVALID", "数量分配引用了无效或重复母卷");
            }
            total += allocation.count();
            result.add(expansion(allocation.sourceRollRef(), width, allocation.count()));
        }
        if (total != intent.count()) {
            throw invalid("AI_QUANTITY_ALLOCATION_NOT_CLOSED", "数量分配合计必须等于全单数量");
        }
        return result;
    }

    private ProcessAiQuantityExpansion expansion(String source, int width, int count) {
        return new ProcessAiQuantityExpansion(source,
                java.util.stream.IntStream.range(0, count).mapToObj(ignored -> width).toList());
    }

    private int exactWidth(ProcessAiQuantityIntent intent) {
        try {
            int width = intent.widthMm().stripTrailingZeros().intValueExact();
            if (width <= 0) throw invalid("AI_QUANTITY_WIDTH_INVALID", "重复复卷门幅必须大于0");
            return width;
        } catch (ArithmeticException ex) {
            throw invalid("AI_QUANTITY_WIDTH_INVALID", "重复复卷门幅必须为整数毫米");
        }
    }

    private void requireSources(List<String> selectedSources) {
        if (selectedSources == null || selectedSources.isEmpty()
                || new HashSet<>(selectedSources).size() != selectedSources.size()) {
            throw invalid("AI_QUANTITY_SOURCE_INVALID", "数量展开的母卷来源无效");
        }
    }

    private BusinessException invalid(String code, String message) {
        return new BusinessException(ResultCode.BAD_REQUEST, code, message);
    }
}
