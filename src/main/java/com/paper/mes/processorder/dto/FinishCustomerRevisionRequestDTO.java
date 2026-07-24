package com.paper.mes.processorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class FinishCustomerRevisionRequestDTO {

    @NotBlank
    @Size(max = 64)
    private String requestId;

    @NotNull
    private Integer expectedOrderVersion;

    @NotBlank
    @Size(max = 255)
    private String reason;

    @Valid
    @NotEmpty
    @Size(max = 500)
    private List<FinishCustomerSpecItemDTO> items;
}
