package com.paper.mes.settle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 结算单高风险动作原因入参。
 */
@Data
public class SettleActionReasonDTO {

    @NotBlank(message = "原因不能为空")
    @Size(max = 255, message = "原因不能超过255个字符")
    private String reason;
}
