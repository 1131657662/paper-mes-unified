package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.FinishConfigSpecDTO;
import com.paper.mes.processorder.dto.RewindPlanPreviewDTO;
import com.paper.mes.processorder.entity.OriginalRoll;

import java.util.List;
import java.util.Map;

public final class RewindSourceWidthValidator {

    private RewindSourceWidthValidator() {
    }

    public static void requireSameWidth(OriginalRoll planOwner,
                                        List<RewindPlanPreviewDTO.RewindSegmentDTO> segments,
                                        Map<String, OriginalRoll> rollByUuid) {
        int expectedWidth = effectiveWidth(planOwner);
        for (RewindPlanPreviewDTO.RewindSegmentDTO segment : segments) {
            for (FinishConfigSpecDTO.FinishSourceDTO source : safeSources(segment)) {
                OriginalRoll sourceRoll = rollByUuid.get(source.getOriginalUuid());
                if (sourceRoll != null && effectiveWidth(sourceRoll) != expectedWidth) {
                    throw new BusinessException("多母卷合并复卷的来源母卷门幅必须一致");
                }
            }
        }
    }

    private static int effectiveWidth(OriginalRoll roll) {
        if (roll.getActualWidth() != null && roll.getActualWidth() > 0) return roll.getActualWidth();
        return roll.getOriginalWidth() == null ? 0 : roll.getOriginalWidth();
    }

    private static List<FinishConfigSpecDTO.FinishSourceDTO> safeSources(
            RewindPlanPreviewDTO.RewindSegmentDTO segment) {
        return segment.getSources() == null ? List.of() : segment.getSources();
    }
}
