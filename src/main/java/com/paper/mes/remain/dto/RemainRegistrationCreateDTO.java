package com.paper.mes.remain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RemainRegistrationCreateDTO {

    @NotBlank
    @Size(max = 64)
    private String requestId;

    @NotBlank
    private String orderUuid;

    @NotBlank
    @Size(max = 100)
    private String confirmationName;

    @NotBlank
    @Size(max = 32)
    private String confirmationChannel;

    @NotNull
    private LocalDateTime confirmationAt;

    @NotBlank
    @Size(max = 500)
    private String confirmationEvidence;

    @Size(max = 500)
    private String remark;

    @Valid
    @NotEmpty
    private List<RemainRegistrationLineDTO> lines;
}
