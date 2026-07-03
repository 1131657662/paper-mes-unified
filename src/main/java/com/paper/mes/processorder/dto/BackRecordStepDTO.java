package com.paper.mes.processorder.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 单道工序回录明细：记录本工序产生的损耗重量。
 */
@Data
public class BackRecordStepDTO {

    @NotBlank(message = "工序uuid不能为空")
    private String uuid;

    /** 本工序损耗重量 kg，参与三级闭合与损耗报表。 */
    @DecimalMin(value = "0.000", message = "工序损耗不能为负数")
    private BigDecimal lossWeight;
}
