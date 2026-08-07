package com.paper.mes.settle.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SettleDiscountDecisionDTO {
    @Size(max = 255, message = "审批意见不能超过255个字符")
    private String reason;
}
