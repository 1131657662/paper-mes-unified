package com.paper.mes.processorder.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/** 完工前后允许核定的单道工序特殊计价。 */
@Data
public class ProcessStepPricingAdjustmentDTO {

    /** 1标准计价 2按指定数量计价 3固定金额 4免收。 */
    @NotNull(message = "计价模式不能为空")
    @Min(value = 1, message = "计价模式不正确")
    @Max(value = 4, message = "计价模式不正确")
    private Integer billingMode;

    /** 模式 2 使用；复卷为吨，锯纸为整数刀数。 */
    @DecimalMin(value = "0.001", message = "最终计费数量必须大于0")
    private BigDecimal billingQuantity;

    /** 模式 3 使用。 */
    @DecimalMin(value = "0.00", message = "固定计费金额不能为负数")
    private BigDecimal billingAmount;

    @NotBlank(message = "计价调整原因不能为空")
    @Size(max = 255, message = "计价调整原因不能超过255个字符")
    private String reason;
}
