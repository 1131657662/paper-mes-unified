package com.paper.mes.delivery.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class DeliveryCustomerRevisionRequestDTO {

    @NotBlank @Size(max = 64)
    private String requestId;
    @NotNull @Min(1)
    private Integer expectedDeliveryVersion;
    @NotBlank @Size(max = 255)
    private String reason;
    @Valid @NotEmpty @Size(max = 500)
    private List<DeliveryCustomerSpecItemDTO> items;
}
