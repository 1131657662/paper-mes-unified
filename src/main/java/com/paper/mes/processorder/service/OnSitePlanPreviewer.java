package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.FinishConfigSpecDTO;
import com.paper.mes.processorder.dto.FinishPreviewVO;
import com.paper.mes.processorder.dto.PlanPreviewVO;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class OnSitePlanPreviewer {

    private static final String ITEM_TRIM = "TRIM";

    public PlanPreviewVO preview(ProcessPlanDTO plan, String originalUuid) {
        PlanPreviewVO vo = shell(plan, originalUuid);
        List<FinishConfigSpecDTO> specs = plan.getFinishSpecs() == null ? List.of() : plan.getFinishSpecs();
        List<FinishPreviewVO.FinishItemPreview> finishes = finishRows(specs);
        vo.setFinishCount(finishes.size());
        vo.setTrimCount(trimCount(specs));
        vo.setTotalEstimateWeight(BigDecimal.ZERO);
        vo.setTotalTrimWeight(BigDecimal.ZERO);
        vo.setFinishes(finishes);
        vo.setReady(!finishes.isEmpty());
        if (finishes.isEmpty()) {
            vo.getErrors().add("现场定尺至少需要填写预计成品件数");
        }
        vo.setSummary("现场定尺预计生成 " + finishes.size() + " 个正式号，规格现场确认");
        return vo;
    }

    private PlanPreviewVO shell(ProcessPlanDTO plan, String originalUuid) {
        PlanPreviewVO vo = new PlanPreviewVO();
        vo.setOriginalUuid(originalUuid);
        vo.setProcessMode(plan.getProcessMode());
        vo.setMainStepType(plan.getMainStepType());
        vo.setRewindMode(plan.getRewindMode());
        vo.setSpareCount(plan.getSpareCount() == null ? 0 : plan.getSpareCount());
        return vo;
    }

    private List<FinishPreviewVO.FinishItemPreview> finishRows(List<FinishConfigSpecDTO> specs) {
        List<FinishPreviewVO.FinishItemPreview> rows = new ArrayList<>();
        for (FinishConfigSpecDTO spec : specs) {
            if (ITEM_TRIM.equals(itemType(spec))) {
                continue;
            }
            int count = spec.getCount() == null ? 1 : spec.getCount();
            for (int i = 0; i < count; i++) {
                rows.add(finishRow(spec));
            }
        }
        return rows;
    }

    private FinishPreviewVO.FinishItemPreview finishRow(FinishConfigSpecDTO spec) {
        FinishPreviewVO.FinishItemPreview row = new FinishPreviewVO.FinishItemPreview();
        row.setFinishWidth(spec.getFinishWidth() == null ? 0 : spec.getFinishWidth());
        row.setFinishDiameter(spec.getFinishDiameter());
        row.setFinishCoreDiameter(spec.getFinishCoreDiameter());
        row.setEstimateWeight(BigDecimal.ZERO);
        row.setTrimWeight(BigDecimal.ZERO);
        row.setSourceSummary("现场定尺");
        return row;
    }

    private int trimCount(List<FinishConfigSpecDTO> specs) {
        return specs.stream()
                .filter(spec -> ITEM_TRIM.equals(itemType(spec)))
                .mapToInt(spec -> spec.getCount() == null ? 1 : spec.getCount())
                .sum();
    }

    private String itemType(FinishConfigSpecDTO spec) {
        return StringUtils.hasText(spec.getItemType()) ? spec.getItemType().trim().toUpperCase() : "FINISH";
    }
}
