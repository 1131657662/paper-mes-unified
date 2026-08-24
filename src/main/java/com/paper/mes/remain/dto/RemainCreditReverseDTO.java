package com.paper.mes.remain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RemainCreditReverseDTO {
    @NotBlank
    @Size(max = 64)
    private String requestId;

    @NotBlank
    @Size(max = 500)
    private String reason;
}
