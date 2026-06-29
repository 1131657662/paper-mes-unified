package com.paper.mes.processorder.dto;

import jakarta.validation.constraints.NotBlank;
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
    private Integer actualGramWeight;
    /** 实测门幅 mm */
    private Integer actualWidth;
    /** 复称实际重量 kg（闭合唯一基准、计费基准） */
    private BigDecimal actualWeight;

    private String remark;
}
