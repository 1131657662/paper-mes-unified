package com.paper.mes.remain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RemainRefundDecisionDTO {
    @NotBlank
    @Size(max = 64)
    private String requestId;

    @Size(max = 100)
    private String paymentReference;

    @NotBlank
    @Size(max = 500)
    private String reason;
}
