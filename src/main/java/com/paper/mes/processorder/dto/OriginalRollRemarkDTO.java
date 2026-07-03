package com.paper.mes.processorder.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 原纸明细备注类字段轻量编辑入参。
 */
@Data
public class OriginalRollRemarkDTO {

    @Size(max = 100, message = "批次不能超过100个字符")
    private String batchNo;

    @Size(max = 255, message = "损伤说明不能超过255个字符")
    private String damageDesc;

    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;
}
