package com.paper.mes.processorder.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 加工单主单备注轻量编辑入参。
 */
@Data
public class ProcessOrderRemarkDTO {

    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;

    @Size(max = 2000, message = "详细备注不能超过2000个字符")
    private String remarkLong;
}
