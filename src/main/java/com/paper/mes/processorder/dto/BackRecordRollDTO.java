package com.paper.mes.processorder.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 单卷原纸回录明细：复称实际参数（实际克重/门幅/重量是计费与闭合基准）。
 */
@Data
public class BackRecordRollDTO {

    @NotBlank(message = "原纸单卷uuid不能为空")
    private String uuid;

    /** 车间实测实际克重 g/㎡ */
    @Positive(message = "原纸实际克重必须大于0")
    private Integer actualGramWeight;
    /** 实测门幅 mm */
    @Positive(message = "原纸实际门幅必须大于0")
    private Integer actualWidth;
    /** 复称实际重量 kg（闭合唯一基准、计费基准） */
    @DecimalMin(value = "0.001", message = "原纸复称实际重量必须大于0")
    private BigDecimal actualWeight;

    @Size(max = 255, message = "原纸回录备注不能超过255个字符")
    private String remark;
}
