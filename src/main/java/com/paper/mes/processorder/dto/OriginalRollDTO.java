package com.paper.mes.processorder.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 原纸明细录入入参。
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
    @Positive(message = "克重必须大于0")
    private Integer gramWeight;

    @NotNull(message = "门幅不能为空")
    @Positive(message = "门幅必须大于0")
    private Integer originalWidth;

    @Positive(message = "原卷直径必须大于0")
    private Integer originalDiameter;
    @Positive(message = "纸芯直径必须大于0")
    private Integer coreDiameter;
    @Positive(message = "原卷长度必须大于0")
    private Integer originalLength;

    @NotNull(message = "单件重量不能为空")
    @Positive(message = "单件重量必须大于0")
    private BigDecimal rollWeight;

    /** 件数，默认 1。 */
    @Min(value = 1, message = "件数至少为1")
    private Integer pieceNum;

    private String batchNo;
    private String damageDesc;

    /** 1标准加工 2现场定尺 3不加工直发 4仅附加工艺 */
    private Integer processMode;
    /** 主工艺类型：1锯纸 2复卷 */
    private Integer mainStepType;
    private String machineUuid;

    private String remark;
}
