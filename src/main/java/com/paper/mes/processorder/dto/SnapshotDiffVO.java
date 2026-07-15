package com.paper.mes.processorder.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 双版本快照对比结果（P2-6）。snap_print 标称值 vs snap_finish 实际值，
 * 按 uuid 配对输出核心维度（原纸克重/门幅、成品预估vs实际重量）差异，供前端对账展示。
 */
@Data
public class SnapshotDiffVO {

    private String orderUuid;
    private String orderNo;
    private List<RollDiff> rollDiffs;
    private List<FinishDiff> finishDiffs;

    @Data
    public static class RollDiff {
        private String uuid;
        private String rollNo;
        /** 标称克重 g/㎡ */
        private Integer printGramWeight;
        /** 实际克重 g/㎡ */
        private Integer finishGramWeight;
        private boolean gramWeightChanged;
        /** 标称门幅 mm */
        private Integer printWidth;
        /** 实际门幅 mm */
        private Integer finishWidth;
        private boolean widthChanged;
    }

    @Data
    public static class FinishDiff {
        private String uuid;
        private String finishRollNo;
        private Integer printWidth;
        private Integer finishWidth;
        private boolean widthChanged;
        private Integer printDiameter;
        private Integer finishDiameter;
        private boolean diameterChanged;
        /** 预估重量 kg */
        private BigDecimal estimateWeight;
        /** 实际重量 kg */
        private BigDecimal actualWeight;
        private boolean weightChanged;
    }
}
