package com.paper.mes.remain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RemainPriceConfirmDTO {

    @NotBlank
    @Size(max = 64)
    private String requestId;

    @NotBlank
    @Size(max = 32)
    private String pricingBasis;

    @NotNull
    @DecimalMin(value = "0")
    private BigDecimal totalAmount;
}
