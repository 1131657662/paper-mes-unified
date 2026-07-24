package com.paper.mes.delivery.dto;

import com.paper.mes.customerdisplay.formula.CustomerWeightCalculationMode;
import com.paper.mes.customerdisplay.formula.CustomerWeightZeroPolicy;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Data
public class DeliveryCustomerSpecItemDTO {

    @NotBlank @Size(max = 36)
    private String deliveryDetailUuid;
    @NotNull @Min(1)
    private Integer expectedDetailVersion;
    @Size(max = 100)
    private String customerPaperName;
    @Min(1) @Max(5000)
    private Integer customerGramWeight;
    @Min(1) @Max(100000)
    private Integer customerFinishWidth;
    @DecimalMin(value = "0", inclusive = false) @DecimalMax("1000000000000")
    private BigDecimal customerDisplayWeight;
    @NotNull
    private CustomerWeightCalculationMode calculationMode;
    @DecimalMin("-1000000000000") @DecimalMax("1000000000000")
    private BigDecimal weightOperand;
    @Size(max = 500)
    private String formulaExpression;
    @Size(max = 20)
    private Map<@NotBlank @Size(max = 40) String,
            @NotNull @DecimalMin("-1000000000000") @DecimalMax("1000000000000") BigDecimal> formulaVariables;
    @NotNull @Min(0) @Max(3)
    private Integer roundingScale = 3;
    @NotNull
    private RoundingMode roundingMode = RoundingMode.HALF_UP;
    @NotNull
    private CustomerWeightZeroPolicy zeroPolicy = CustomerWeightZeroPolicy.SKIP;
    @Size(max = 255)
    private String customerRemark;
}
