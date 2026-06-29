package com.paper.mes.processorder.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 单件成品回录明细：车间实际成品重量与报废/异常信息。
 */
@Data
public class BackRecordFinishDTO {

    @NotBlank(message = "成品uuid不能为空")
    private String uuid;

    /** 车间实际成品重量 kg */
    private BigDecimal actualWeight;
    /** 报废重量 kg */
    private BigDecimal scrapWeight;
    /** 0正品 1边角余料 */
    private Integer isRemain;
    /** 是否异常次品 */
    private Integer isAbnormal;
    private String abnormalType;
    private String actualRemark;
}
