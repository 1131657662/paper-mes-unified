package com.paper.mes.settle.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SettleDiscountApprovalQuery {
    @Pattern(regexp = "pending|processed|mine", message = "审批范围不正确")
    private String scope = "pending";

    @Pattern(regexp = "FINANCE|ADMIN", message = "审批级别不正确")
    private String requiredLevel;

    @Size(max = 100, message = "关键字不能超过100个字符")
    private String keyword;

    @Min(value = 1, message = "页码必须大于0")
    private long current = 1;

    @Min(value = 1, message = "每页条数必须大于0")
    @Max(value = 100, message = "每页条数不能超过100")
    private long size = 20;
}
