package com.paper.mes.processorder.dto;

import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.entity.ProcessStep;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 加工单详情（主表 + 原纸明细 + 成品 + 工序）。
 * Phase 1 阶段 finishRolls/steps 通常为空，由 Phase 2 打印/回录流程产生。
 */
@Data
public class ProcessOrderDetailVO {

    private ProcessOrder order;

    private List<OriginalRoll> originalRolls;

    private List<FinishRoll> finishRolls;

    private List<ProcessStep> steps;

    private List<RollProductionVO> rollProductions;

    @Data
    public static class RollProductionVO {
        private String originalUuid;
        private String extraNo;
        private String batchNo;
        private String rollNo;
        private String damageDesc;
        private String paperName;
        private Integer gramWeight;
        private Integer originalWidth;
        private BigDecimal rollWeight;
        private BigDecimal processAmount;
        private Integer pieceNum;
        private Integer processMode;
        private Integer mainStepType;
        private Integer rollStatus;
        private String remark;
        private List<ProcessStep> steps;
        private List<StageOutputVO> stageOutputs;
        private List<RewindParamVO> rewindParams;
        private List<FinishProductionVO> finishes;
    }

    @Data
    public static class StageOutputVO {
        private String uuid;
        private String outputNo;
        private String finishRollUuid;
        private String parentOutputUuid;
        private Integer stageLevel;
        private Integer outputSort;
        private Integer outputType;
        private Integer outputStatus;
        private String paperName;
        private Integer gramWeight;
        private Integer finishWidth;
        private Integer finishDiameter;
        private Integer finishCoreDiameter;
        private BigDecimal estimateWeight;
        private BigDecimal actualWeight;
        private Integer isRemain;
        private Integer sourceStepType;
        private String sourceSummary;
        private String remark;
    }

    @Data
    public static class RewindParamVO {
        private Integer paramMode;
        private Integer layerSort;
        private Integer outDiameter;
        private Integer coreDiameter;
        private Integer layerWidth;
        private BigDecimal areaRatio;
        private BigDecimal splitRatio;
        private String remark;
    }

    @Data
    public static class FinishProductionVO {
        private String uuid;
        private String finishRollNo;
        private Integer rowSort;
        private Integer rollNoStatus;
        private Integer isSpare;
        private Integer isRemain;
        private Integer sourceType;
        private String paperName;
        private Integer gramWeight;
        private Integer finishWidth;
        private Integer finishDiameter;
        private Integer finishCoreDiameter;
        private BigDecimal estimateWeight;
        private BigDecimal actualWeight;
        private Integer trimWidthShare;
        private BigDecimal trimWeightShare;
        private Integer finishStatus;
        private List<FinishSourceVO> sources;
    }

    @Data
    public static class FinishSourceVO {
        private String originalUuid;
        private String rollNo;
        private String paperName;
        private BigDecimal shareRatio;
        private BigDecimal shareWeight;
        private String remark;
    }
}
