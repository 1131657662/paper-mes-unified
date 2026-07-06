package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.FinishConfigSpecDTO;
import com.paper.mes.processorder.dto.FinishPreviewVO;
import com.paper.mes.processorder.dto.PlanPreviewVO;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class SawPlanPreviewer {

    private static final String ITEM_FINISH = "FINISH";
    private static final String ITEM_TRIM = "TRIM";
    private static final int PROCESS_MODE_STANDARD = 1;

    public PlanPreviewVO preview(ProcessPlanDTO plan, OriginalRoll roll) {
        PlanPreviewVO vo = shell(plan, roll);
        List<FinishConfigSpecDTO> specs = plan.getFinishSpecs() == null ? List.of() : plan.getFinishSpecs();
        List<FinishConfigSpecDTO> finishes = finishSpecs(specs);
        int trimWidth = effectiveTrimWidth(specs, roll);
        int finishWidth = widthTotal(finishes);
        int originalWidth = roll.getOriginalWidth() == null ? 0 : roll.getOriginalWidth();
        int pieceCount = finishCount(finishes);
        vo.setFinishCount(pieceCount);
        vo.setTrimCount(trimWidth > 0 ? 1 : 0);
        BigDecimal trimWeight = estimateTrimWeight(roll, trimWidth);
        List<FinishPreviewVO.FinishItemPreview> rows = toPreviewRows(finishes, roll, trimWidth, trimWeight);
        vo.setFinishes(rows);
        vo.setTotalEstimateWeight(sumEstimateWeight(rows));
        vo.setTotalTrimWeight(trimWeight);
        vo.setReady(pieceCount > 0 && validWidth(vo, originalWidth, finishWidth, trimWidth));
        if (pieceCount <= 0) {
            vo.getErrors().add("锯纸至少需要一条成品规格");
        }
        vo.setSummary(summary(pieceCount, knifeCount(finishes, trimWidth), finishWidth, trimWidth));
        return vo;
    }

    public List<FinishConfigSpecDTO> finishSpecs(List<FinishConfigSpecDTO> specs) {
        List<FinishConfigSpecDTO> finishes = new ArrayList<>();
        if (specs == null) {
            return finishes;
        }
        for (FinishConfigSpecDTO spec : specs) {
            if (!ITEM_TRIM.equals(itemType(spec))) {
                finishes.add(spec);
            }
        }
        return finishes;
    }

    public List<FinishConfigSpecDTO> saveSpecs(List<FinishConfigSpecDTO> specs, OriginalRoll roll) {
        List<FinishConfigSpecDTO> sourceSpecs = specs == null ? List.of() : specs;
        ProcessPlanDTO plan = new ProcessPlanDTO();
        plan.setProcessMode(roll.getProcessMode());
        plan.setMainStepType(1);
        plan.setSpareCount(0);
        plan.setFinishSpecs(sourceSpecs);
        List<FinishPreviewVO.FinishItemPreview> previews = preview(plan, roll).getFinishes();
        List<FinishConfigSpecDTO> result = new ArrayList<>(previews.size() + 1);
        for (FinishPreviewVO.FinishItemPreview item : previews) {
            result.add(toSaveSpec(item));
        }
        int trimWidth = effectiveTrimWidth(sourceSpecs, roll);
        if (trimWidth > 0) {
            result.add(toTrimSaveSpec(roll, trimWidth));
        }
        return result;
    }

    public int effectiveTrimWidth(List<FinishConfigSpecDTO> specs, OriginalRoll roll) {
        if (!isStandardProcess(roll)) {
            return 0;
        }
        List<FinishConfigSpecDTO> sourceSpecs = specs == null ? List.of() : specs;
        int explicitTrim = trimWidth(sourceSpecs);
        if (explicitTrim > 0) {
            return explicitTrim;
        }
        int originalWidth = roll.getOriginalWidth() == null ? 0 : roll.getOriginalWidth();
        if (originalWidth <= 0) {
            return 0;
        }
        return Math.max(0, originalWidth - widthTotal(finishSpecs(sourceSpecs)));
    }

    public int knifeCount(List<FinishConfigSpecDTO> specs, int trimWidth) {
        int finishCount = finishCount(specs);
        if (finishCount <= 0) {
            return 0;
        }
        return Math.max(0, finishCount - 1) + (trimWidth > 0 ? 1 : 0);
    }

    private PlanPreviewVO shell(ProcessPlanDTO plan, OriginalRoll roll) {
        PlanPreviewVO vo = new PlanPreviewVO();
        vo.setOriginalUuid(roll.getUuid());
        vo.setProcessMode(plan.getProcessMode());
        vo.setMainStepType(plan.getMainStepType());
        vo.setSpareCount(plan.getSpareCount() == null ? 0 : plan.getSpareCount());
        return vo;
    }

    private FinishConfigSpecDTO toSaveSpec(FinishPreviewVO.FinishItemPreview item) {
        FinishConfigSpecDTO spec = new FinishConfigSpecDTO();
        spec.setItemType(ITEM_FINISH);
        spec.setCount(1);
        spec.setFinishWidth(item.getFinishWidth());
        spec.setFinishDiameter(item.getFinishDiameter());
        spec.setFinishCoreDiameter(item.getFinishCoreDiameter());
        spec.setEstimateWeight(item.getEstimateWeight());
        return spec;
    }

    private FinishConfigSpecDTO toTrimSaveSpec(OriginalRoll roll, int trimWidth) {
        FinishConfigSpecDTO spec = new FinishConfigSpecDTO();
        spec.setItemType(ITEM_TRIM);
        spec.setCount(1);
        spec.setFinishWidth(trimWidth);
        spec.setEstimateWeight(estimateTrimWeight(roll, trimWidth));
        return spec;
    }

    private boolean validWidth(PlanPreviewVO vo, int originalWidth, int finishWidth, int trimWidth) {
        if (originalWidth <= 0) {
            return true;
        }
        int totalWidth = finishWidth + trimWidth;
        if (totalWidth > originalWidth) {
            vo.getErrors().add("锯纸成品门幅加切边不能超过母卷门幅");
            return false;
        }
        return true;
    }

    private List<FinishPreviewVO.FinishItemPreview> toPreviewRows(List<FinishConfigSpecDTO> specs, OriginalRoll roll,
                                                                  int trimWidth, BigDecimal trimWeight) {
        List<FinishPreviewVO.FinishItemPreview> rows = new ArrayList<>();
        BigDecimal totalWeight = isStandardProcess(roll) ? totalWeight(roll).subtract(trimWeight) : BigDecimal.ZERO;
        BigDecimal widthBasis = BigDecimal.valueOf(widthTotal(specs));
        BigDecimal allocated = BigDecimal.ZERO;
        int totalCount = finishCount(specs);
        BigDecimal trimShare = trimShare(trimWeight, totalCount);
        for (FinishConfigSpecDTO spec : specs) {
            int count = spec.getCount() == null ? 1 : spec.getCount();
            for (int i = 0; i < count; i++) {
                FinishPreviewVO.FinishItemPreview row = new FinishPreviewVO.FinishItemPreview();
                row.setFinishWidth(spec.getFinishWidth());
                row.setFinishDiameter(spec.getFinishDiameter());
                row.setFinishCoreDiameter(spec.getFinishCoreDiameter());
                row.setEstimateWeight(estimateFinishWeight(totalWeight, widthBasis, spec, rows.size(), totalCount, allocated));
                allocated = allocated.add(row.getEstimateWeight() == null ? BigDecimal.ZERO : row.getEstimateWeight());
                row.setTrimWidth(trimWidth);
                row.setTrimWeight(trimShare);
                row.setSourceSummary("当前母卷");
                rows.add(row);
            }
        }
        return rows;
    }

    private BigDecimal trimShare(BigDecimal trimWeight, int totalCount) {
        if (trimWeight == null || totalCount <= 0) {
            return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        }
        return trimWeight.divide(BigDecimal.valueOf(totalCount), 3, RoundingMode.HALF_UP);
    }

    private BigDecimal estimateFinishWeight(BigDecimal totalWeight, BigDecimal widthBasis, FinishConfigSpecDTO spec,
                                            int index, int totalCount, BigDecimal allocated) {
        if (totalCount <= 0 || totalWeight.signum() <= 0 || widthBasis.signum() <= 0) {
            return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        }
        if (index == totalCount - 1) {
            return totalWeight.subtract(allocated).setScale(3, RoundingMode.HALF_UP);
        }
        BigDecimal width = BigDecimal.valueOf(spec.getFinishWidth() == null ? 0 : spec.getFinishWidth());
        return totalWeight.multiply(width).divide(widthBasis, 3, RoundingMode.HALF_UP);
    }

    private BigDecimal sumEstimateWeight(List<FinishPreviewVO.FinishItemPreview> rows) {
        return rows.stream()
                .map(FinishPreviewVO.FinishItemPreview::getEstimateWeight)
                .filter(weight -> weight != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal estimateTrimWeight(OriginalRoll roll, int trimWidth) {
        if (trimWidth <= 0 || roll.getOriginalWidth() == null || roll.getOriginalWidth() <= 0) {
            return BigDecimal.ZERO;
        }
        return totalWeight(roll).multiply(BigDecimal.valueOf(trimWidth))
                .divide(BigDecimal.valueOf(roll.getOriginalWidth()), 3, RoundingMode.HALF_UP);
    }

    private BigDecimal totalWeight(OriginalRoll roll) {
        return (roll.getRollWeight() == null ? BigDecimal.ZERO : roll.getRollWeight())
                .multiply(BigDecimal.valueOf(roll.getPieceNum() == null ? 1 : roll.getPieceNum()));
    }

    private int trimWidth(List<FinishConfigSpecDTO> specs) {
        if (specs == null) {
            return 0;
        }
        return specs.stream()
                .filter(spec -> ITEM_TRIM.equals(itemType(spec)))
                .mapToInt(spec -> spec.getFinishWidth() == null ? 0 : spec.getFinishWidth() * count(spec))
                .sum();
    }

    private int widthTotal(List<FinishConfigSpecDTO> specs) {
        return specs.stream().mapToInt(spec -> spec.getFinishWidth() == null ? 0 : spec.getFinishWidth() * count(spec)).sum();
    }

    private int finishCount(List<FinishConfigSpecDTO> specs) {
        return specs.stream().mapToInt(this::count).sum();
    }

    private int count(FinishConfigSpecDTO spec) {
        return spec.getCount() == null ? 1 : spec.getCount();
    }

    private String itemType(FinishConfigSpecDTO spec) {
        return StringUtils.hasText(spec.getItemType()) ? spec.getItemType().trim().toUpperCase() : ITEM_FINISH;
    }

    private boolean isStandardProcess(OriginalRoll roll) {
        return roll.getProcessMode() == null || roll.getProcessMode() == PROCESS_MODE_STANDARD;
    }

    private String summary(int finishCount, int knifeCount, int finishWidth, int trimWidth) {
        return "锯纸预计生成 " + finishCount + " 个正式号，刀数 " + knifeCount
                + "，成品门幅 " + finishWidth + "mm，切边 " + trimWidth + "mm";
    }
}
