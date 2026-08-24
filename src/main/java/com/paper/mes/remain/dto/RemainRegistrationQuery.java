package com.paper.mes.remain.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RemainRegistrationQuery {

    @Size(max = 36)
    private String orderUuid;

    @Size(max = 36)
    private String customerUuid;
}
