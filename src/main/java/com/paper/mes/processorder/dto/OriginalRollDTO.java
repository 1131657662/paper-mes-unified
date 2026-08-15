package com.paper.mes.processorder.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import com.paper.mes.processorder.model.WeightStatus;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 原纸明细录入入参。
 */
@Data
public class OriginalRollDTO {

    /** 草稿保存时用于保留已有母卷身份；新建母卷不传。 */
    private String uuid;

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

    /** 回录后的实测门幅；草稿更新未传入时由服务端保留已有值。 */
    @Positive(message = "实测门幅必须大于0")
    private Integer actualWidth;

    @Positive(message = "原卷直径必须大于0")
    private Integer originalDiameter;
    @Positive(message = "纸芯直径必须大于0")
    private Integer coreDiameter;
    @Positive(message = "原卷长度必须大于0")
    private Integer originalLength;

    /** 标称/估算单件重量；UNKNOWN 时允许为空。 */
    private BigDecimal rollWeight;

    /** UNKNOWN / ESTIMATED / MEASURED. 未传时由服务端按数值兼容推断。 */
    private WeightStatus weightStatus;

    /** 件数，默认 1。 */
    @Min(value = 1, message = "件数至少为1")
    @Max(value = 500, message = "件数不能超过500")
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
