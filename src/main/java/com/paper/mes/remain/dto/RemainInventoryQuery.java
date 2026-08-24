package com.paper.mes.remain.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RemainInventoryQuery {

    @Size(max = 36)
    private String registrationUuid;

    @Size(max = 36)
    private String customerUuid;

    private Boolean availableOnly = true;
}
