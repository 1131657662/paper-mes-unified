package com.paper.mes.remain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class RemainSaleCreateDTO {
    @NotBlank
    @Size(max = 64)
    private String requestId;
    @NotNull
    private LocalDateTime processDate;
    @Size(max = 36)
    private String warehouseUuid;
    @NotBlank
    private String pricingMode;
    @DecimalMin(value = "0")
    private BigDecimal actualWeight;
    @DecimalMin(value = "0")
    private BigDecimal unitPrice;
    @DecimalMin(value = "0")
    private BigDecimal totalAmount;
    @NotNull
    @DecimalMin(value = "0")
    private BigDecimal receivedAmount;
    @Size(max = 100)
    private String buyerName;
    @Size(max = 50)
    private String vehicleNo;
    @Size(max = 100)
    private String weighingTicketNo;
    @Size(max = 500)
    private String weighingEvidence;
    @Size(max = 500)
    private String reason;
    @NotEmpty
    @Valid
    private List<RemainSaleLineDTO> lines;
}
