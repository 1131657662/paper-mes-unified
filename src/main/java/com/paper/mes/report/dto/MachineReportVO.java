package com.paper.mes.report.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 机台产出统计：按机台分组的卷数/产出吨位/刀数/损耗。
 */
@Data
public class MachineReportVO {

    private String machineUuid;

    private String machineName;

    /** 加工原纸卷数 */
    private Long rollCount;

    /** 产出实际总重 kg（SUM actual_weight） */
    private BigDecimal totalOutputWeight;

    /** 锯纸总刀数 */
    private Long totalKnife;

    /** 总损耗 kg */
    private BigDecimal totalLossWeight;
}
