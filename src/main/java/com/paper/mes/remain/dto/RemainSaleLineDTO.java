package com.paper.mes.remain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RemainSaleLineDTO {
    @NotBlank
    private String lotUuid;
    @NotNull
    @DecimalMin(value = "0.001")
    private BigDecimal systemWeight;
}
