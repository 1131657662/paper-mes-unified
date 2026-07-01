package com.paper.mes.processorder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 原纸明细录入入参（单卷）。
 */
@Data
public class OriginalRollDTO {

    /** 客户内部编号 */
    private String extraNo;
    /** 来料母卷号 */
    private String rollNo;

    @NotBlank(message = "纸张品名不能为空")
    private String paperName;

    @NotNull(message = "克重不能为空")
    private Integer gramWeight;

    @NotNull(message = "门幅不能为空")
    private Integer originalWidth;

    private Integer originalDiameter;
    private Integer coreDiameter;
    private Integer originalLength;

    @NotNull(message = "单件重量不能为空")
    private BigDecimal rollWeight;

    /** 件数，默认1 */
    private Integer pieceNum;

    private String batchNo;
    private String damageDesc;

    /** 1标准加工 2现场定尺 3不加工直发 */
    private Integer processMode;
    /** 主工艺类型：1锯纸 2复卷 */
    private Integer mainStepType;
    private String machineUuid;

    private String remark;
}
