package com.paper.mes.remain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class RemainRollbackDTO {

    @NotBlank
    @Size(max = 64)
    private String requestId;

    @NotBlank
    @Size(max = 500)
    private String reason;

    @Valid
    @NotEmpty
    private List<RemainRollbackLineDTO> lines;
}
