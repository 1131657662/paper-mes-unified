package com.paper.mes.remain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RemainRollbackLineDTO {

    @NotBlank
    private String registrationLineUuid;

    @DecimalMin(value = "0.001")
    private BigDecimal rollbackWeight;
}
