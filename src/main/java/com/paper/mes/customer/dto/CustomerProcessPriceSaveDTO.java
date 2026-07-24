package com.paper.mes.customer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CustomerProcessPriceSaveDTO {

    @NotBlank(message = "工艺不能为空")
    private String catalogUuid;
    @NotBlank(message = "计价口径不能为空")
    @Pattern(regexp = "PIECE|TON|FIXED", message = "计价口径不正确")
    private String billingBasis;
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    @NotNull(message = "价格不能为空")
    private BigDecimal price;
    @Min(value = 0, message = "默认标记无效")
    @Max(value = 1, message = "默认标记无效")
    private Integer isDefault;
}
